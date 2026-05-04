package edu.usac.edd.simulation;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;
import edu.usac.edd.model.Transfer;
import edu.usac.edd.structures.Queue;
import java.util.function.Consumer;

public class Dispatcher {

    public static class BranchQueues {
        public final Queue  ingreso  = new Queue("Ingreso");
        public final Queue  traspaso = new Queue("Traspaso");
        public final Queue  salida   = new Queue("Salida");
        public final String branchId;
        BranchQueues(String id) { this.branchId = id; }
    }

    private static class QueueMap {
        private static class Entry {
            String       key;
            BranchQueues value;
            Entry        next;
            Entry(String k, BranchQueues v) { key = k; value = v; }
        }
        private static final int CAP = 64;
        private final Entry[] table = new Entry[CAP];

        private int hash(String k) {
            int h = 0;
            for (char c : k.toCharArray()) h = h * 31 + c;
            return Math.abs(h) % CAP;
        }

        synchronized BranchQueues getOrCreate(String key) {
            int i = hash(key);
            for (Entry e = table[i]; e != null; e = e.next)
                if (e.key.equals(key)) return e.value;
            BranchQueues bq = new BranchQueues(key);
            Entry e = new Entry(key, bq);
            e.next = table[i]; table[i] = e;
            return bq;
        }

        synchronized BranchQueues get(String key) {
            for (Entry e = table[hash(key)]; e != null; e = e.next)
                if (e.key.equals(key)) return e.value;
            return null;
        }

        synchronized void forEach(java.util.function.BiConsumer<String, BranchQueues> action) {
            for (Entry head : table)
                for (Entry e = head; e != null; e = e.next)
                    action.accept(e.key, e.value);
        }
    }

    private static class EventList {
        private static class Node {
            SimulationEvent data; Node next;
            Node(SimulationEvent e) { data = e; }
        }
        private Node head, tail;
        private int  size;

        synchronized void add(SimulationEvent e) {
            Node n = new Node(e);
            if (tail != null) tail.next = n;
            tail = n;
            if (head == null) head = n;
            size++;
        }

        synchronized void forEach(Consumer<SimulationEvent> action) {
            for (Node n = head; n != null; n = n.next) action.accept(n.data);
        }
        synchronized int size() { return size; }
    }

    private final BranchManager manager;
    private final QueueMap       queues   = new QueueMap();
    private final EventList      log      = new EventList();
    private Runnable             onUpdate;

    public Dispatcher(BranchManager manager) { this.manager = manager; }

    public void setOnUpdate(Runnable r) { this.onUpdate = r; }

    public BranchQueues getQueues(String branchId) {
        return queues.getOrCreate(branchId);
    }

    public void dispatch(Transfer transfer) {
        if (transfer.getRoute() == null || transfer.getRoute().isEmpty()) return;

        Thread t = new Thread(() -> {
            java.util.List<String> route = transfer.getRoute();
            Product p = transfer.getProduct();

            for (int hop = 0; hop < route.size(); hop++) {
                String branchId = route.get(hop);
                Branch branch   = manager.getBranch(branchId);
                if (branch == null) continue;

                BranchQueues bq          = getQueues(branchId);
                boolean      isDestination = (hop == route.size() - 1);

                bq.ingreso.enqueue(p);
                logEvent(branchId, p.getBarcode(), "Ingresó a cola de INGRESO");
                notifyGUI();
                sleep(branch.getTimeIngreso() * 1000L);
                bq.ingreso.dequeue();
                notifyGUI();

                if (isDestination) {
                    p.setStatus("Disponible");
                    p.setBranchId(branchId);
                    manager.addProduct(branchId, p.copy());
                    transfer.setPhase(Transfer.Phase.DELIVERED);
                    logEvent(branchId, p.getBarcode(), "✅ Entregado en destino");
                    notifyGUI();
                    break;
                }

                bq.traspaso.enqueue(p);
                logEvent(branchId, p.getBarcode(), "En cola de TRASPASO");
                notifyGUI();
                sleep(branch.getTimeTraspaso() * 1000L);
                bq.traspaso.dequeue();

                bq.salida.enqueue(p);
                logEvent(branchId, p.getBarcode(), "En cola de SALIDA");
                notifyGUI();
                sleep(branch.getTimeDespacho() * 1000L);
                bq.salida.dequeue();
                notifyGUI();
            }
        }, "Dispatcher-" + transfer.getProduct().getBarcode());
        t.setDaemon(true);
        t.start();
    }

    private void sleep(long ms) {
        long capped = Math.min(ms, 3000);
        try { Thread.sleep(Math.max(500, capped)); }
        catch (InterruptedException ignored) {}
    }

    private void logEvent(String branchId, String barcode, String msg) {
        log.add(new SimulationEvent(branchId, barcode, msg, System.currentTimeMillis()));
    }

    private void notifyGUI() {
        if (onUpdate != null)
            try { onUpdate.run(); } catch (Exception ignored) {}
    }

    public java.util.List<SimulationEvent> getLog() {
        java.util.List<SimulationEvent> copy = new java.util.ArrayList<>();
        log.forEach(copy::add);
        return copy;
    }


    public void forEachQueue(java.util.function.BiConsumer<String, BranchQueues> action) {
        queues.forEach(action);
    }

    public java.util.Map<String, BranchQueues> getAllQueues() {
        java.util.Map<String, BranchQueues> map = new java.util.LinkedHashMap<>();
        queues.forEach(map::put);
        return map;
    }

    public static class SimulationEvent {
        public final String branchId, barcode, message;
        public final long   timestamp;
        SimulationEvent(String b, String bc, String m, long ts) {
            branchId = b; barcode = bc; message = m; timestamp = ts;
        }
        @Override public String toString() {
            return String.format("[%s] %s → %s", branchId, barcode, message);
        }
    }
}