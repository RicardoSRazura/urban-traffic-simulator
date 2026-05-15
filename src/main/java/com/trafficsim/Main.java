package com.trafficsim;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.controller.SimulationController;
import com.trafficsim.controller.SimulationState;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("===========================================");
        System.out.println("   SIMULADOR DE TRÁFICO URBANO - CONSOLA  ");
        System.out.println("===========================================\n");

        // --- Configuración de prueba ---
        SimulationConfig config = new SimulationConfig();
        config.setVehicleCount(10);   // Pocos vehículos para ver bien la consola
        config.setGreenMs(3000);
        config.setYellowMs(1000);
        config.setRedMs(4000);

        // --- Crear controlador ---
        SimulationController controller = new SimulationController(config);

        // --- Listener que imprime eventos en consola ---
        controller.setListener(new SimulationController.SimulationListener() {

            @Override
            public void onStateChanged(SimulationState newState) {
                System.out.println("[ESTADO] --> " + newState);
            }

            @Override
            public void onVehicleArrived(int vehicleId, long travelTimeMs) {
                System.out.printf("[LLEGÓ] Vehículo #%d  |  Tiempo: %d ms%n",
                        vehicleId, travelTimeMs);
            }

            @Override
            public void onAllVehiclesArrived() {
                System.out.println("\n[FIN] Todos los vehículos llegaron.");
            }
        });

        // --- PASO 1: Inicializar (calcula rutas) ---
        System.out.println("[ACCION] Inicializando...");
        controller.initialize();

        // Esperamos a que initialize() termine en su hilo interno
        // (pasa de CALCULATING a IDLE)
        while (controller.getState() == SimulationState.IDLE
                && controller.getVehicles().isEmpty()) {
            Thread.sleep(100);
        }
        while (controller.getState() == SimulationState.CALCULATING) {
            Thread.sleep(100);
        }

        System.out.println("[INFO] Vehículos creados: " + controller.getVehicles().size());
        System.out.printf("[INFO] A* secuencial : %d ms%n",
                controller.getMetrics().getSequentialRouteTime());
        System.out.printf("[INFO] A* paralelo   : %d ms%n",
                controller.getMetrics().getParallelRouteTime());
        System.out.printf("[INFO] Speedup       : %.2fx%n\n",
                controller.getMetrics().getSpeedup());

        // --- PASO 2: Iniciar simulación ---
        System.out.println("[ACCION] Iniciando simulación...\n");
        controller.start();

        // --- Esperamos a que todos lleguen o timeout de 60 segundos ---
        long timeout = System.currentTimeMillis() + 60_000;
        while (controller.getState() == SimulationState.RUNNING
                && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);

            // Imprimimos progreso cada 2 segundos
            int arrived = controller.getArrivedCount();
            int total   = controller.getVehicles().size();
            System.out.printf("[PROGRESO] %d / %d vehículos llegaron%n",
                    arrived, total);
        }

        // --- PASO 3: Detener y mostrar reporte ---
        if (controller.getState() == SimulationState.RUNNING) {
            System.out.println("\n[TIMEOUT] Deteniendo por tiempo máximo...");
            controller.stop();
        }

        // Pequeña pausa para que los últimos callbacks lleguen
        Thread.sleep(500);

        // --- Reporte final ---
        System.out.println("\n===========================================");
        System.out.println("              REPORTE FINAL               ");
        System.out.println("===========================================");
        System.out.printf("Vehículos totales    : %d%n",
                controller.getVehicles().size());
        System.out.printf("Vehículos llegados   : %d%n",
                controller.getArrivedCount());
        System.out.printf("Primero en llegar    : Vehículo #%d%n",
                controller.getMetrics().getFirstArrival());
        System.out.printf("Tiempo prom. viaje   : %.0f ms%n",
                controller.getMetrics().getAverageTravelTime());
        System.out.printf("A* secuencial        : %d ms%n",
                controller.getMetrics().getSequentialRouteTime());
        System.out.printf("A* paralelo          : %d ms%n",
                controller.getMetrics().getParallelRouteTime());
        System.out.printf("Speedup              : %.2fx%n",
                controller.getMetrics().getSpeedup());
        System.out.println("===========================================");
    }
}
