package com.trafficsim.threads;

import com.trafficsim.metrics.MetricsCollector;
import com.trafficsim.model.City;
import com.trafficsim.model.Intersection;
import com.trafficsim.model.Position;

import java.util.List;

public class VehicleThread extends Thread {

    // --- Estado del Vehiculo ---
    private final int vehicleId;            // Indentificar unico del vehiculo
    private final List<Position> route;     // Ruta calculada por A* - lista de posiciones
    private final City city;                // Referencia al mapa compartido
    private final MetricsCollector metrics; //Registra tiempos para el reporte final

    private volatile boolean paused = false;
    private final Runnable onArrivalCallback;

    //Posicion actual del vehiculo en el mapa
    private volatile Position currentPosition;

    //Estado del vehiculo para que la GUI pueda consultarlo
    public enum VehicleState { MOVING, WAITING_LIGHT, WAITING_LOCK, ARRIVED }
    private volatile VehicleState state = VehicleState.MOVING;

    // Cuanto tiempo "vive" en cada interseccion al cruzar
    private static final int MOVE_DELAY_MS = 300;

    // Cuanto tiempo espera antes de reintentar si no puede cruzar
    private static final int RETRY_LIGHT_MS = 100;
    private static final int RETRY_LOCK_MS = 50;

    public VehicleThread(int vehicleId, List<Position> route, City city, MetricsCollector metrics, Runnable onArrivalCallback) {
        this.vehicleId = vehicleId;
        this.route = route;
        this.city = city;
        this.metrics = metrics;
        this.currentPosition = route.get(0); // Empieza en el primer punto de la ruta
        this.onArrivalCallback = onArrivalCallback;

        setName("Vehiculo-" + vehicleId);
        setDaemon(true);
    }

    @Override
    public void run() {
        //Registramos el momento exacto de salida para calcular tiempo total de viaje
        long departureTime = System.currentTimeMillis();
        metrics.recordDeparture(vehicleId, departureTime);

        //Recorremos la ruta saltando el primer elemento
        // route.get(0) es el punto de partida, route.get(route.size()-1) es el destino
        for (int i = 1; i < route.size(); i++) {
            if (isInterrupted()) break; // Respetamos una posbible interrupcion externa
            moveToPosition(route.get(i));
        }

        // El vehiculo llego al destino
        long arrivalTime = System.currentTimeMillis();
        state = VehicleState.ARRIVED;
        metrics.recordArrival(vehicleId, arrivalTime);

        if (onArrivalCallback != null) {
            onArrivalCallback.run();
        }

        System.out.println("[" + getName() + "] llego a destino en " + (arrivalTime - departureTime) + " ms");
    }

    // Intentar moverse a la siguiente posiciond de la ruta
    // Esta es la logica central de la concurrencia en el sistema
    private void moveToPosition(Position nextPos) {
        Intersection target = city.getIntersection(nextPos);

        //Acumulamos el tiempo total de espera para las metricas
        long waitStart = 0;
        boolean waited = false;

        //Bucle de reintento: el vehiculo no avanza hasta que pueda cruzar
        while (!isInterrupted()) {

            // ---- Verificación de pausa (separada del bucle de reintento) ----
            while (paused && !isInterrupted()) {
                try { Thread.sleep(100); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (isInterrupted()) return;

            // Primer paso es revisar el semaforo
            // Si la interseccion tiene semaforo y no esta en verde, esperamos
            // volatile en lightState garantiza que leemos el valor mas reciente
            // escrito por TrafficLightThread - Sin necessidad de syncronized aqui
            if (!target.isPassable()) {
                state = VehicleState.WAITING_LIGHT;

                if (!waited) {
                    waitStart = System.currentTimeMillis();
                    waited = true;
                }
                target.incrementWaitCount(); // Estadistica de congestion del cruce

                try {
                    Thread.sleep(RETRY_LIGHT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue; //Vuelve al inicio del while y revisa de nuevo
            }

            // ---- El segundo paso es intentar adquirir el lock de la interseccion ----
            // tryLock() es no bloqueante: devuelve false inmediatamente si
            // otro vehiculo ya esta cruzando. Esto es mejor que lock()
            // porque evita que el hilo quede suspendido indefinidamente y
            // permite que el vehiculo siga revisando el semaforo mientras espera
            if (!target.tryEnter()) {
                state = VehicleState.WAITING_LOCK;

                if(!waited) {
                    waitStart = System.currentTimeMillis();
                    waited = true;
                }

                try {
                    Thread.sleep(RETRY_LOCK_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue; // Reintenta desde el paso 1
            }

            // --- El tercer paso es cruzar la interseccion ---
            // Llegamos aqui solo si: semaforo verde y lock adquirido
            // Y si somos el unico hilo dentro de esta interseccion ahora mismo
            try {
                state = VehicleState.MOVING;
                currentPosition = nextPos; // Actualizamos la posicion, esto lo lee la GUI

                //Registramos tiempo de espera si hubi
                if (waited) {
                    long waitEnd = System.currentTimeMillis();
                    metrics.addWaitTime(vehicleId, waitEnd - waitStart);
                }

                // Simulamos el tiempo de cruce (velocidad del vehiculo)
                Thread.sleep(MOVE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                // El paso cuatro es liberar el lock
                // Siempre en finally: incluso si hay alguna excepcion,
                // la interseccion queda libre. Sin esto, un crash dejaria
                // el loch tomado para siempre y todos los vehiculos se bloquearian
                target.exit();
            }

            // Cruce exitoso - Salimos del bucle de reintento
            break;
        }
    }

    // Metodo par pausar/reanudar desde el controlador
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    // ---- Getters para la GUI y el controlador ----

    public int getVehicleId() {
        return vehicleId;
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public VehicleState getVeichleState() {
        return state;
    }

    public List<Position> getRoute() {
        return route;
    }

    public Position getDestination() {
        return route.get(route.size() - 1);
    }

    public Position getOrigin() {
        return route.get(0);
    }

    // Cuanto avanzo en su ruta?, esto nos sirve para ver una barra de progreso en la GUI
    public double getProgressPercent() {
        if (route.size() <= 1) return 100.0;
        int currentIndex = route.indexOf(currentPosition);
        return (currentIndex / (double)(route.size() - 1)) * 100.0;
   }
}
