# Stream en java 

Dominar los __Streams__ en Java es dar el salto de una programacion imperativa (decirle al equipo como hacer las cosas paso a paso) a una programacion declarativa (decirle al equipo que quieres obtener).

# Ruta de aprendizaje:
  -  1. La anatomia de un Stream: Entender que un Stream no es una estructura de datos (como un ArrayList), sino un conducto de datos con tres partes: Fuente, Operaciones Intermedias y Operacion Terminal.
  -  2. Operaciones de Transformacion y Filtrado: Aprender a usar filter, map y sorted para molderar los datos a tu antojo.
  -  3. El "Gran Final" (Colectores y Reduccion): Como convertir ese flujo de datos nuevamente en una lista, un mapa o un solo valor (como un pormedio o una suma) usando collect y reduce.

1. __La anatomia de un Stream__
    Un Stream funciona como una cinta transportadora en una fabrica. Los datos pasan por ella, se transforman, pero la cinta no "guarda" los productos; solo los mueve. 
    
    Para empezar, necesitamos una fuente. Imagina que tenemos esta lista:  

             List<String> lenguajes = Arrays.asList("Java", "Python", "C++", "JavaScript");
    Para convertirla en un Stream, simplemente llamamos al metodo .stream(). Apartir de ahi, podemos encadenar operaciones. Lo mas importante es que las operaciones intermedias son "perezosas" (lazy): no se ejecutan hasta que llamas a una operacion terminal.

    ¿Por qué crees que es una ventaja técnica que las operaciones sean "perezosas" y no se ejecuten hasta el puro final?
   En Java, esto es fundamental para el rendimiento. Imagina que tienes una lista de un millón de registros y aplicas diez filtros. Si los Streams fueran "ansiosos", crearían diez listas intermedias de un millón de elementos cada una. Gracias a la naturaleza perezosa, el Stream solo procesa los elementos estrictamente necesarios cuando se lo pides al final.

# Conceptos de collect y reduce
1. __collect (El Recolector)__ 🧺.\
   collect es una operación de mutación. Su trabajo es tomar los elementos que vienen por la "cinta transportadora" y meterlos en un contenedor, como una List, un Set o un Map.
   - __Uso principal__: Cuando quieres transformar tu flujo de datos de vuelta en una colección de Java.
   - __Ejemplo:__ "Tengo un flujo de nombres y quiero guardarlos en una ArrayList".
2. __reduce (El Reductor)__ 🔄.\
   reduce es una operación de agregación. No mete cosas en una bolsa; las combina entre sí para producir un solo valor final que no es una colección.
   - __Uso principal0:__ Cuando buscas un resultado único como una suma, un producto, el valor máximo o una cadena concatenada.
   - __Ejemplo:__ "Tengo un flujo de números y quiero obtener la suma total de todos ellos".
3. Diferencia clave en el Backend ⚙️.\
    Desde la perpectiva de un desarrollador backend, la diferencua tecnica es que collect esta dise;ado para trabajar eficientemente con objetos que cambian (contenedores mutables), mientras que reduce es ideal para valores inmutables (numeros, strings) donde cada paso de la combinacion genera un valor nuevo.\

## Fase 1: Filtrado y Transformación
Imagina que tienes una lista de nombres y solo quieres los que empiezan con "J", pero ademas solo quieres todos en mayusculas.

En la programacion antigua, usarias un bucle __*for*__ y un __*if*__. Con Streams, encadenamos funciones:

        List<String> nombres = Arrays.asList("Juan", "Maria", "Jose", "Ana");
                    
        List<String> resultado = nombres.stream()
            .filter(n -> n.startsWith("J"))  // Paso A: Filtrar
            .map(String::toUpperCase)        // Paso B: Transformar
            .collect(Collectors.toList());   // Paso C: Cerrar

## Fase 2: El "Gran Final" (Operaciones Terminales)
Una vez que hemos transformado los datos en el Stream, estos siguen siendo un flujo etereo.\
Para "materializarlo" en algo util (una lista, un numero, un booleano), necesutamos una __operacion terminal__. Una vez ejecutada, el Sream se cierra y no puede volver a usarse.\

Aqui tienes las mas comunes:
1. collect(Collectors.toList()): Guarda los resultados en una nueva lista. 📋
2. reduce(): Combina todos los elementos en uno solo (útil para sumas o concatenaciones complejas). 🔄
3. forEach(): Realiza una acción para cada elemento (como imprimir en consola), pero no devuelve nada. 📢

## Fase 3: Optimizacion con Streams paralelos
En el desarrollo backend, a veces manejas millones de registros. Java ofrece .parallelStream() para dividir el trabajo entre todos los nuvleos de tu procesador automaticamente.

Sin embargo, hay hay algo llamado "__overhead__" (coste de gestion). Dividir la tarea, asignar hilos y luego volver a juntar los resultados consume tiempo y memoria.

Si para lavar dos platos llamas a cinco personas, tardarás más tiempo organizándolas y repartiendo los platos que si los lavas tú solo. En Java, el overhead (la gestión de hilos y la unión de resultados) puede hacer que un Stream paralelo sea mucho más lento que uno secuencial si la carga de trabajo no es masiva.

## Resumen de la Clase: Tu "Cheat Sheet" de Streams
Para que domines esto en tu camino como desarrollador backend, aquí tienes la estructura final que hemos construido:

| Fase                |         Operación             |                                      Función Principal |    Tipo    | 
|:--------------------|:-----------------------------:|-------------------------------------------------------:|-----------:|
| 1. Origen           |           .stream()           |          Convierte una colección en un flujo de datos. |    Inicial |
| 2. Filtro           |       .filter(p -> ...)       |       Descarta elementos que no cumplen una condición. | Intermedia |
| 3. Mapa             |        .map(e -> ...)         | Transforma un elemento en otro (ej. String a Integer). | Intermedia |              
| 4. Reducción        |   .reduce(0, (a, b) -> a+b)   |          Combina todos los elementos en un solo valor. |   Terminal |
| 5. Colector         | .collect(Collectors.toList()) |         Guarda los resultados en una nueva lista/mapa. |   Terminal |
 En versiones de Java en adelante no se necesita .collect(Collectors.toList()) se puede usar .toList() directamente, aunque genera una lista inmutable, si la quisieras mutable se usaria .collect(Collectors.toList()).

# El "Boss Final": Un ejemplo del mundo real\
Imagina que estás en el backend de una app de eCommerce y necesitas:
1. Obtener todos los pedidos (Order).
2. Filtrar solo los que superan los $100.
3. Obtener el correo del cliente de esos pedidos.
4. Guardarlos en una lista para enviarles un cupón de descuento.

          List<String> correosVIP = pedidos.stream()
              .filter(pedido -> pedido.getMonto() > 100) // Filtramos
              .map(pedido -> pedido.getClienteEmail())  // Transformamos (de Pedido a String)
              .collect(Collectors.toList());             // Recolectamos
Ademas de esos operadores, tambien estan los siguientees:
1. __reduce__: para colapsar todos los elementos en un solo valor(suma, producto, concatenacion).
2. __flatMap__: para cuando cada elemento contiene una coleeccion adentro y necesitas "aplanarla" en un solo stream.
3. __groupinBy__: para agrupar elementos en un Map segun algun criterio. Es el mas potente y el que mas se usa en backend real.