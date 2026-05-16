package com.trafficsim.threads;

import com.trafficsim.metrics.MetricsCollector;
import com.trafficsim.model.City;
import com.trafficsim.model.Intersection;
import com.trafficsim.model.Position;

import java.util.List;

public class VehicleThread extends Thread {

    // ── Estado del vehículo ──────────────────────────────────────────────────
    private final int vehicleId;
    private final List<Position> route;
    private final City city;
    private final MetricsCollector metrics;

    private volatile boolean paused = false;
    private final Runnable onArrivalCallback;

    private volatile Position currentPosition;

    // Posición SIGUIENTE durante el cruce — necesaria para interpolar
    private volatile Position nextPosition;

    // Progreso de 0.0 a 1.0 entre currentPosition y nextPosition
    // volatile para que el renderer lo lea sin sincronización extra
    private volatile double moveProgress = 0.0;

    public enum VehicleState { MOVING, WAITING_LIGHT, WAITING_LOCK, ARRIVED }
    private volatile VehicleState state = VehicleState.MOVING;

    // ms por celda — viene de SimulationConfig (slider de la GUI)
    private final int moveDelayMs;

    // Pasos de interpolación por cruce: más pasos = más fluido
    private static final int INTERPOLATION_STEPS = 20;

    private static final int RETRY_LIGHT_MS = 100;
    private static final int RETRY_LOCK_MS  = 50;

    public VehicleThread(int vehicleId, List<Position> route, City city,
                         MetricsCollector metrics, Runnable onArrivalCallback,
                         int moveDelayMs) {
        this.vehicleId         = vehicleId;
        this.route             = route;
        this.city              = city;
        this.metrics           = metrics;
        this.currentPosition   = route.get(0);
        this.nextPosition      = route.get(0);
        this.onArrivalCallback = onArrivalCallback;
        this.moveDelayMs       = moveDelayMs;

        setName("Vehiculo-" + vehicleId);
        setDaemon(true);
    }

    @Override
    public void run() {
        long departureTime = System.currentTimeMillis();
        metrics.recordDeparture(vehicleId, departureTime);

        for (int i = 1; i < route.size(); i++) {
            if (isInterrupted()) break;
            moveToPosition(route.get(i));
        }

        long arrivalTime = System.currentTimeMillis();
        state = VehicleState.ARRIVED;
        moveProgress = 1.0;
        metrics.recordArrival(vehicleId, arrivalTime);

        if (onArrivalCallback != null) {
            onArrivalCallback.run();
        }

        System.out.println("[" + getName() + "] llegó a destino en "
                + (arrivalTime - departureTime) + " ms");
    }

    private void moveToPosition(Position next) {
        Intersection target = city.getIntersection(next);

        long waitStart = 0;
        boolean waited = false;

        while (!isInterrupted()) {

            // Pausa
            while (paused && !isInterrupted()) {
                try { Thread.sleep(50); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (isInterrupted()) return;

            // Paso 1: semáforo
            if (!target.isPassable()) {
                state = VehicleState.WAITING_LIGHT;
                if (!waited) { waitStart = System.currentTimeMillis(); waited = true; }
                target.incrementWaitCount();
                try { Thread.sleep(RETRY_LIGHT_MS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                continue;
            }

            // Paso 2: lock
            if (!target.tryEnter()) {
                state = VehicleState.WAITING_LOCK;
                if (!waited) { waitStart = System.currentTimeMillis(); waited = true; }
                try { Thread.sleep(RETRY_LOCK_MS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                continue;
            }

            // Paso 3: cruzar con animación interpolada
            try {
                state = VehicleState.MOVING;

                if (waited) {
                    metrics.addWaitTime(vehicleId, System.currentTimeMillis() - waitStart);
                }

                // En lugar de un solo Thread.sleep(moveDelayMs),
                // dividimos el cruce en INTERPOLATION_STEPS pasos pequeños.
                // En cada paso actualizamos moveProgress para que el renderer
                // dibuje la posición exacta entre las dos celdas → movimiento suave.
                nextPosition = next;
                long stepDelay = Math.max(1, moveDelayMs / INTERPOLATION_STEPS);

                for (int step = 1; step <= INTERPOLATION_STEPS; step++) {
                    if (isInterrupted() || paused) break;
                    moveProgress = (double) step / INTERPOLATION_STEPS;
                    try { Thread.sleep(stepDelay); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Posición discreta actualizada al terminar la animación
                currentPosition = next;
                nextPosition    = next;
                moveProgress    = 0.0;

            } finally {
                target.exit();
            }

            break;
        }
    }

    public void setPaused(boolean paused) { this.paused = paused; }

    // ── Getters para la GUI ──────────────────────────────────────────────────

    public int getVehicleId()             { return vehicleId; }
    public Position getCurrentPosition()  { return currentPosition; }
    public Position getNextPosition()     { return nextPosition; }

    /**
     * Progreso de 0.0 a 1.0 entre currentPosition y nextPosition.
     * VehicleRenderer lo usa para interpolar la posición en píxeles.
     */
    public double getMoveProgress()       { return moveProgress; }

    public VehicleState getVeichleState() { return state; }
    public List<Position> getRoute()      { return route; }
    public Position getDestination()      { return route.get(route.size() - 1); }
    public Position getOrigin()           { return route.get(0); }

    public double getProgressPercent() {
        if (route.size() <= 1) return 100.0;
        int currentIndex = route.indexOf(currentPosition);
        return (currentIndex / (double)(route.size() - 1)) * 100.0;
    }
}