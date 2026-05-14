package com.trafficsim.pathfinding;

import com.trafficsim.model.City;
import com.trafficsim.model.Position;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class RouteCalculator {

    private final City city;

    // Guardamos los ultimos tiempos medidos para mostrarlos en metricas
    private long lastSequentialTimeMs;
    private long lastParallelTimeMs;

    public RouteCalculator (City city) {
        this.city = city;
    }

    // ---- Calculo Secuencial ----
    public List<List<Position>> calculateSequential(List<ParallelRouteTask.RouteRequest> requests){

        AStarPathfinder pathfinder = new AStarPathfinder(city);
        long start = System.nanoTime();

        List<List<Position>> routes = requests.stream()
                .map(req -> pathfinder.findPath(req.start(), req.end()))
                .toList();

        lastSequentialTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("[Secuencial] %d rutas en %d ms%n", requests.size(), lastSequentialTimeMs);

        return routes;
    }

    // ---- Calculo Paralelo con ForkJoinPool ----
    public List<List<Position>> calculateParallel( List<ParallelRouteTask.RouteRequest> requests) {

        // ForkJoinPool.commonPool() usa tantos hilos como nucleos disponibles
        // Puedes usar new ForkJoinPool(N) para forzar N hilos especificos
        ForkJoinPool pool = ForkJoinPool.commonPool();
        long start = System.nanoTime();

        ParallelRouteTask task = new ParallelRouteTask(requests, city);
        List<List<Position>> routes = pool.invoke(task);

        lastParallelTimeMs = (System.nanoTime() - start) / 1_000_000;
        // Evitamos división por cero
        double speedup = lastParallelTimeMs > 0
                ? (double) lastSequentialTimeMs / lastParallelTimeMs
                : lastSequentialTimeMs > 0 ? lastSequentialTimeMs : 0.0;
        System.out.printf("[Paralelo]   %d rutas en %d ms (x%.1f speedup)%n", requests.size(), lastParallelTimeMs, speedup);

        return routes;
    }

    public long getLastSequentialTimeMs() { return lastSequentialTimeMs; }
    public long getLastParallelTimeMs() { return lastParallelTimeMs; }

    // Speedup real: cuantas veces fue mas rapido el paralelo
    public double getSpeedup() {
        if (lastParallelTimeMs == 0) return 0.0;
        return (double) lastSequentialTimeMs / lastParallelTimeMs;
    }
}
