package com.trafficsim.threads;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.model.Intersection;
import com.trafficsim.model.Intersection.LightState;

public class TrafficLightThread extends Thread {
    private final Intersection intersection; // La interseccion que controla
    private final SimulationConfig config;   // Tiempos configurables por el usuario
    private volatile boolean running = true; // Bandera para detener el hilo limpiamente

    public TrafficLightThread(Intersection intersection, SimulationConfig config) {
        this.intersection = intersection;
        this.config = config;

        // setDaemon(true): este hilo muere automaticamente cuando el programa
        // principal termina. Sin esto, la JVM nunca cerraria porque los semaforos
        // corren en bucle infinito.
        setDaemon(true);

        // Nombre descriptivo - aparecera en el debugger y en los logs
        setName("Semaforo-" + intersection.getPosition());
    }

    @Override
    public void run() {
        // El semaforo ya arranco en RED dentro de assignSemaphore()
        // Aqui empezamos el ciclo normal: verde primero
        while (running && !isInterrupted()) {
            try {
                // --- VERDE: los vehiculos pueden cruzar ---
                intersection.setLightState(LightState.GREEN);
                Thread.sleep(config.getGreenMs());

                // --- AMARILLO: los vehiculos deben detenerse ---
                // Tratamos amarillo igual que rojo desde el punto de vista
                // de los vehiculos (isPassable() solo acepta GREEN)
                intersection.setLightState(LightState.YELLOW);
                Thread.sleep(config.getYellowMs());

                // --- ROJO: cruce bloqueado ---
                intersection.setLightState(LightState.RED);
                Thread.sleep(config.getRedMs());
            } catch (InterruptedException e) {
                // InterrupedException ocurre cuando alguien llama stop()
                // o cuanndo la JVM interrumpe el hilo al cerrar.
                // Restauramos la bandera de interrupcion y salimos limpiamente
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Esta funcion detiene el semaforo de forma segura desde afuera,
    // por ejemplo cuando paramos la simulacion
    public void stopLight() {
        running = false;
        interrupt(); // Despierta el Thread.sleep() para que salga inmediatamente
    }

    public Intersection getIntersection() {
        return intersection;
    }
}
