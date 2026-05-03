package edu.usac.edd.catalog;

import edu.usac.edd.graph.BranchGraph;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;
import edu.usac.edd.model.Transfer;
import edu.usac.edd.simulation.Dispatcher;
import edu.usac.edd.structures.Queue;

import java.util.*;

/**
 * Coordinador central.
 * Gestiona todas las sucursales, el grafo y las transferencias.
 */
public class BranchManager extends Catalog {

    private final BranchGraph         graph       = new BranchGraph();
    private final Map<String, Catalog> catalogs   = new LinkedHashMap<>();
    private final Dispatcher           dispatcher  = new Dispatcher(this);
    private final List<Transfer>       transfers   = new ArrayList<>();

    // Gestión de sucursales
    public void addBranch(Branch b) {
        graph.addBranch(b);
        catalogs.put(b.getId(), new Catalog(b.getId()));
    }

    public void removeBranch(String id) {
        graph.removeBranch(id);
        catalogs.remove(id);
    }

    public void addConnection(String fromId, String toId,
                              double time, double cost, boolean bidir) {
        graph.addEdge(fromId, toId, time, cost, bidir);
    }

    public Branch  getBranch(String id)        { return graph.getBranch(id); }
    public Collection<Branch> getAllBranches()  { return graph.getBranches(); }
    public BranchGraph getGraph()              { return graph; }

    // ── Gestión de productos ──────────────────────────────────────────────
    public boolean addProduct(String branchId, Product p) {
        Catalog cat = catalogs.get(branchId);
        if (cat == null) return false;
        p.setBranchId(branchId);
        return cat.addProduct(p);
    }

    public boolean removeProduct(String branchId, String barcode) {
        Catalog cat = catalogs.get(branchId);
        return cat != null && cat.removeProduct(barcode);
    }

    public Product searchByBarcode(String branchId, String barcode) {
        Catalog cat = catalogs.get(branchId);
        return cat != null ? cat.searchByBarcode(barcode) : null;
    }

    public Product searchByName(String branchId, String name) {
        Catalog cat = catalogs.get(branchId);
        return cat != null ? cat.searchByName(name) : null;
    }

    public Catalog getCatalog(String branchId) { return catalogs.get(branchId); }
    public Map<String, Catalog> getAllCatalogs(){ return catalogs; }

    // ── Transferencia entre sucursales ────────────────────────────────────
    public Transfer initiateTransfer(String barcode, String fromBranchId,
                                     String toBranchId,
                                     Transfer.Criterion criterion) {
        Catalog src = catalogs.get(fromBranchId);
        if (src == null) return null;

        Product p = src.searchByBarcode(barcode);
        if (p == null) return null;

        // Calcular ruta óptima con Dijkstra
        List<String> route = graph.dijkstra(fromBranchId, toBranchId, criterion);
        if (route.isEmpty()) return null;

        double[] costs = graph.routeCost(route);
        // Añadir tiempos de procesamiento de sucursales intermedias
        double processingTime = 0;
        for (int i = 1; i < route.size() - 1; i++) {
            Branch mid = graph.getBranch(route.get(i));
            if (mid != null) processingTime += mid.getTimeTraspaso() + mid.getTimeDespacho();
        }
        Branch dest = graph.getBranch(toBranchId);
        if (dest != null) processingTime += dest.getTimeIngreso();

        Transfer transfer = new Transfer(p, fromBranchId, toBranchId, criterion);
        transfer.setRoute(route);
        transfer.setTotalTime(costs[0] + processingTime);
        transfer.setTotalCost(costs[1]);
        transfer.setPhase(Transfer.Phase.IN_TRANSIT);

        p.setStatus("En tránsito");
        transfers.add(transfer);

        // Despachar en el simulador
        dispatcher.dispatch(transfer);

        return transfer;
    }

    public List<Transfer> getTransfers() { return transfers; }
    public Dispatcher     getDispatcher(){ return dispatcher; }

    // ── Undo global ───────────────────────────────────────────────────────
    public boolean undoLastOperation(String branchId) {
        Catalog cat = catalogs.get(branchId);
        return cat != null && cat.undo();
    }

    /** Busca un producto en TODAS las sucursales */
    public Map<String, Product> searchGlobal(String barcode) {
        Map<String, Product> found = new LinkedHashMap<>();
        for (Map.Entry<String, Catalog> e : catalogs.entrySet()) {
            Product p = e.getValue().searchByBarcode(barcode);
            if (p != null) found.put(e.getKey(), p);
        }
        return found;
    }

    /** Total de productos en todo el sistema */
    public int totalProducts() {
        return catalogs.values().stream().mapToInt(Catalog::size).sum();
    }
}
