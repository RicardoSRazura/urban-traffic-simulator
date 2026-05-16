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

    // ── Componentes principales ──────────────────────────────────────────────
    private final City city;
    private final SimulationConfig config;
    private final MetricsCollector metrics;
    private final RouteCalculator routeCalculator;

    // ── Hilos activos ────────────────────────────────────────────────────────
    private final CopyOnWriteArrayList<TrafficLightThread> lightThreads;
    private final CopyOnWriteArrayList<VehicleThread> vehicleThreads;

    // ── Estado ───────────────────────────────────────────────────────────────
    private volatile SimulationState state = SimulationState.IDLE;
    private volatile boolean paused = false;
    private final AtomicInteger arrivedCount = new AtomicInteger(0);
    private final Random random = new Random(42);

    // ── Listener para la GUI ─────────────────────────────────────────────────
    public interface SimulationListener {
        void onStateChanged(SimulationState newState);
        void onVehicleArrived(int vehicleId, long travelTimeMs);
        void onAllVehiclesArrived();
    }

    private SimulationListener listener;

    // ── Constructor ──────────────────────────────────────────────────────────

    public SimulationController(SimulationConfig config) {
        this.config          = config;
        this.city            = new City();
        this.metrics         = new MetricsCollector();
        this.routeCalculator = new RouteCalculator(city);
        this.lightThreads    = new CopyOnWriteArrayList<>();
        this.vehicleThreads  = new CopyOnWriteArrayList<>();
    }

    public void setListener(SimulationListener listener) {
        this.listener = listener;
    }

    // ── Paso 1: initialize() ─────────────────────────────────────────────────

    public void initialize() {
        setState(SimulationState.CALCULATING);

        Thread initThread = new Thread(() -> {

            // 1a. Asignar semaforos
            List<Position> semaphorePositions = city.getDefaultSemaphorePositions();
            city.assignSemaphores(semaphorePositions);

            // 1b. Crear hilos de semaforo
            for (Position pos : semaphorePositions) {
                Intersection inter = city.getIntersection(pos);
                TrafficLightThread lightThread = new TrafficLightThread(inter, config);
                lightThreads.add(lightThread);
            }

            // 1c. Generar peticiones de ruta
            List<ParallelRouteTask.RouteRequest> requests = generateRouteRequests();

            // 1d. Calcular rutas secuencial (para medir speedup)
            routeCalculator.calculateSequential(requests);

            // 1e. Calcular rutas en paralelo (las que usan los vehiculos)
            List<List<Position>> routes = routeCalculator.calculateParallel(requests);

            // 1f. Guardar tiempos en metricas
            metrics.setSequentialRouteTime(routeCalculator.getLastSequentialTimeMs());
            metrics.setParallelRoutime(routeCalculator.getLastParallelTimeMs());
            metrics.setSpeedup(routeCalculator.getSpeedup());

            // 1g. Crear VehicleThread por cada ruta valida
            // Aqui pasamos config.getMoveDelayMs() para que la velocidad
            // venga del slider de la GUI, no de una constante fija
            for (int i = 0; i < routes.size(); i++) {
                final int vehicleId = i;
                List<Position> route = routes.get(vehicleId);

                if (route.isEmpty()) {
                    System.err.println("Vehiculo " + vehicleId + " sin ruta - omitido");
                    continue;
                }

                System.out.printf("[RUTA] Vehículo #%d: %s --> %s  (%d pasos)%n",
                        vehicleId,
                        route.get(0),
                        route.get(route.size() - 1),
                        route.size());

                VehicleThread vehicle = new VehicleThread(
                        vehicleId,
                        route,
                        city,
                        metrics,
                        () -> onVehicleArrived(vehicleId),
                        config.getMoveDelayMs()  // <-- velocidad desde el slider
                );
                vehicleThreads.add(vehicle);
            }

            setState(SimulationState.IDLE);

        }, "Init-Thread");

        initThread.setDaemon(true);
        initThread.start();
    }

    // ── Paso 2: start() ──────────────────────────────────────────────────────

    public void start() {
        if (state != SimulationState.IDLE) {
            System.err.println("No se puede iniciar: estado actual = " + state);
            return;
        }

        arrivedCount.set(0);
        setState(SimulationState.RUNNING);

        for (TrafficLightThread light : lightThreads) {
            light.start();
        }

        for (VehicleThread vehicle : vehicleThreads) {
            vehicle.setPaused(false);
            vehicle.start();
        }
    }

    // ── Paso 3a: pause() ─────────────────────────────────────────────────────

    public void pause() {
        if (state != SimulationState.RUNNING) return;
        paused = true;
        for (VehicleThread vehicle : vehicleThreads) {
            vehicle.setPaused(true);
        }
        setState(SimulationState.PAUSED);
    }

    // ── Paso 3b: resume() ────────────────────────────────────────────────────

    public void resume() {
        if (state != SimulationState.PAUSED) return;
        paused = false;
        for (VehicleThread vehicle : vehicleThreads) {
            vehicle.setPaused(false);
        }
        setState(SimulationState.RUNNING);
    }

    // ── Paso 4: stop() ───────────────────────────────────────────────────────

    public void stop() {
        setState(SimulationState.FINISHED);
        for (VehicleThread vehicle : vehicleThreads) {
            vehicle.interrupt();
        }
        for (TrafficLightThread light : lightThreads) {
            light.stopLight();
        }
    }

    // ── Paso 5: reset() ──────────────────────────────────────────────────────

    public void reset() {
        stop();

        for (VehicleThread v : vehicleThreads) {
            try { v.join(500); } catch (InterruptedException ignored) {}
        }
        for (TrafficLightThread l : lightThreads) {
            try { l.join(500); } catch (InterruptedException ignored) {}
        }

        vehicleThreads.clear();
        lightThreads.clear();
        arrivedCount.set(0);
        metrics.reset();
        setState(SimulationState.IDLE);
    }

    // ── Callbacks internos ───────────────────────────────────────────────────

    private void onVehicleArrived(int vehicleId) {
        long travelTime = metrics.getTravelTime(vehicleId);
        int arrived = arrivedCount.incrementAndGet();

        if (listener != null) {
            listener.onVehicleArrived(vehicleId, travelTime);
        }

        if (arrived >= vehicleThreads.size()) {
            setState(SimulationState.FINISHED);
            if (listener != null) {
                listener.onAllVehiclesArrived();
            }
            printFinalReport();
        }
    }

    // ── Generación de rutas ──────────────────────────────────────────────────

    private List<ParallelRouteTask.RouteRequest> generateRouteRequests() {
        List<Position> borderPositions = new ArrayList<>(getBorderPositions());
        Collections.shuffle(borderPositions, random);

        List<ParallelRouteTask.RouteRequest> requests = new ArrayList<>();
        int count = config.getVehicleCount();

        for (int i = 0; i < count; i++) {
            Position start = borderPositions.get(i % borderPositions.size());
            Position end;
            do {
                end = borderPositions.get(random.nextInt(borderPositions.size()));
            } while (end.equals(start));

            requests.add(new ParallelRouteTask.RouteRequest(i, start, end));
        }

        return requests;
    }

    private List<Position> getBorderPositions() {
        List<Position> borders = new ArrayList<>();
        int size = City.SIZE;

        for (int i = 0; i < size; i++) {
            borders.add(new Position(0, i));
            borders.add(new Position(size - 1, i));
            borders.add(new Position(i, 0));
            borders.add(new Position(i, size - 1));
        }

        return borders.stream().distinct().toList();
    }

    // ── Reporte final ────────────────────────────────────────────────────────

    private void printFinalReport() {
        System.out.println("\n====== REPORTE FINAL ======");
        System.out.printf("Vehículos completados : %d%n",    arrivedCount.get());
        System.out.printf("Primer en llegar      : Vehículo #%d%n", metrics.getFirstArrival());
        System.out.printf("Tiempo promedio viaje : %.0f ms%n", metrics.getAverageTravelTime());
        System.out.printf("Cálculo secuencial    : %d ms%n",  metrics.getSequentialRouteTime());
        System.out.printf("Cálculo paralelo      : %d ms%n",  metrics.getParallelRoutime());
        System.out.printf("Speedup               : %.2fx%n",  metrics.getSpeedup());
        System.out.printf("Velocidad vehículos   : %d ms/celda%n", config.getMoveDelayMs());
        System.out.println("===========================\n");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setState(SimulationState newState) {
        this.state = newState;
        if (listener != null) listener.onStateChanged(newState);
    }

    // ── Getters para la GUI ──────────────────────────────────────────────────

    public SimulationState getState()                    { return state; }
    public List<VehicleThread> getVehicles()             { return vehicleThreads; }
    public List<TrafficLightThread> getLightThreads()    { return lightThreads; }
    public City getCity()                                { return city; }
    public MetricsCollector getMetrics()                 { return metrics; }
    public SimulationConfig getConfig()                  { return config; }
    public int getArrivedCount()                         { return arrivedCount.get(); }
    public boolean isPaused()                            { return paused; }
    public double getSpeedup()                           { return routeCalculator.getSpeedup(); }
}