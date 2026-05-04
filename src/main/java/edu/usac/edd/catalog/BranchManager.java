package edu.usac.edd.catalog;

import edu.usac.edd.graph.BranchGraph;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;
import edu.usac.edd.model.Transfer;
import edu.usac.edd.simulation.Dispatcher;
import edu.usac.edd.structures.HashTable;
import edu.usac.edd.structures.LinkedList;
import edu.usac.edd.structures.Queue;
import java.util.function.Consumer;

public class BranchManager {

    // Tabla hash para catálogos
    private static final int CATALOG_CAP = 64;
    private final CatalogMap  catalogs   = new CatalogMap(CATALOG_CAP);
    private final BranchGraph graph      = new BranchGraph();
    private final Dispatcher  dispatcher = new Dispatcher(this);

    private final TransferList transfers  = new TransferList();

    // maphash interno para Catalog
    private static class CatalogMap {
        private static class Entry {
            String  key;
            Catalog value;
            Entry   next;
            Entry(String k, Catalog v) { key = k; value = v; }
        }
        private final Entry[] table;
        private final int     cap;
        private int           count;

        CatalogMap(int cap) { this.cap = cap; table = new Entry[cap]; }

        private int hash(String k) {
            int h = 0;
            for (char c : k.toCharArray()) h = h * 31 + c;
            return Math.abs(h) % cap;
        }

        void put(String key, Catalog val) {
            int i = hash(key);
            for (Entry e = table[i]; e != null; e = e.next)
                if (e.key.equals(key)) { e.value = val; return; }
            Entry e = new Entry(key, val);
            e.next = table[i]; table[i] = e; count++;
        }

        Catalog get(String key) {
            for (Entry e = table[hash(key)]; e != null; e = e.next)
                if (e.key.equals(key)) return e.value;
            return null;
        }

        void remove(String key) {
            int i = hash(key);
            Entry prev = null, cur = table[i];
            while (cur != null) {
                if (cur.key.equals(key)) {
                    if (prev != null) prev.next = cur.next;
                    else             table[i]   = cur.next;
                    count--; return;
                }
                prev = cur; cur = cur.next;
            }
        }

        boolean containsKey(String key) { return get(key) != null; }
        int size() { return count; }

        void forEachValue(Consumer<Catalog> action) {
            for (Entry head : table)
                for (Entry e = head; e != null; e = e.next)
                    action.accept(e.value);
        }

        void forEach(java.util.function.BiConsumer<String, Catalog> action) {
            for (Entry head : table)
                for (Entry e = head; e != null; e = e.next)
                    action.accept(e.key, e.value);
        }
    }

    // Lista Enlzada transfer
    private static class TransferList {
        private static class Node {
            Transfer data; Node next;
            Node(Transfer t) { data = t; }
        }
        private Node head; private int size;

        void add(Transfer t) {
            Node n = new Node(t); n.next = head; head = n; size++;
        }
        int size() { return size; }
        void forEach(Consumer<Transfer> action) {
            for (Node c = head; c != null; c = c.next) action.accept(c.data);
        }
    }

    // Sucurlsales
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

    public Branch              getBranch(String id)   { return graph.getBranch(id); }
    public java.util.Collection<Branch> getAllBranches(){ return graph.getBranches(); }
    public BranchGraph         getGraph()              { return graph; }

    // Gestión de productos
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

    public Catalog getCatalog(String branchId)  { return catalogs.get(branchId); }

    public void forEachCatalog(java.util.function.BiConsumer<String, Catalog> action) {
        catalogs.forEach(action);
    }

    public Transfer initiateTransfer(String barcode, String fromBranchId,
                                     String toBranchId, Transfer.Criterion criterion) {
        Catalog src = catalogs.get(fromBranchId);
        if (src == null) return null;
        Product p = src.searchByBarcode(barcode);
        if (p == null) return null;

        java.util.List<String> route = graph.dijkstra(fromBranchId, toBranchId, criterion);
        if (route.isEmpty()) return null;

        double[] costs = graph.routeCost(route);
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
        dispatcher.dispatch(transfer);
        return transfer;
    }


    public void forEachTransfer(Consumer<Transfer> action) {
        transfers.forEach(action);
    }

    public int transferCount() { return transfers.size(); }

    public Dispatcher getDispatcher() { return dispatcher; }

    public boolean undoLastOperation(String branchId) {
        Catalog cat = catalogs.get(branchId);
        return cat != null && cat.undo();
    }

    public int totalProducts() {
        int[] total = {0};
        catalogs.forEachValue(cat -> total[0] += cat.size());
        return total[0];
    }

    public java.util.List<Transfer> getTransfers() {
        java.util.List<Transfer> list = new java.util.ArrayList<>();
        transfers.forEach(list::add);
        return list;
    }
}