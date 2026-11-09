# Actividad formativa 3. Herencia en Java

**Estudiante:** Norma Uribe · **Proyecto:** Gestión de juguetes y juegos de mesa

La aplicación amplía el inventario de la Unidad 2. Conserva el menú y el almacenamiento en H2. Ahora cada producto se representa como un juguete o un juego de mesa, según sus características.

| Clase | Atributos y comportamiento |
|---|---|
| `Producto` | Clase base abstracta. Centraliza código, nombre, categoría, precio, stock y edad mínima. Implementa `vender()`, `reponer()`, `cambiarPrecio()` y `valorInventario()`. |
| `Juguete extends Producto` | Agrega `material`. Su método exclusivo `fichaDeUso()` consulta la edad mínima heredada. |
| `JuegoDeMesa extends Producto` | Agrega `minJugadores` y `maxJugadores`. Su método exclusivo `admiteJugadores(int)` comprueba si un grupo cabe en ese rango. |

Ambas clases llaman a `super(...)` para inicializar los datos comunes. Sus versiones de `toString()` llaman a `super.toString()` y agregan la información del subtipo. El programa principal instancia ambos tipos al registrar productos o al cargar los ejemplos. La lista usa referencias de tipo `Producto`: Java ejecuta el `toString()` que corresponde a cada objeto. Este comportamiento se conoce como polimorfismo.

`ProductoDAO` usa la categoría para reconstruir el subtipo al consultar H2. Si se copia la base de la Unidad 2 a `data/jugueteria.mv.db` con el programa cerrado, la aplicación agrega las columnas que faltan y conserva los datos previos. Los atributos nuevos de los registros anteriores aparecen como «sin especificar».

## Ejecución con JDK 17 o posterior

1. Abre una terminal en esta carpeta. Comprueba la versión con `java -version`.
2. El ZIP de entrega ya incluye H2. Si descargaste el código desde GitHub, crea la carpeta `lib` y coloca ahí [h2-2.3.232.jar](https://repo.maven.apache.org/maven2/com/h2database/h2/2.3.232/h2-2.3.232.jar).
3. Ejecuta el programa y las pruebas con estos comandos:

```bash
java -Dfile.encoding=UTF-8 -cp "lib/*" Jugueteria.java
java -Dfile.encoding=UTF-8 -cp "lib/*" Jugueteria.java --pruebas
```

En IntelliJ IDEA, Eclipse o NetBeans, configura JDK 17 o posterior, agrega el JAR de H2 a las dependencias y ejecuta la clase `Jugueteria`. Usa esta carpeta como directorio de trabajo. La aplicación crea `data/jugueteria.mv.db` en la primera ejecución y carga seis ejemplos cuando la tabla está vacía. Las pruebas usan bases temporales.

## Guion para la sesión en línea (4 minutos)

1. **Objetivo (30 s).** «Extendí el inventario de la Unidad 2 para representar dos tipos de productos que comparten operaciones, pero tienen datos distintos».
2. **Código (1 min).** Muestra la clase `Producto` y los constructores de las subclases. Señala `extends`, `super(...)` y `@Override`. Explica cómo `fichaDeUso()` usa `getEdadMinima()` sin duplicar el atributo.
3. **Demostración (1 min y 30 s).** Con una base nueva, selecciona **1 Listar**. Los juguetes muestran su material y los juegos su rango de jugadores. Selecciona **5 Vender** → `M001` → `2`. La venta calcula $598.00 MXN y el stock baja de 8 a 6 con el método heredado. Selecciona **2 Buscar** → `M001` para verificarlo.
4. **Validación y cierre (1 min).** Corre `--pruebas`, muestra las 25 verificaciones y abre la rama de la actividad en GitHub. «La herencia me permitió reutilizar las reglas del inventario y agregar características propias sin repetir la lógica de ventas».

## Evidencia

`RESULTADO_PRUEBAS.txt` contiene la salida de las pruebas de inventario, herencia, polimorfismo, persistencia y migración. `DEMO_CONSOLA.txt` registra una ejecución del menú con una base nueva.

## Referencias

- Universidad Ciudadana de Nuevo León. (s. f.). [Herencia](https://licenciatura.ucnl.edu.mx/mod/page/view.php?id=810866). Campus virtual.
- Oracle. (s. f.). [Inheritance](https://docs.oracle.com/javase/tutorial/java/IandI/subclasses.html). *The Java Tutorials*.
