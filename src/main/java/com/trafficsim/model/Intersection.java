package com.trafficsim.model;

import java.util.concurrent.locks.ReentrantLock;

public class Intersection {
    // Enum de los estados posibles del semaforo
    public enum LightState { GREEN, YELLOW, RED }

    private final Position position;            // Coordenada de esta interseccion
    private final ReentrantLock lock;           // Mutex - el corazon de la sincronizacion
    private volatile LightState lightState;     // volatile: esto hace visible entre hilos sin syncronized
    private volatile boolean hasSemaphore;      // Una bandera para indicar si tiene algun semaforo asignado la interseccion

    //Para metricas: cuantos vehiculos han esperado aqui
    private volatile int waitCount = 0;


    public Intersection(Position position) {
        this.position = position;
        //fair=true: los hilos esperan en orden FIFO - mas justo, evita la inanicion
        //fair=false: seria mas rapido pero un hilo podria esperar para siempre
        this.lock = new ReentrantLock(true);
        this.lightState = LightState.GREEN; //Se coloca por defecto verde por el caso de que no tenga semaforo
        this.hasSemaphore = false;
    }

    // ---- Metodos de Sincronizacion ----

    // Intenta adquirir un lock sin bloquearse
    // Devuelve true si lo consiguio, false si otro hilo ya lo tiene
    // El vehiculo usara esto en un bucle: "intenta, si falla, espera un poco y vuelve"
    public boolean tryEnter() {
        return lock.tryLock();
    }

    // Libera el lock. Siempre debe llamarse despues de que tryEnter() haya sido exitoso.
    // La comprobacion isHeldByCurrentThread() es una salvaguarda:
    //  evita que un hilo diferente al que entro intente liberar el lock
    public void exit() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    // Este metodo lo que hace es preguntarse si un vehiculo puede cruzar ahora mismo
    // Para esto necesito que el semaforo este en verde o que no tengo semaforo
    public boolean isPassable() {
        if(!hasSemaphore) return true;          //Sin semaforo: siempre verde
        return lightState == LightState.GREEN;  //Con semaforo: solo en verde
    }

    // ---- Metodos del semaforo (estos metodos seran llamados por TrafficLightThread) ----

    public void setLightState(LightState state) {
        // Como es volatile se garantiza la visibilidad inmediata en todos los hilos
        this.lightState = state;
    }

    public void assignSemaphore() {
        this.hasSemaphore = true;
        this.lightState = LightState.RED; // Empieza en rojo al activar semaforo
    }

    // ---- Metricas ----

    //Llamado por VehicleThread cuando un vehiculo no puedo entrar y tuvo que esperar
    public void incrementWaitCount() {
        waitCount++; //Esto no necesita sincronizacion exacta ya que solo es estadistica aproximada
    }

    // ---- Getters ----

    public Position getPosition() {
        return position;
    }

    public LightState getLightState() {
        return lightState;
    }

    public boolean isHasSemaphore() {
        return hasSemaphore;
    }

    public int getWaitCount() {
        return waitCount;
    }

    // Con esta funcion preguntamos si esta ocupada la interseccion por un vehiculo
    // isLocked() devuelve ture si el lock esta tomado por algun hilo
    public boolean isOccupied() {
        return lock.isLocked();
    }

    @Override
    public String toString() {
        return "Intersection" + position + (hasSemaphore ? "[" + lightState + "]" : "[libre]");

    }
}
