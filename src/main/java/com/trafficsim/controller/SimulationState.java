package com.trafficsim.controller;

public enum SimulationState {
    IDLE,           // Recien creado, sin inicializar
    CALCULATING,    // Calculando rutas (A* secuencial y paralelo)
    RUNNING,        // Simulacion corriendo
    PAUSED,         // Pausada por el usuario
    FINISHED        // Todos los vehiculos llegaron o fue detenida
}
