package com.trafficsim;

import com.trafficsim.model.City;
import com.trafficsim.model.Intersection;
import com.trafficsim.model.Position;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        City city = new City();
        city.assignSemaphores(city.getDefaultSemaphorePositions());

        Position origin = new Position(0, 0);
        List<Position> neighbors = city.getReachableNeighbors(origin);
        System.out.println("Vecinos de (0,0): " + neighbors);

        Intersection inter = city.getIntersection(new Position(2, 2));
        System.out.println(inter); // Intersection(2, 2)[RED] — tiene semáforo
    }
}
