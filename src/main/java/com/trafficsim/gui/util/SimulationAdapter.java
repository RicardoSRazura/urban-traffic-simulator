package com.trafficsim.gui.util;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.controller.SimulationController;
import com.trafficsim.controller.SimulationController.SimulationListener;
import com.trafficsim.controller.SimulationState;
import com.trafficsim.model.City;
import com.trafficsim.threads.TrafficLightThread;
import com.trafficsim.threads.VehicleThread;

import java.util.List;

/**
 * Puente entre SimulationController (lógica) y la GUI (presentación).
 * Los paneles solo conocen este adaptador, nunca el controlador directamente.
 */
public class SimulationAdapter {

    // ── Interfaz que implementa la GUI ───────────────────────────────────────
    public interface GuiListener {
        void onStateChanged(SimulationState state);
        void onVehicleArrived(int vehicleId, long travelTimeMs);
        void onAllVehiclesArrived();
    }

    private SimulationController controller;
    private GuiListener guiListener;

    // ── API pública para los paneles ─────────────────────────────────────────

    public void initialize(SimulationConfig config) {
        this.controller = new SimulationController(config);

        controller.setListener(new SimulationListener() {
            @Override
            public void onStateChanged(SimulationState newState) {
                if (guiListener != null) guiListener.onStateChanged(newState);
            }
            @Override
            public void onVehicleArrived(int vehicleId, long travelTimeMs) {
                if (guiListener != null) guiListener.onVehicleArrived(vehicleId, travelTimeMs);
            }
            @Override
            public void onAllVehiclesArrived() {
                if (guiListener != null) guiListener.onAllVehiclesArrived();
            }
        });

        controller.initialize();
    }

    public void start()  { if (controller != null) controller.start();  }
    public void pause()  { if (controller != null) controller.pause();  }
    public void resume() { if (controller != null) controller.resume(); }
    public void stop()   { if (controller != null) controller.stop();   }
    public void reset()  { if (controller != null) controller.reset();  }

    // ── Acceso de solo-lectura para el canvas (~60 fps) ──────────────────────

    public List<VehicleThread> getVehicles() {
        return controller != null ? controller.getVehicles() : List.of();
    }

    public List<TrafficLightThread> getLightThreads() {
        return controller != null ? controller.getLightThreads() : List.of();
    }

    public City getCity() {
        return controller != null ? controller.getCity() : null;
    }

    public SimulationState getState() {
        return controller != null ? controller.getState() : SimulationState.IDLE;
    }

    public int getArrivedCount() {
        return controller != null ? controller.getArrivedCount() : 0;
    }

    public double getAverageTravelTime() {
        return controller != null ? controller.getMetrics().getAverageTravelTime() : 0;
    }

    public double getSpeedup() {
        return controller != null ? controller.getSpeedup() : 0;
    }

    public long getSequentialRouteTime() {
        return controller != null ? controller.getMetrics().getSequentialRouteTime() : 0;
    }

    public long getParallelRouteTime() {
        return controller != null ? controller.getMetrics().getParallelRoutime() : 0;
    }

    // ── Helpers de estado ────────────────────────────────────────────────────

    public boolean isRunning() {
        return getState() == SimulationState.RUNNING;
    }

    public boolean isPaused() {
        return getState() == SimulationState.PAUSED;
    }

    public boolean isCalculating() {
        return getState() == SimulationState.CALCULATING;
    }

    public void setGuiListener(GuiListener listener) {
        this.guiListener = listener;
    }
}