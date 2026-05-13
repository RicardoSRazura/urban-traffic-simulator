package com.trafficsim.model;

import java.util.Objects;

public class Position {
    //Fila y columna en la cuadricula (0-indexed)
    // row=0 es la fila de arriba, col=0 es la columna de la izquierda
    public final int row;
    public final int col;

    public Position(int row, int col){
        this.row = row;
        this.col = col;
    }

    // Esta funcion devuelve los 4 vecino ortogonales(arriba, abajo, izquierda, derecha)
    // Usado por A* para explorar posibles pasos
    public Position[] neighbors() {
        return new Position[] {
                new Position(row - 1, col), //arriba
                new Position(row + 1, col), //abajo
                new Position(row, col - 1), //Izquierda
                new Position(row, col + 1) // derecha
        };
    }

    // Distancia Manhattan - la heuristica de A*
    // Es la cantidad minima de pasos de una cuadricula sin diagonales
    public int manhattanDistance(Position other) {
        return Math.abs(this.row - other.row) + Math.abs(this.col - other.col);
    }

    // equals y hashCode son necesarios porque Position se usa como
    // clave en HashMap y HashSet dentro de A* y MetricsCollector

    @Override
    public boolean equals(Object o) {
        if (this == o ) return true;
        if (!(o instanceof Position p)) return false;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
