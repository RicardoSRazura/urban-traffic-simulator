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

    // ---- Componentes principales ----
    private final City city;
    private final SimulationConfig config;
    private final MetricsCollector metrics;
    private final RouteCalculator routeCalculator;

    // ---- Hilos activos ----
    // CopyOnWriteArrayList: thread-safe para lectura desde la GUI
    // mientras el controlador agrega/elimina hilos
    private final CopyOnWriteArrayList<TrafficLightThread> lightThreads;
    private final CopyOnWriteArrayList<VehicleThread> vehicleThreads;

    // ---- Estado de la simulacion ----
    private volatile SimulationState state = SimulationState.IDLE;

    // Pausa: todos los vehiculos consultan esta bandera antes de cada paso
    private volatile boolean paused = false;

    // Contador de vehiculos que han llegado al destino
    private final AtomicInteger arrivedCount = new AtomicInteger(0);

    // Semilla para reproducibilidad - mismo valor = mismas posiciones aleatorias
    private final Random random = new Random(42);


    // Listener para notificar a la GUI de cambios de estado ----
    // La GUI implementa esta interfaz para saber cuando redibujar o
    // actualizar metricas sin necesidad de polling constante
    public interface SimulationListener {
        void onStateChanged(SimulationState newState);
        void onVehicleArrived(int vehicleId, long travelTimeMs);
        void onAllVehiclesArrived();
    }

    private SimulationListener listener;

    // ---- Constructor ----


    public SimulationController(SimulationConfig config) {
        this.config = config;
        this.city = new City();
        this.metrics = new MetricsCollector();
        this.routeCalculator = new RouteCalculator(city);
        this.lightThreads = new CopyOnWriteArrayList<>();
        this.vehicleThreads = new CopyOnWriteArrayList<>();
    }

    public void setListener(SimulationListener listener) {
        this.listener = listener;
    }

    // ---- Paso 1 - initialize()
    // Prepara la ciudad y calcula rutas. Se llama antes de start().
    // Se corre en un hilo separado para no bloquear la GUI mientras
    // A* calcula todas las rutas
    public void initialize() {
        setState(SimulationState.CALCULATING);

        // Hilo separado para no congelar la GUI durante el calculo
        Thread initThread = new Thread(() -> {
            //1a. Asignar semaforos a las intersecciones principales
            List<Position> semaphorePositions = city.getDefaultSemaphorePositions();
            city.assignSemaphores(semaphorePositions);

            //1b. Crear los hilos de semaforo (uno por interseccion con semaforo)
            for (Position pos : semaphorePositions) {
                Intersection inter = city.getIntersection(pos);
                TrafficLightThread lightThread = new TrafficLightThread(inter, config);
                lightThreads.add(lightThread);
            }

            //1c. Generar las peticiones de ruta para cada vehiculo
            List<ParallelRouteTask.RouteRequest> requests = generateRouteRequests();

            //1d. Calcular rutas secuencialmente (para medir tiempo base)
            routeCalculator.calculateSequential(requests);

            //1e. Calcular rutas en paralelo (las que usaran los vehiculos)
            List<List<Position>> routes = routeCalculator.calculateParallel(requests);

            //1f. Guardar tiempos de calculo en metricas
            metrics.setSequentialRouteTime(routeCalculator.getLastSequentialTimeMs());
            metrics.setParallelRoutime(routeCalculator.getLastParallelTimeMs());
            metrics.setSpeedup(routeCalculator.getSpeedup());

            //1g. Crear un VehicleThread por cada ruta valida calculada
            for (int i = 0; i < routes.size(); i++) {
                final int vehicleId = i;
                List<Position> route = routes.get(vehicleId);

                // Si A* no encontro una ruta, omitimos ese vehiculo
                if(route.isEmpty()) {
                    System.err.println("Vehiculo " + vehicleId + " sin ruta - omitido");
                    continue;
                }

                // Agrega este print de diagnóstico:
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
                        //Callback: cuando el vehiculo llega, notificamos al controlador
                        () -> onVehicleArrived(vehicleId)
                );
                vehicleThreads.add(vehicle);
            }

            // Listo para correr
            setState(SimulationState.IDLE);
        }, "Init-Thread");

        initThread.setDaemon(true);
        initThread.start();
    }

    // Paso 2 - Start()
    // Arranca todos los hilos. Solo valiudo si el estado es IDLE
    public void start() {
        if (state != SimulationState.IDLE) {
            System.err.println("No se puede iniciar: estado actual = " + state);
            return;
        }

        arrivedCount.set(0);
        setState(SimulationState.RUNNING);

        // Primero arrancamos los semaforos - deben estar corriendo
        // antes de que los vehiculos intenten cruzar intersecciones
        for (TrafficLightThread light : lightThreads) {
            light.start();
        }

        // Luego arrancamos los vehiculos
        for (VehicleThread vehicle : vehicleThreads) {
            vehicle.setPaused(false); // Aseguramos que no arranquen pausados
            vehicle.start();
        }
    }

    // Paso 3a - pause()
    // Congela los vehiculos en su posicion actual
    // Los semaforos siguen corriendo, esto lo hace un poco mas realista
    public void pause() {
        if (state != SimulationState.RUNNING) return;

        paused = true;
        //Notificamos a cada vehiculo par que se detenga en el siguiente paso
        for (VehicleThread vehicle : vehicleThreads) {
            vehicle.setPaused(true);
        }
        setState(SimulationState.PAUSED);
    }

    // Paso 3b - resume()
    // Reanuda los vehiculos desde donde estaban
    public void resume() {
        if (state != SimulationState.PAUSED) return;

        paused= false;
        for(VehicleThread vehicle : vehicleThreads) {
            vehicle.setPaused(false);
        }
        setState(SimulationState.RUNNING);
    }

    // Paso 4 - stop()
    // Interrumpe todos los hilos y limpia el estado
    public void stop() {
        setState(SimulationState.FINISHED);

        // Interrumpimos vehiculos
        for(VehicleThread vehicle : vehicleThreads) {
            vehicle.interrupt();
        }

        //Interrumpimos semaforos
        for(TrafficLightThread light : lightThreads) {
            light.stopLight();
        }
    }

    // Paso 5 - reset()
    // Detiene todo y limpia para poder volver a initialize() + start()
    public void reset() {
        stop();

        // Esperamos que todos los hilos terminen antes de limpiar
        // join() con timeout par no bloqeuarnos indefinidamente
        for (VehicleThread v: vehicleThreads) {
            try {
                v.join(500);
            } catch (InterruptedException ignored) {}
        }
        for (TrafficLightThread l : lightThreads) {
            try {
                l.join(500);
            } catch (InterruptedException ignored) {}
        }

        vehicleThreads.clear();
        lightThreads.clear();
        arrivedCount.set(0);
        metrics.reset();
        setState(SimulationState.IDLE);
    }

    // Callback interno: llamado por cada VehicleThread al llegar al destino
    private void onVehicleArrived(int vehicleId) {
        long travelTime = metrics.getTravelTime(vehicleId);
        int arrived = arrivedCount.incrementAndGet();

        // Notificamos a la GUI
        if (listener!= null) {
            listener.onVehicleArrived(vehicleId, travelTime);
        }

        // Llegaron todos llos vehiculos?
        if (arrived >= vehicleThreads.size()) {
            setState(SimulationState.FINISHED);
            if (listener != null) {
                listener.onAllVehiclesArrived();
            }
            printFinalReport();
        }
    }

    // Genera las peticiones de ruta para cada vehiculo
    // Los origenes y destinos se toman de los bordes del mapa
    // para simular vehiculos entrando y saliendo de la ciudad.
    private List<ParallelRouteTask.RouteRequest> generateRouteRequests() {
        List<Position> borderPositions = new ArrayList<>(getBorderPositions());
        Collections.shuffle(borderPositions, random);

        List<ParallelRouteTask.RouteRequest> requests = new ArrayList<>();
        int count = config.getVehicleCount();

        for (int i = 0; i < count; i++) {
            Position start = borderPositions.get(i % borderPositions.size());
            Position end;

            // Aseguramos que origen y destino sean diferentes
            do {
                end = borderPositions.get(random.nextInt(borderPositions.size()));
            } while (end.equals(start));

            requests.add(new ParallelRouteTask.RouteRequest(i, start, end));
        }

        return requests;
    }

    // Devuelve todas las posiciones del borde de la cuadricula 12x12
    // Son los puntos de entrada y salida de la ciudad
    private List<Position> getBorderPositions() {
        List<Position> borders = new ArrayList<>();
        int size = City.SIZE;

        for (int i = 0; i < size; i++) {
            borders.add(new Position(0, i)); // Borde superior
            borders.add(new Position(size - 1, i)); // Borde inferior
            borders.add(new Position(i, 0)); // Borde izquierdo
            borders.add(new Position(i, size - 1)); // Borde derecho
        }

        //Eliminamos duplicados de las esquinas
        return borders.stream().distinct().toList();
    }

    // Reporte final en consola
    private void printFinalReport() {
        System.out.println("\n====== REPORTE FINAL ======");
        System.out.printf("Vehículos completados : %d%n", arrivedCount.get());
        System.out.printf("Primer en llegar      : Vehículo #%d%n",
                metrics.getFirstArrival());
        System.out.printf("Tiempo promedio viaje : %.0f ms%n",
                metrics.getAverageTravelTime());
        System.out.printf("Cálculo secuencial    : %d ms%n",
                metrics.getSequentialRouteTime());
        System.out.printf("Cálculo paralelo      : %d ms%n",
                metrics.getParallelRoutime());
        System.out.printf("Speedup               : %.2fx%n",
                metrics.getSpeedup());
        System.out.println("===========================\n");
    }

    // Helpers de estado
    private void setState(SimulationState newState) {
        this.state = newState;
        if (listener != null) {
            listener.onStateChanged(newState);
        }
    }

    // Getters para la GUI

    public SimulationState getState() {
        return state;
    }

    public List<VehicleThread> getVehicles() {
        return vehicleThreads;
    }

    public List<TrafficLightThread> getLightThreads() {
        return lightThreads;
    }

    public City getCity() {
        return city;
    }

    public MetricsCollector getMetrics() {
        return metrics;
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public int getArrivedCount() {
        return arrivedCount.get();
    }

    public boolean isPaused() {
        return paused;
    }

    public double getSpeedup() {
        return routeCalculator.getSpeedup();
    }
}
