package com.trafficsim.pathfinding;

import com.trafficsim.model.City;
import com.trafficsim.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

// RecursiveTask<T> es la base de ForkJoinPool para tareas que devuelven valor
// T = List<List<Position>> - una lista de rutas, una por vehiculo
public class ParallelRouteTask extends RecursiveTask<List<List<Position>>> {

    //Umbral: si hay ese numero de vehiculos o menos, calculamos secuencialmente
    // sin dividir mas. Se puede ajustar segun el numero de nucleos que esten disponibles.
    private static final int THRESHOLD = 4;

    private final List<RouteRequest> requests; // Lista de pares (origen , destino)
    private final City city;

    // RouteRequest: simple contenedor de un par origen-destino por vehiculo
    public record RouteRequest(int vehicleId, Position start, Position end) {}

    public ParallelRouteTask(List<RouteRequest> requests, City city) {
        this.requests = requests;
        this.city = city;
    }

    @Override
    protected List<List<Position>> compute() {

        //Caso base: pocos vehiculos - calculamos directamente en este hilo
        if (requests.size() <= THRESHOLD) {
            return computeSequentially();
        }

        //Caso recursivo: dividimos en dos mitades
        int mid = requests.size()/2;
        List<RouteRequest> leftHalf = requests.subList(0, mid);
        List<RouteRequest> rightHalf = requests.subList(mid, requests.size());

        // Creamos dos subtareas
        ParallelRouteTask leftTask = new ParallelRouteTask(leftHalf, city);
        ParallelRouteTask rightTask = new ParallelRouteTask(rightHalf, city);

        // fork() envia leftTask a otro hilo del pool (asincrono)
        leftTask.fork();

        // compute() ejecuta rightTask en este mismo hilo (sincrono
        // Este patron fork-izquierda/compute-derecha es el estandar de ForkJoin
        List<List<Position>> rightResult = rightTask.compute();

        // join() espera a que leftTask termine y obtiene el resultado
        List<List<Position>> leftResult = leftTask.join();

        // Combinamos ambos resultados en orden
        List<List<Position>> combined = new ArrayList<>(leftResult);
        combined.addAll(rightResult);
        return combined;
    }

    // Calcula las rutas de este subgrupo de forma secuencial
    private List<List<Position>> computeSequentially() {
        AStarPathfinder pathfinder = new AStarPathfinder(city);
        // Nota: cada hilo tiene su propio AStarPathfinder
        // AStarPathfinder no tiene estado compartido (todas sus estructuras
        // son locales al metodo findPath), por lo que es thread-safe por diseño
        List<List<Position>> routes = new ArrayList<>();
        for (RouteRequest req : requests) {
            routes.add(pathfinder.findPath(req.start(), req.end()));
        }
        return routes;
    }
}
