package com.trafficsim.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector {

    private final ConcurrentHashMap<Integer, Long> departureTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> arrivalTimes   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> waitTimes      = new ConcurrentHashMap<>();

    private volatile long   sequentialRouteTime;
    private volatile long   parallelRouteTime;
    private volatile double speedup;

    private final AtomicInteger avoidedCollisions = new AtomicInteger(0);
    private final AtomicInteger totalLightWaits   = new AtomicInteger(0);
    private final AtomicInteger totalLockWaits    = new AtomicInteger(0);

    public void recordDeparture(int vehicleId, long timeMs) { departureTimes.put(vehicleId, timeMs); }
    public void recordArrival(int vehicleId, long timeMs)   { arrivalTimes.put(vehicleId, timeMs); }
    public void addWaitTime(int vehicleId, long waitMs)     { waitTimes.merge(vehicleId, waitMs, Long::sum); }
    public void recordLightWait()  { totalLightWaits.incrementAndGet(); }
    public void recordLockWait()   { totalLockWaits.incrementAndGet(); avoidedCollisions.incrementAndGet(); }

    public long getTravelTime(int vehicleId) {
        Long dep = departureTimes.get(vehicleId);
        Long arr = arrivalTimes.get(vehicleId);
        if (dep == null || arr == null) return -1;
        return arr - dep;
    }

    public int getFirstArrival() {
        return arrivalTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(-1);
    }

    public long getFastestTravelTime() {
        return arrivalTimes.keySet().stream().mapToLong(this::getTravelTime)
                .filter(t -> t >= 0).min().orElse(0);
    }

    public long getSlowestTravelTime() {
        return arrivalTimes.keySet().stream().mapToLong(this::getTravelTime)
                .filter(t -> t >= 0).max().orElse(0);
    }

    public double getAverageTravelTime() {
        return arrivalTimes.keySet().stream().mapToLong(this::getTravelTime)
                .filter(t -> t >= 0).average().orElse(0.0);
    }

    public long getTotalWaitTime() {
        return waitTimes.values().stream().mapToLong(Long::longValue).sum();
    }

    public double getAverageWaitTime() {
        if (waitTimes.isEmpty()) return 0.0;
        return waitTimes.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public double getCongestionIndex() {
        double totalTravel = arrivalTimes.keySet().stream()
                .mapToLong(this::getTravelTime).filter(t -> t >= 0).sum();
        if (totalTravel == 0) return 0.0;
        return (getTotalWaitTime() / totalTravel) * 100.0;
    }

    public long getWaitTime(int vehicleId)       { return waitTimes.getOrDefault(vehicleId, 0L); }
    public int  getAvoidedCollisions()           { return avoidedCollisions.get(); }
    public int  getTotalLightWaits()             { return totalLightWaits.get(); }
    public int  getTotalLockWaits()              { return totalLockWaits.get(); }
    public Map<Integer, Long> getAllArrivalTimes(){ return arrivalTimes; }
    public Map<Integer, Long> getAllWaitTimes()   { return waitTimes; }

    public long   getSequentialRouteTime()       { return sequentialRouteTime; }
    public void   setSequentialRouteTime(long t) { this.sequentialRouteTime = t; }
    public long   getParallelRoutime()           { return parallelRouteTime; }
    public void   setParallelRoutime(long t)     { this.parallelRouteTime = t; }
    public double getSpeedup()                   { return speedup; }
    public void   setSpeedup(double s)           { this.speedup = s; }

    public void reset() {
        departureTimes.clear();
        arrivalTimes.clear();
        waitTimes.clear();
        avoidedCollisions.set(0);
        totalLightWaits.set(0);
        totalLockWaits.set(0);
        sequentialRouteTime = 0;
        parallelRouteTime   = 0;
        speedup             = 0;
    }
}