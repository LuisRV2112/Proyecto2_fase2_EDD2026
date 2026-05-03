package edu.usac.edd.model;

/**
 * Entidad principal del sistema.
 * Inmutable excepto por status y stock (cambian durante tránsito).
 */
public class Product {
    private final String name;
    private final String barcode;       // clave única global
    private final String category;
    private final String expiryDate;    // ISO "YYYY-MM-DD"
    private final String brand;
    private double price;
    private int    stock;
    private String status;              // "Disponible","En tránsito","Agotado"
    private String branchId;            // sucursal actual

    public Product(String name, String barcode, String category,
                   String expiryDate, String brand, double price, int stock) {
        this.name       = name;
        this.barcode    = barcode;
        this.category   = category;
        this.expiryDate = expiryDate;
        this.brand      = brand;
        this.price      = price;
        this.stock      = stock;
        this.status     = "Disponible";
        this.branchId   = "";
    }

    /** Copia profunda — usada en rollback y transferencias */
    public Product copy() {
        Product p = new Product(name, barcode, category, expiryDate,
                                brand, price, stock);
        p.status   = this.status;
        p.branchId = this.branchId;
        return p;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getName()       { return name; }
    public String getBarcode()    { return barcode; }
    public String getCategory()   { return category; }
    public String getExpiryDate() { return expiryDate; }
    public String getBrand()      { return brand; }
    public double getPrice()      { return price; }
    public int    getStock()      { return stock; }
    public String getStatus()     { return status; }
    public String getBranchId()   { return branchId; }

    // ── Setters ────────────────────────────────────────────────────────────
    public void setPrice(double price)    { this.price  = price; }
    public void setStock(int stock)       { this.stock  = stock; }
    public void setStatus(String status)  { this.status = status; }
    public void setBranchId(String id)    { this.branchId = id; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Cat: %s | Exp: %s | Q%.2f | Stock:%d | %s",
                barcode, name, category, expiryDate, price, stock, status);
    }
}
