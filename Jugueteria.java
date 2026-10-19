/*
 * ACT formativa Norma Uribe Octubre 2026
 * Gestión de juguetes y juegos de mesa.
 *
 * Requisito: JDK 17. Dependencia incluida: lib/h2-2.3.232.jar.
 * Ejecutar desde esta carpeta:
 *   java -Dfile.encoding=UTF-8 -cp "lib/*" Jugueteria.java
 * Pruebas:
 *   java -Dfile.encoding=UTF-8 -cp "lib/*" Jugueteria.java --pruebas
 *
 * Este archivo reúne la clase principal, el modelo Producto,
 * la clase de acceso a datos ProductoDAO y las pruebas.
 * Los comentarios explican las reglas y las operaciones importantes.
 */
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.sql.*;
import java.nio.file.*;

/** Punto de entrada: captura datos, instancia objetos y ejecuta el menú. */
public class Jugueteria {
    private static final Scanner entrada = new Scanner(System.in);
    /** Solicita una línea completa, evitando mezclar nextInt con nextLine. */
    private static String leer(String mensaje) { System.out.print(mensaje); return entrada.nextLine().trim(); }
    private static int entero(String mensaje) { return Integer.parseInt(leer(mensaje)); }
    private static BigDecimal dinero() { return new BigDecimal(leer("Precio MXN (ej. 249.90): ")); }
    /** Carga seis productos de ejemplo únicamente cuando la tabla está vacía. */
    public static void sembrar(ProductoDAO dao) throws SQLException {
        if(!dao.listar().isEmpty()) return; // Conserva los datos existentes y evita cargar ejemplos otra vez.
        dao.insertar(new Producto("J001","Bloques de construcción",Producto.Categoria.JUGUETE,new BigDecimal("349.90"),12,3));
        dao.insertar(new Producto("J002","Peluche de ajolote",Producto.Categoria.JUGUETE,new BigDecimal("199.00"),20,3));
        dao.insertar(new Producto("J003","Auto de juguete",Producto.Categoria.JUGUETE,new BigDecimal("149.50"),15,4));
        dao.insertar(new Producto("M001","Ajedrez",Producto.Categoria.JUEGO_MESA,new BigDecimal("299.00"),8,6));
        dao.insertar(new Producto("M002","Dominó",Producto.Categoria.JUEGO_MESA,new BigDecimal("179.00"),10,6));
        dao.insertar(new Producto("M003","Lotería mexicana",Producto.Categoria.JUEGO_MESA,new BigDecimal("99.00"),18,5));
    }
    /** Ejecuta un menú de consola y conserva los cambios entre sesiones. */
    public static void main(String[] args) throws Exception {
        // La opción --pruebas verifica el sistema sin modificar el inventario principal.
        if (args.length == 1 && args[0].equals("--pruebas")) {
            Pruebas.main(new String[0]);
            return;
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("data"));
        try(ProductoDAO dao=new ProductoDAO("jdbc:h2:./data/jugueteria")) {
            sembrar(dao);
            while(true) {
                System.out.println("\nJUGUETERÍA: 1 Listar | 2 Buscar | 3 Registrar | 4 Precio | 5 Vender | 6 Reponer | 7 Eliminar | 8 Valor total | 0 Salir");
                if(!entrada.hasNextLine()) break;
                try {
                    int opcion=entero("Opción: ");
                    if(opcion==0) break; // Termina el ciclo y permite cerrar la conexión.
                    if(opcion==1) { dao.listar().forEach(System.out::println); continue; }
                    if(opcion==8) {
                        BigDecimal total=dao.listar().stream().map(Producto::valorInventario).reduce(BigDecimal.ZERO,BigDecimal::add);
                        System.out.println("Valor total: $"+total+" MXN"); continue;
                    }
                    if(opcion<2 || opcion>7) { System.out.println("Opción inexistente."); continue; }
                    String codigo=leer("Código: ").toUpperCase(Locale.ROOT);
                    if(opcion==3) {
                        String nombre=leer("Nombre: ");
                        int tipo=entero("Categoría (1 juguete, 2 juego de mesa): ");
                        if(tipo!=1 && tipo!=2) throw new IllegalArgumentException("Categoría inválida.");
                        Producto p=new Producto(codigo,nombre,tipo==1?Producto.Categoria.JUGUETE:Producto.Categoria.JUEGO_MESA,
                            dinero(),entero("Stock: "),entero("Edad mínima: "));
                        dao.insertar(p); System.out.println("Producto registrado."); continue;
                    }
                    Producto p=dao.buscar(codigo).orElseThrow(()->new IllegalArgumentException("Producto no encontrado.")); // Recupera el objeto o informa que no existe.
                    switch(opcion) {
                        case 2 -> System.out.println(p);
                        case 4 -> { p.cambiarPrecio(dinero()); dao.actualizar(p); System.out.println("Precio actualizado."); }
                        case 5 -> { BigDecimal importe=p.vender(entero("Cantidad: ")); dao.actualizar(p); System.out.println("Venta: $"+importe+" MXN"); }
                        case 6 -> { p.reponer(entero("Cantidad: ")); dao.actualizar(p); System.out.println("Stock actualizado."); }
                        case 7 -> { if(leer("Escriba SI para eliminar: ").equals("SI")) { dao.eliminar(codigo); System.out.println("Producto eliminado."); } }
                        default -> System.out.println("Opción inexistente.");
                    }
                } catch(IllegalArgumentException | ArithmeticException e) { System.out.println("Dato inválido: "+e.getMessage()); }
                  catch(SQLException e) { System.out.println("Error de base de datos. Código SQL: "+e.getSQLState()); }
            }
        }
    }
}

/** Representa un juguete o juego de mesa y protege sus reglas de negocio. */
class Producto {
    public enum Categoria { JUGUETE, JUEGO_MESA }
    private final String codigo; // Identificador único que no cambia después de construir el objeto.
    private final String nombre;
    private final Categoria categoria;
    private final int edadMinima;
    private BigDecimal precio; // BigDecimal representa importes decimales en pesos mexicanos.
    private int stock; // Existencias disponibles de esta instancia.
    /** Crea un producto válido. El precio se maneja en pesos mexicanos. */
    public Producto(String codigo, String nombre, Categoria categoria,
                    BigDecimal precio, int stock, int edadMinima) {
        if (codigo == null || !codigo.matches("[A-Z0-9-]{1,20}"))
            throw new IllegalArgumentException("Código: use 1 a 20 letras mayúsculas, números o guiones.");
        if (nombre == null || nombre.isBlank() || nombre.length() > 100)
            throw new IllegalArgumentException("Nombre obligatorio, máximo 100 caracteres.");
        if (categoria == null || stock < 0 || edadMinima < 0 || edadMinima > 99)
            throw new IllegalArgumentException("Categoría, stock o edad inválidos.");
        this.codigo = codigo; this.nombre = nombre.trim(); this.categoria = categoria;
        this.stock = stock; this.edadMinima = edadMinima;
        cambiarPrecio(precio);
    }
    /** Cambia el precio sin permitir valores negativos o fracciones de centavo. */
    public void cambiarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0)
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        try { this.precio = precio.setScale(2, java.math.RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Use máximo dos decimales."); }
        if (this.precio.precision() > 10) throw new IllegalArgumentException("Precio demasiado grande.");
    }
    /** Disminuye existencias y devuelve el importe de la venta. */
    public BigDecimal vender(int cantidad) {
        if (cantidad <= 0 || cantidad > stock)
            throw new IllegalArgumentException("Cantidad inválida o stock insuficiente.");
        stock -= cantidad; // Descuenta unidades únicamente después de validar la venta.
        return precio.multiply(BigDecimal.valueOf(cantidad)); // Devuelve precio unitario por unidades vendidas.
    }
    /** Aumenta existencias y comprueba el límite de un entero. */
    public void reponer(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad mayor que cero requerida.");
        stock = Math.addExact(stock, cantidad); // Suma y detecta un posible desbordamiento de int.
    }
    /** Calcula el valor del inventario de este producto. */
    public BigDecimal valorInventario() { return precio.multiply(BigDecimal.valueOf(stock)); }
    /** Consulta el atributo codigo sin modificarlo. */
    /** Consulta el atributo codigo sin modificarlo. */
    public String getCodigo() { return codigo; }
    /** Consulta el atributo nombre sin modificarlo. */
    /** Consulta el atributo nombre sin modificarlo. */
    public String getNombre() { return nombre; }
    /** Consulta el atributo categoria sin modificarlo. */
    /** Consulta el atributo categoria sin modificarlo. */
    public Categoria getCategoria() { return categoria; }
    /** Consulta el atributo precio sin modificarlo. */
    /** Consulta el atributo precio sin modificarlo. */
    public BigDecimal getPrecio() { return precio; }
    /** Consulta el atributo stock sin modificarlo. */
    /** Consulta el atributo stock sin modificarlo. */
    public int getStock() { return stock; }
    /** Consulta el atributo edadminima sin modificarlo. */
    /** Consulta el atributo edadminima sin modificarlo. */
    public int getEdadMinima() { return edadMinima; }
    /** Devuelve una fila legible para el listado de consola. */
    @Override public String toString() {
        return codigo + " | " + nombre + " | " + categoria + " | $" + precio
            + " MXN | stock " + stock + " | edad " + edadMinima + "+";
    }
}

/** Guarda y recupera productos en una base H2 mediante JDBC y consultas parametrizadas. */
class ProductoDAO implements AutoCloseable {
    private final Connection conexion;
    /** Abre la base de datos y crea la tabla si todavía no existe. */
    public ProductoDAO(String url) throws SQLException {
        conexion = DriverManager.getConnection(url, "sa", ""); // Abre H2 con la cuenta local de demostración.
        try (Statement s = conexion.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS productos (codigo VARCHAR(20) PRIMARY KEY, "
                + "nombre VARCHAR(100) NOT NULL, categoria VARCHAR(20) NOT NULL "
                + "CHECK(categoria IN ('JUGUETE','JUEGO_MESA')), precio DECIMAL(10,2) NOT NULL CHECK(precio>=0), "
                + "stock INT NOT NULL CHECK(stock>=0), edad_minima INT NOT NULL CHECK(edad_minima BETWEEN 0 AND 99))");
        }
    }
    /** Inserta un objeto. La clave primaria impide códigos duplicados. */
    public void insertar(Producto p) throws SQLException {
        try (PreparedStatement s = conexion.prepareStatement("INSERT INTO productos VALUES (?,?,?,?,?,?)")) {
            s.setString(1,p.getCodigo()); s.setString(2,p.getNombre()); s.setString(3,p.getCategoria().name());
            s.setBigDecimal(4,p.getPrecio()); s.setInt(5,p.getStock()); s.setInt(6,p.getEdadMinima());
            s.executeUpdate(); // Ejecuta la inserción parametrizada en la base de datos.
        }
    }
    /** Convierte una fila SQL en una nueva instancia de Producto. */
    private Producto convertir(ResultSet r) throws SQLException {
        return new Producto(r.getString("codigo"),r.getString("nombre"),
            Producto.Categoria.valueOf(r.getString("categoria")),r.getBigDecimal("precio"),
            r.getInt("stock"),r.getInt("edad_minima"));
    }
    /** Busca por clave primaria y devuelve un Optional vacío si no existe. */
    public Optional<Producto> buscar(String codigo) throws SQLException {
        try (PreparedStatement s = conexion.prepareStatement("SELECT * FROM productos WHERE codigo=?")) {
            s.setString(1,codigo); // Sustituye el primer signo ? por el código solicitado.
            try (ResultSet r=s.executeQuery()) { return r.next()?Optional.of(convertir(r)):Optional.empty(); }
        }
    }
    /** Lista los productos ordenados por código. */
    public List<Producto> listar() throws SQLException {
        List<Producto> salida=new ArrayList<>();
        try (Statement s=conexion.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM productos ORDER BY codigo")) {
            while(r.next()) salida.add(convertir(r)); // Cada fila se transforma en un objeto Producto.
        }
        return salida;
    }
    /** Persiste precio y stock modificados por los métodos del objeto. */
    public void actualizar(Producto p) throws SQLException {
        try (PreparedStatement s=conexion.prepareStatement("UPDATE productos SET precio=?,stock=? WHERE codigo=?")) {
            s.setBigDecimal(1,p.getPrecio()); s.setInt(2,p.getStock()); s.setString(3,p.getCodigo());
            if(s.executeUpdate()!=1) throw new SQLException("Producto inexistente.");
        }
    }
    /** Elimina por código y devuelve si encontró el producto. */
    public boolean eliminar(String codigo) throws SQLException {
        try (PreparedStatement s=conexion.prepareStatement("DELETE FROM productos WHERE codigo=?")) {
            s.setString(1,codigo); // Sustituye el primer signo ? por el código solicitado.
            return s.executeUpdate()==1;
        }
    }
    /** Cierra la conexión al terminar el bloque try-with-resources. */
    @Override public void close() throws SQLException { conexion.close(); }
}

/** Pruebas reproducibles aisladas de la base de datos de demostración. */
class Pruebas {
    private static int total=0;
    private static void comprobar(boolean ok,String caso) {
        if(!ok) throw new AssertionError(caso);
        total++; System.out.println("OK "+total+": "+caso); // Cuenta e identifica cada verificación correcta.
    }
    private static void invalido(Runnable accion,String caso) {
        try { accion.run(); throw new AssertionError(caso); }
        catch(IllegalArgumentException e) { comprobar(true,caso); }
    }
    public static void main(String[] args) throws Exception {
        Path carpeta=Files.createTempDirectory("jugueteria-test-"); // Aísla las pruebas del inventario de demostración.
        String url="jdbc:h2:"+carpeta.resolve("prueba").toAbsolutePath();
        Producto p=new Producto("T001","Ajedrez",Producto.Categoria.JUEGO_MESA,new BigDecimal("100.00"),5,6);
        comprobar(p.vender(2).equals(new BigDecimal("200.00")) && p.getStock()==3,"Venta e importe");
        invalido(()->p.vender(4),"Venta sin stock rechazada");
        comprobar(p.getStock()==3,"Rechazo conserva stock");
        invalido(()->p.vender(0),"Venta de cero rechazada");
        invalido(()->p.reponer(-1),"Reposición negativa rechazada");
        invalido(()->p.cambiarPrecio(new BigDecimal("-1")),"Precio negativo rechazado");
        invalido(()->p.cambiarPrecio(new BigDecimal("1.001")),"Fracción de centavo rechazada");
        p.reponer(2); p.cambiarPrecio(new BigDecimal("120.00"));
        comprobar(p.valorInventario().equals(new BigDecimal("600.00")),"Reposición y valoración");
        try(ProductoDAO dao=new ProductoDAO(url)) {
            dao.insertar(p); comprobar(dao.listar().size()==1,"Alta y listado");
            try { dao.insertar(p); throw new AssertionError("Duplicado"); }
            catch(SQLException e) { comprobar("23505".equals(e.getSQLState()),"Código duplicado rechazado"); }
            Producto guardado=dao.buscar("T001").orElseThrow(); guardado.vender(1); dao.actualizar(guardado);
            comprobar(dao.buscar("T001").orElseThrow().getStock()==4,"Actualización persistida");
            comprobar(dao.buscar("NOEXISTE").isEmpty(),"Búsqueda inexistente");
        }
        try(ProductoDAO dao=new ProductoDAO(url)) {
            comprobar(dao.buscar("T001").orElseThrow().getStock()==4,"Persistencia al reabrir conexión");
            comprobar(dao.eliminar("T001") && dao.listar().isEmpty(),"Eliminación");
            Jugueteria.sembrar(dao); Jugueteria.sembrar(dao);
            comprobar(dao.listar().size()==6,"Carga inicial sin duplicados");
        }
        try(var rutas=Files.walk(carpeta)) { for(Path ruta:rutas.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(ruta); }
        System.out.println("RESULTADO: "+total+" pruebas correctas.");
    }
}
