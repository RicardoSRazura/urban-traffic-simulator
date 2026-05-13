#### ¿Que es el modelo en este proyecto?
El modelo en este caso es la representacion del mundo real en codigo, la ciudad y sus calles, sus intersecciones y las coordenadas. Es la base sobre la que todo lo demas ( hilos, semaforos, vehiculos) va a operar.

La clase **Position.java** reperesenta una coordenada en el mapa. 
*Position* es simplemente una coordenada, es un par de numeros que identifican un punto en la cuadriculo planteada en el proyecto. Este es como la manera mas comun en la que se comunican las demas clases para hablar de una ubicacion. No tiene mucha logica compleja, pero es bastante fundamental porque aparece en absolutamente todo: la clase **City** la usa para indexar intersecciones, los vehiculos la usan para su ruta, el Algoritmo de A* la usa para los nodos del grafo.
