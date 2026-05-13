package com.trafficsim.model;

public class Street {

    // Enum para el tipo de sentido de la calle
    public enum Direction {
        ONE_WAY, // Solo se puede ir de 'from' hacia 'to'
        TWO_WAY // Se puede ir en ambas direcciones
    }

    private final String name;
    private final Position from; // Interseccion de inicio
    private final Position to; // Interseccion de fin
    private final Direction direction; // Un sentido o doble sentido

    public Street(String name, Position from, Position to, Direction direction) {
        this.name = name;
        this.from = from;
        this.to = to;
        this.direction = direction;
    }

    // Lo que hace A* es preguntarse si un vehiculo puede viajar desde 'origin' usando esta calle o alguna otra?
    // Y haciendo eso es como comienza a explorar vecinos
    public boolean canTraverse(Position origin) {
        if (direction == Direction.TWO_WAY){
            // Doble sentido: puede venir de cualquier extremo
            return origin.equals(from) || origin.equals(to);
        } else {
            // Un sentido: solo puede venir del origen definido
            return origin.equals(from);
        }
    }

    // Como venimos desde la posicion de 'origin', nos preguntamos a que interseccion debemos de llegar?
    public Position getDestination(Position origin) {
        if (origin.equals(from)) return to;
        if (origin.equals(to) && direction == Direction.TWO_WAY) return from;
        // Si se no se llegara a cumplir ninguna condicion puede que sea un error de logica
        // no se deberia de llamar esto si canTraverse devolvio false
        // entonces por eso lanzamos un error
        throw new IllegalArgumentException(
                "No se puede traversar desde " + origin + " en la calle " + name
        );
    }

    public String getName() {
        return name;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        String arrow = direction == Direction.TWO_WAY ? "<->" : "->";
        return name + " [" + from + " " + arrow + " " + to + "]";
    }
}
