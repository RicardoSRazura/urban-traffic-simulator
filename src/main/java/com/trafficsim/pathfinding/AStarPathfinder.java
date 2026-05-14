package com.trafficsim.pathfinding;

import com.trafficsim.model.City;
import com.trafficsim.model.Position;

import java.util.*;

public class AStarPathfinder {
    private final City city;

    public AStarPathfinder(City city) {
        this.city = city;
    }

    // Metodo principal: devuelve la lista de posiciones desde start a end
    // incluyendo ambos extremos. Devuelve lista vacia si no hay camino.
    public List<Position> findPath(Position start, Position end) {

        //Caso trivial: ya estamos en el destino
        if (start.equals(end)) {
            return List.of(start);
        }

        // ---- Open set: nodos a evaluar, ordenados por f(n) ascendente ----
        // El comparator compara por fCost. Si hay empate, desempata por hCost
        // preferimos el nodo mas cercano al destino - ya que tiene mejor comportamiento visual
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt((Node n) -> n.fCost).thenComparingInt(n -> n.hCost));

        // ---- Closed set: posiciones ya evaluadas completamente ----
        Set<Position> closedSet = new HashSet<>();

        // ---- gCost: costo real acumulado desde el origen hasta cada nodo ----
        // Usamos Integer.MAX_VALUE como infinito para nodos no descubiertos
        Map<Position, Integer> gCost = new HashMap<>();

        // ---- cameFrom: de que posicion venimos - para reconstruir la ruta ----
        Map<Position, Position> cameFrom = new HashMap<>();

        //Inicializamos el nodo de origen
        int startH = start.manhattanDistance(end);
        gCost.put(start, 0);
        openSet.add(new Node(start, 0, startH));

        // ---- Bucle principal de A* ----
        while (!openSet.isEmpty()) {

            // Sacamos el nodo copn menor f(n) del open set
            Node current = openSet.poll();

            // Llegamos al destino?
            if (current.position.equals(end)) {
                return reconstructPath(cameFrom, end);
            }

            // Marcamos como evaluado - no lo procesamos de nuevo
            closedSet.add(current.position);

            // ---- Exploramos los vecino alcanzables ----
            // getReachableNeighbors respeta el sentido de las calles
            for (Position neighbor: city.getReachableNeighbors(current.position)) {

                // Si ya lo evaluamos, lo saltamos
                if (closedSet.contains(neighbor)) continue;

                // Costo real para llegar a este vecino desde el origen
                // En nuestro mapa cada paso cuesta 1, pero aqui podriamos
                // agregar costos por congestion o tipo de calle en el futuro si se necesita
                int tentativeG = gCost.getOrDefault(current.position, Integer.MAX_VALUE) + 1;

                // Se encontro el camino mas corto hacia este vecino
                if (tentativeG < gCost.getOrDefault(neighbor, Integer.MAX_VALUE)) {

                    // Actualizamos: este es el mejor camiuno conocido hacia 'neighbor'
                    cameFrom.put(neighbor, current.position);
                    gCost.put(neighbor, tentativeG);

                    int h = neighbor.manhattanDistance(end);
                    int f = tentativeG + h;

                    // Lo agregamos al open set para evaluarlo pronto
                    // Nota: si ya estaba en el open set con un f mayor,
                    // la priorityQueue tendra dos entradas del mismo nodo.
                    // El closedSet se encargara de ignorar el duplicado cuando salga.
                    openSet.add(new Node(neighbor, tentativeG, h));
                }
            }
        }

        // Si el open set se vacio sin llegar al destino, no hay camino posible
        // Esto puede pasar con calles de un solo sentido que crean callejones
        System.err.println("[A*] No se encontro ruta de " + start + " a " + end);
        return Collections.emptyList();
    }

    // Reconstruye la ruta siguiendo el mapa cameFrom hacia atras,
    // desde el destino hasta el origen, y luego invierte la lista.
    private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position end) {
        List<Position> path = new ArrayList<>();
        Position current = end;

        // Seguimos el rastro hacia atras hasta que no haya predecesor
        // el origen no tiene entrada en cameFrom
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current); // null cuando llegamos al origen
        }

        // La lista esta de destino a origen - entonces la invertimos
        Collections.reverse(path);
        return path;
    }

    // Esta es una clase interna Node - representa un nodo en el open set
    // Encapsula posicion + costos para la priorityQueue

    private static class Node {
        final Position position;
        final int gCost; // Costo real acumulado desde origen
        final int hCost; // Heuristica: distancia Manhattan al destino
        final int fCost; // f = g + h

        public Node(Position position, int gCost, int hCost) {
            this.position = position;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
    }
}
