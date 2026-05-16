package com.trafficsim.config;

public class SimulationConfig {

    // Tiempos del semaforo en milisegundos
    private int greenMs  = 5000;
    private int yellowMs = 2000;
    private int redMs    = 6000;

    // Cantidad de vehiculos en la simulacion
    private int vehicleCount = 30;

    // Tiempo de cruce por celda en ms (controla la velocidad visual de los vehiculos)
    // Valor alto = vehiculos lentos, valor bajo = vehiculos rapidos
    private int moveDelayMs = 800;

    // Constructor con valores por defecto
    public SimulationConfig() {}

    // Constructor completo
    public SimulationConfig(int greenMs, int yellowMs, int redMs, int vehicleCount) {
        this.greenMs      = greenMs;
        this.yellowMs     = yellowMs;
        this.redMs        = redMs;
        this.vehicleCount = vehicleCount;
    }

    // Constructor completo con velocidad
    public SimulationConfig(int greenMs, int yellowMs, int redMs, int vehicleCount, int moveDelayMs) {
        this.greenMs      = greenMs;
        this.yellowMs     = yellowMs;
        this.redMs        = redMs;
        this.vehicleCount = vehicleCount;
        this.moveDelayMs  = moveDelayMs;
    }

    // ── Getters y Setters ────────────────────────────────────────────────────

    public int getGreenMs()  { return greenMs; }
    public void setGreenMs(int greenMs) { this.greenMs = greenMs; }

    public int getYellowMs() { return yellowMs; }
    public void setYellowMs(int yellowMs) { this.yellowMs = yellowMs; }

    public int getRedMs()    { return redMs; }
    public void setRedMs(int redMs) { this.redMs = redMs; }

    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }

    public int getMoveDelayMs() { return moveDelayMs; }
    public void setMoveDelayMs(int moveDelayMs) { this.moveDelayMs = moveDelayMs; }
}