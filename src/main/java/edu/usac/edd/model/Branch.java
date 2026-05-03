package edu.usac.edd.model;

/**
 * Sucursal del supermercado.
 * Cada sucursal tiene su propio catálogo (estructuras independientes)
 * y tres colas de procesamiento de productos.
 */
public class Branch {
    private final String id;
    private String name;
    private String location;
    private int    timeIngreso;    // segundos: procesar llegada
    private int    timeTraspaso;   // segundos: preparar para reenvío
    private int    timeDespacho;   // segundos: intervalo entre despachos

    // Coordenadas para dibujar en el canvas del grafo
    private double x;
    private double y;

    public Branch(String id, String name, String location,
                  int timeIngreso, int timeTraspaso, int timeDespacho) {
        this.id           = id;
        this.name         = name;
        this.location     = location;
        this.timeIngreso  = timeIngreso;
        this.timeTraspaso = timeTraspaso;
        this.timeDespacho = timeDespacho;
        this.x = 100;
        this.y = 100;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getLocation()    { return location; }
    public int    getTimeIngreso()  { return timeIngreso; }
    public int    getTimeTraspaso() { return timeTraspaso; }
    public int    getTimeDespacho() { return timeDespacho; }
    public double getX()           { return x; }
    public double getY()           { return y; }

    // ── Setters ────────────────────────────────────────────────────────────
    public void setName(String name)              { this.name = name; }
    public void setLocation(String location)      { this.location = location; }
    public void setTimeIngreso(int t)             { this.timeIngreso = t; }
    public void setTimeTraspaso(int t)            { this.timeTraspaso = t; }
    public void setTimeDespacho(int t)            { this.timeDespacho = t; }
    public void setPosition(double x, double y)   { this.x = x; this.y = y; }

    @Override
    public String toString() {
        return String.format("Sucursal[%s] %s @ %s (ing:%ds tra:%ds des:%ds)",
                id, name, location, timeIngreso, timeTraspaso, timeDespacho);
    }
}
