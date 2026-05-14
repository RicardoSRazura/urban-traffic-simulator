package com.trafficsim.model;

import java.util.ArrayList;
import java.util.List;

public class City {
    public static final int SIZE = 12; //Cuadricula de 12x12

    // La cuadricula de intersecciones: [fila][columna]
    // Ejemplo: intersections[3][4] es la inteseccion en row=3, col=4
    private final Intersection[][] intersections;

    //Lista de todas las calles
    private final List<Street> streets;

    public City() {
        this.intersections = new Intersection[SIZE][SIZE];
        this.streets = new ArrayList<>();
        initializeIntersections();
        initializeStreets();
    }

    // Crea las 144 intersecciones (12x12), cada una con su posicion
    private void initializeIntersections(){
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                intersections[row][col] = new Intersection(new Position(row, col));
            }
        }
    }

    // Crea todas las calles conectando intersecciones vecinas
    // Hay dos tipos de conexiones:
    // - Horizontales: (row, col) <-> (row, col+1) -> en total 12 filas x 11 pares = 132
    // - Verticales: (row, col) <-> (row+1, col) -> en total 11 pares x 12 cols = 132
    //                                                               Total de 264 calles
    private void initializeStreets() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {

                // Calle horizontal -> conecta con el vecino de la derecha
                if (col + 1 < SIZE) {
                    Position from = new Position(row, col);
                    Position to = new Position(row, col + 1);

                    // Calles pares son doble sentido, impares de un sentido
                    // Esto da variedad al mapa y hace que el routing se aprecie de mejor manera
                    Street.Direction dir = (row % 2 == 0)
                            ? Street.Direction.TWO_WAY
                            : Street.Direction.ONE_WAY;
                    streets.add(new Street("Calle H-" + row + "-" + col, from, to, dir));
                }

                // Calle vertical -> Conecta con el vecino de abajo
                if (row + 1 < SIZE) {
                    Position from = new Position(row, col);
                    Position to = new Position(row + 1, col);

                    Street.Direction dir;
                    if (col % 2 == 0) {
                        //Columnas pares: doble sentido
                        dir = Street.Direction.TWO_WAY;
                    } else {
                        // Columnas impares alternan: pares van abajo, impares van a arriba
                        // Esto evita que haya callejones sin salida
                        dir = (col % 4 == 1)
                                ? Street.Direction.ONE_WAY // col 1,5,9: de arriba hacia abajo
                                : Street.Direction.TWO_WAY; // col 3,7,11: doble sentido
                    }

                    streets.add(new Street("Calle V-" + row + "-" + col, from, to, dir));
                }
            }
        }
    }

    // ---- Consultas que usaran los vehiculos y A* ----

    //Devuelve la interseccion en esta posicion, o null si esta fuera del mapa
    public Intersection getIntersection(Position pos) {
        if (!isValid(pos)) return null;
        return intersections[pos.row][pos.col];
    }

    // La posicion esta dentro de la cuadricula?
    public boolean isValid(Position pos) {
        return pos.row >= 0 && pos.row < SIZE
                && pos.col >= 0 && pos.col < SIZE;
    }

    // Devuelve las calles que salen de una posicion dada.
    // A* llama a esto para saber a donde puede moverse desde un nodo
    public List<Street> getStreetsFrom(Position origin) {
        List<Street> result = new ArrayList<>();
        for (Street s : streets) {
            if (s.canTraverse(origin)){
                result.add(s);
            }
        }
        return result;
    }

    // Devuelve las posiciones vecinas a las que se puede llegar desde 'origin'
    // respetando el sentido de las calles. Esto es lo que consume A* para funcionar
    public List<Position> getReachableNeighbors(Position origin) {
        List<Position> neighbors = new ArrayList<>();
        for (Street s : getStreetsFrom(origin)) {
            neighbors.add(s.getDestination(origin));
        }
        return neighbors;
    }

    // Asigna semaforos a intersecciones especificas del mapa
    // Las posiciones elegidas son intersecciones "principales" -
    // cruces de avenidas importantes, distribuidas por toda la ciudad
    public void assignSemaphores(List<Position> positions){
        for(Position pos : positions) {
            Intersection intersection = getIntersection(pos);
            if(intersection != null){
                intersection.assignSemaphore();
            }
        }
    }

    // Genera las posiciones recomendadas para semaforos (entre 10 y 20)
    // Estan distribuidas en una cuadricula interior para cubrir toda la ciudad
    public List<Position> getDefaultSemaphorePositions() {
        List<Position> positions = new ArrayList<>();
        //Coloca semaforos cada 3 nodos en filas/columnas interiores
        for (int row = 2; row < SIZE - 1; row += 3) {
            for (int col = 2; col < SIZE - 1; col += 3) {
                positions.add(new Position(row, col));
            }
        }
        return positions; //Esto genera exactamente 16 posiciones de semaforos
    }

    public List<Street> getStreets(){
        return streets;
    }
    public Intersection[][] getIntersections() {
        return intersections;
    }
    public int getSize() {
        return SIZE;
    }
}
