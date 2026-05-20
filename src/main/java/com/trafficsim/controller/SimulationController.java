package com.trafficsim.controller;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.metrics.MetricsCollector;
import com.trafficsim.model.City;
import com.trafficsim.model.Intersection;
import com.trafficsim.model.Position;
import com.trafficsim.pathfinding.ParallelRouteTask;
import com.trafficsim.pathfinding.RouteCalculator;
import com.trafficsim.threads.TrafficLightThread;
import com.trafficsim.threads.VehicleThread;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationController {

    private final City city;
    private final SimulationConfig config;
    private final MetricsCollector metrics;
    private final RouteCalculator routeCalculator;
    private final CopyOnWriteArrayList<TrafficLightThread> lightThreads;
    private final CopyOnWriteArrayList<VehicleThread> vehicleThreads;

    private volatile SimulationState state = SimulationState.IDLE;
    private volatile boolean paused = false;
    private final AtomicInteger arrivedCount = new AtomicInteger(0);
    private final Random random = new Random();
    private volatile long simStartTime = 0;

    public interface SimulationListener {
        void onStateChanged(SimulationState newState);
        void onVehicleArrived(int vehicleId, long travelTimeMs);
        void onAllVehiclesArrived();
    }

    private SimulationListener listener;

    public SimulationController(SimulationConfig config) {
        this.config          = config;
        this.city            = new City();
        this.metrics         = new MetricsCollector();
        this.routeCalculator = new RouteCalculator(city);
        this.lightThreads    = new CopyOnWriteArrayList<>();
        this.vehicleThreads  = new CopyOnWriteArrayList<>();
    }

    public void setListener(SimulationListener listener) { this.listener = listener; }

    public void initialize() {
        setState(SimulationState.CALCULATING);
        Thread initThread = new Thread(() -> {
            printHeader("INICIANDO SIMULADOR DE TRAFICO URBANO");
            List<Position> semaphorePositions = city.getDefaultSemaphorePositions();
            city.assignSemaphores(semaphorePositions);
            System.out.printf("  Semaforos activos     : %d intersecciones%n", semaphorePositions.size());
            System.out.printf("  Vehiculos solicitados : %d%n", config.getVehicleCount());
            System.out.printf("  Velocidad vehiculos   : %d ms/celda%n", config.getMoveDelayMs());
            System.out.printf("  Ciclo semaforo        : verde=%ds  amarillo=%ds  rojo=%ds%n",
                    config.getGreenMs()/1000, config.getYellowMs()/1000, config.getRedMs()/1000);
            System.out.println();
            for (Position pos : semaphorePositions) {
                lightThreads.add(new TrafficLightThread(city.getIntersection(pos), config));
            }
            List<ParallelRouteTask.RouteRequest> requests = generateRouteRequests();
            printSeparator();
            System.out.println("  CALCULO DE RUTAS (A*)");
            printSeparator();
            routeCalculator.calculateSequential(requests);
            List<List<Position>> routes = routeCalculator.calculateParallel(requests);
            metrics.setSequentialRouteTime(routeCalculator.getLastSequentialTimeMs());
            metrics.setParallelRoutime(routeCalculator.getLastParallelTimeMs());
            metrics.setSpeedup(routeCalculator.getSpeedup());
            System.out.println();
            System.out.println("  RUTAS ASIGNADAS:");
            int routesOk = 0;
            for (int i = 0; i < routes.size(); i++) {
                final int vehicleId = i;
                List<Position> route = routes.get(vehicleId);
                if (route.isEmpty()) {
                    System.out.printf("  [!] Vehiculo #%02d: sin ruta disponible%n", vehicleId);
                    continue;
                }
                System.out.printf("  [#%02d] %s -> %s  (%d pasos)%n",
                        vehicleId, route.get(0), route.get(route.size()-1), route.size());
                vehicleThreads.add(new VehicleThread(vehicleId, route, city, metrics,
                        () -> onVehicleArrived(vehicleId), config.getMoveDelayMs()));
                routesOk++;
            }
            System.out.printf("%n  %d/%d vehiculos con ruta valida%n", routesOk, config.getVehicleCount());
            setState(SimulationState.IDLE);
        }, "Init-Thread");
        initThread.setDaemon(true);
        initThread.start();
    }

    public void start() {
        if (state != SimulationState.IDLE) return;
        arrivedCount.set(0);
        simStartTime = System.currentTimeMillis();
        setState(SimulationState.RUNNING);
        printSeparator();
        System.out.println("  SIMULACION EN CURSO");
        printSeparator();
        for (TrafficLightThread l : lightThreads) l.start();
        for (VehicleThread v : vehicleThreads) { v.setPaused(false); v.start(); }
    }

    public void pause() {
        if (state != SimulationState.RUNNING) return;
        paused = true;
        for (VehicleThread v : vehicleThreads) v.setPaused(true);
        setState(SimulationState.PAUSED);
        System.out.println("  [Pausado]");
    }

    public void resume() {
        if (state != SimulationState.PAUSED) return;
        paused = false;
        for (VehicleThread v : vehicleThreads) v.setPaused(false);
        setState(SimulationState.RUNNING);
        System.out.println("  [Reanudado]");
    }

    public void stop() {
        setState(SimulationState.FINISHED);
        for (VehicleThread v : vehicleThreads) v.interrupt();
        for (TrafficLightThread l : lightThreads) l.stopLight();
    }

    public void reset() {
        stop();
        for (VehicleThread v : vehicleThreads) { try { v.join(500); } catch (InterruptedException ignored) {} }
        for (TrafficLightThread l : lightThreads) { try { l.join(500); } catch (InterruptedException ignored) {} }
        vehicleThreads.clear();
        lightThreads.clear();
        arrivedCount.set(0);
        metrics.reset();
        setState(SimulationState.IDLE);
    }

    private void onVehicleArrived(int vehicleId) {
        long travelTime = metrics.getTravelTime(vehicleId);
        int arrived = arrivedCount.incrementAndGet();
        System.out.printf("  OK Vehiculo #%02d llego | trayecto: %,d ms%n", vehicleId, travelTime);
        if (listener != null) listener.onVehicleArrived(vehicleId, travelTime);
        if (arrived >= vehicleThreads.size()) {
            setState(SimulationState.FINISHED);
            if (listener != null) listener.onAllVehiclesArrived();
            printFinalReport();
        }
    }

    private void printFinalReport() {
        long totalSimTime = System.currentTimeMillis() - simStartTime;
        int total     = vehicleThreads.size();
        int completed = arrivedCount.get();
        System.out.println();
        printHeader("REPORTE FINAL DE SIMULACION");
        System.out.println("  VEHICULOS");
        System.out.printf("  +-- Total despachados    : %d%n", total);
        System.out.printf("  +-- Completaron trayecto : %d  (%.1f%%)%n",
                completed, (completed/(double)total)*100.0);
        System.out.printf("  +-- Primero en llegar    : Vehiculo #%d%n", metrics.getFirstArrival());
        System.out.println();
        System.out.println("  TIEMPOS DE TRAYECTO");
        System.out.printf("  +-- Promedio             : %,.0f ms  (%.1f s)%n",
                metrics.getAverageTravelTime(), metrics.getAverageTravelTime()/1000.0);
        System.out.printf("  +-- Mas rapido           : %,d ms%n", metrics.getFastestTravelTime());
        System.out.printf("  +-- Mas lento            : %,d ms%n", metrics.getSlowestTravelTime());
        System.out.printf("  +-- Duracion simulacion  : %,d ms  (%.1f s)%n",
                totalSimTime, totalSimTime/1000.0);
        System.out.println();
        System.out.println("  ESPERAS Y CONGESTION");
        System.out.printf("  +-- Esperas por semaforo : %,d veces%n", metrics.getTotalLightWaits());
        System.out.printf("  +-- Esperas por cruce    : %,d veces%n", metrics.getTotalLockWaits());
        System.out.printf("  +-- Tiempo total esperado: %,d ms%n", metrics.getTotalWaitTime());
        System.out.printf("  +-- Espera promedio/veh  : %,.0f ms%n", metrics.getAverageWaitTime());
        System.out.printf("  +-- Indice de congestion : %.1f%%%n", metrics.getCongestionIndex());
        System.out.println();
        System.out.println("  SEGURIDAD VIAL (CONCURRENCIA)");
        System.out.printf("  +-- Choques evitados     : %,d%n", metrics.getAvoidedCollisions());
        System.out.printf("  +-- Semaforos activos    : %d intersecciones%n", lightThreads.size());
        System.out.printf("  +-- Hilos en paralelo    : %d vehiculos + %d semaforos%n",
                total, lightThreads.size());
        System.out.println();
        System.out.println("  RENDIMIENTO A* (PATHFINDING)");
        System.out.printf("  +-- Calculo secuencial   : %,d ms%n", metrics.getSequentialRouteTime());
        System.out.printf("  +-- Calculo paralelo     : %,d ms%n", metrics.getParallelRoutime());
        System.out.printf("  +-- Speedup conseguido   : %.2fx mas rapido%n", metrics.getSpeedup());
        System.out.printf("  +-- Velocidad vehiculos  : %d ms/celda%n", config.getMoveDelayMs());
        System.out.println();
        printSeparator();
    }

    private void printHeader(String title) {
        System.out.println();
        printSeparator();
        System.out.printf("  %s%n", title);
        printSeparator();
    }

    private void printSeparator() {
        System.out.println("  ============================================");
    }

    private List<ParallelRouteTask.RouteRequest> generateRouteRequests() {
        List<Position> borderPositions = new ArrayList<>(getBorderPositions());
        Collections.shuffle(borderPositions, random);
        List<ParallelRouteTask.RouteRequest> requests = new ArrayList<>();
        int count = config.getVehicleCount();
        for (int i = 0; i < count; i++) {
            Position start = borderPositions.get(i % borderPositions.size());
            Position end;
            do { end = borderPositions.get(random.nextInt(borderPositions.size())); }
            while (end.equals(start));
            requests.add(new ParallelRouteTask.RouteRequest(i, start, end));
        }
        return requests;
    }

    private List<Position> getBorderPositions() {
        List<Position> borders = new ArrayList<>();
        int size = City.SIZE;
        for (int i = 0; i < size; i++) {
            borders.add(new Position(0, i));
            borders.add(new Position(size-1, i));
            borders.add(new Position(i, 0));
            borders.add(new Position(i, size-1));
        }
        return borders.stream().distinct().toList();
    }

    private void setState(SimulationState newState) {
        this.state = newState;
        if (listener != null) listener.onStateChanged(newState);
    }

    public SimulationState getState()                 { return state; }
    public List<VehicleThread> getVehicles()          { return vehicleThreads; }
    public List<TrafficLightThread> getLightThreads() { return lightThreads; }
    public City getCity()                             { return city; }
    public MetricsCollector getMetrics()              { return metrics; }
    public SimulationConfig getConfig()               { return config; }
    public int getArrivedCount()                      { return arrivedCount.get(); }
    public boolean isPaused()                         { return paused; }
    public double getSpeedup()                        { return routeCalculator.getSpeedup(); }
}