package edu.usac.edd.model;

import java.util.List;

/**
 * Transferencia de producto entre sucursales.
 * Contiene la ruta completa y el ETA calculado.
 */
public class Transfer {
    public enum Criterion { TIME, COST }
    public enum Phase     { QUEUED, IN_TRANSIT, DELIVERED, FAILED }

    private final Product  product;
    private final String   originId;
    private final String   destId;
    private final Criterion criterion;
    private List<String>   route;       // IDs de sucursales en orden
    private double         totalCost;
    private double         totalTime;   // segundos estimados
    private Phase          phase;
    private int            currentHop;  // índice en route
    private long           startMs;     // timestamp inicio

    public Transfer(Product product, String originId, String destId,
                    Criterion criterion) {
        this.product   = product;
        this.originId  = originId;
        this.destId    = destId;
        this.criterion = criterion;
        this.phase     = Phase.QUEUED;
        this.currentHop = 0;
        this.startMs   = System.currentTimeMillis();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public Product    getProduct()    { return product; }
    public String     getOriginId()   { return originId; }
    public String     getDestId()     { return destId; }
    public Criterion  getCriterion()  { return criterion; }
    public List<String> getRoute()    { return route; }
    public double     getTotalCost()  { return totalCost; }
    public double     getTotalTime()  { return totalTime; }
    public Phase      getPhase()      { return phase; }
    public int        getCurrentHop() { return currentHop; }
    public long       getStartMs()    { return startMs; }

    public void setRoute(List<String> route)     { this.route = route; }
    public void setTotalCost(double c)           { this.totalCost = c; }
    public void setTotalTime(double t)           { this.totalTime = t; }
    public void setPhase(Phase phase)            { this.phase = phase; }
    public void advanceHop()                     { currentHop++; }

    /** Sucursal actual en la ruta */
    public String currentBranch() {
        if (route == null || currentHop >= route.size()) return destId;
        return route.get(currentHop);
    }

    /** ¿Ya llegó al destino? */
    public boolean arrived() {
        return phase == Phase.DELIVERED ||
               (route != null && currentHop >= route.size() - 1);
    }

    /** ETA formateado en minutos:segundos */
    public String etaFormatted() {
        long mins = (long)(totalTime / 60);
        long secs = (long)(totalTime % 60);
        return String.format("%dm %02ds", mins, secs);
    }

    @Override
    public String toString() {
        return String.format("Transfer[%s → %s, %s, %s, ETA:%s]",
                originId, destId, product.getBarcode(),
                criterion, etaFormatted());
    }
}
