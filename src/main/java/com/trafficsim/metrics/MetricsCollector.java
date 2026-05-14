package com.trafficsim.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsCollector {

    // ConcurrentHashMap es thread-safe: multiples vehiculos pueden
    // escribir sus metricas al mismo tiempo sin syncronized manual.
    private final ConcurrentHashMap<Integer, Long> departureTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> arrivalTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> waitTimes = new ConcurrentHashMap<>();

    private volatile long sequentialRouteTime;
    private volatile long parallelRoutime;
    private volatile double speedup;

    public void recordDeparture(int vehicleId, long timeMs) {
        departureTimes.put(vehicleId, timeMs);
    }

    public void recordArrival(int vehicleId, long timeMs) {
        arrivalTimes.put(vehicleId, timeMs);
    }

    //Acumula tiempo de espera (Puede llamarse multiples veces por vehiculo)
    public void addWaitTime(int vehicleId, long waitMs) {
        waitTimes.merge(vehicleId, waitMs, Long::sum);
    }

    //Tiempo total de viaje de un vehiculo
    public long getTravelTime(int vehicleId) {
        Long dep = departureTimes.get(vehicleId);
        Long arr = arrivalTimes.get(vehicleId);
        if (dep == null || arr == null) return -1;
        return arr - dep;
    }

    // Metrica para saber cual vehiculo llego primero
    public int getFirstArrival() {
        return arrivalTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    // Tiempo promedio de viaje de todos los vehiculos que llegaron
    public double getAverageTravelTime() {
        return arrivalTimes.keySet().stream()
                .mapToLong(this::getTravelTime)
                .filter(t -> t >= 0)
                .average()
                .orElse(0.0);
    }

    // Tiempo total que un vehiculo paso esperando (semaforos + locks)
    public long getWaitTime(int vehicleId) {
        return waitTimes.getOrDefault(vehicleId, 0L);
    }

    public Map<Integer, Long> getAllArrivalTimes() { return arrivalTimes; }
    public Map<Integer, Long> getAllWaitTimes() { return  waitTimes; }

    public long getSequentialRouteTime() {
        return sequentialRouteTime;
    }

    public void setSequentialRouteTime(long sequentialRouteTime) {
        this.sequentialRouteTime = sequentialRouteTime;
    }

    public long getParallelRoutime() {
        return parallelRoutime;
    }

    public void setParallelRoutime(long parallelRoutime) {
        this.parallelRoutime = parallelRoutime;
    }

    public double getSpeedup() {
        return speedup;
    }

    public void setSpeedup(double speedup) {
        this.speedup = speedup;
    }

    // Metodo reset para cuando el usuario reinicia la simulacion:
    public void reset() {
        departureTimes.clear();
        arrivalTimes.clear();
        waitTimes.clear();
        sequentialRouteTime = 0;
        parallelRoutime = 0;
        speedup = 0;
    }
}
