package edu.usac.edd.simulation;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;
import edu.usac.edd.model.Transfer;
import edu.usac.edd.structures.Queue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulador de despacho entre sucursales.
 * Cada sucursal tiene 3 colas: ingreso, traspaso, salida.
 * El flujo respeta los tiempos configurados en cada Branch.
 */
public class Dispatcher {

    /** Las 3 colas de cada sucursal */
    public static class BranchQueues {
        public final Queue ingreso   = new Queue("Ingreso");
        public final Queue traspaso  = new Queue("Traspaso");
        public final Queue salida    = new Queue("Salida");
        public final String branchId;
        BranchQueues(String id) { this.branchId = id; }
    }

    private final BranchManager                     manager;
    private final Map<String, BranchQueues>         queues;
    private final List<SimulationEvent>             log;

    /** Callback para notificar a la GUI de cambios */
    private Runnable onUpdate;

    public Dispatcher(BranchManager manager) {
        this.manager = manager;
        this.queues  = new ConcurrentHashMap<>();
        this.log     = new ArrayList<>();
    }

    public void setOnUpdate(Runnable r) { this.onUpdate = r; }

    /** Obtiene o crea las colas de una sucursal */
    public BranchQueues getQueues(String branchId) {
        return queues.computeIfAbsent(branchId, BranchQueues::new);
    }

    /**
     * Inicia el despacho de una transferencia.
     * Coloca el producto en la cola de ingreso de la primera sucursal
     * y programa el avance hop a hop en un hilo separado.
     */
    public void dispatch(Transfer transfer) {
        if (transfer.getRoute() == null || transfer.getRoute().isEmpty()) return;

        Thread t = new Thread(() -> {
            List<String> route = transfer.getRoute();
            Product p = transfer.getProduct();

            for (int hop = 0; hop < route.size(); hop++) {
                String branchId = route.get(hop);
                Branch branch   = manager.getBranch(branchId);
                if (branch == null) continue;

                BranchQueues bq = getQueues(branchId);
                boolean isDestination = (hop == route.size() - 1);
                boolean isIntermediate = (!isDestination && hop > 0);

                // ── Cola de ingreso ──────────────────────────────────────
                bq.ingreso.enqueue(p);
                logEvent(branchId, p.getBarcode(), "Ingresó a cola de INGRESO");
                notifyGUI();
                sleep(branch.getTimeIngreso() * 1000L);
                bq.ingreso.dequeue();
                notifyGUI();

                if (isDestination) {
                    // Producto llegó al destino final
                    p.setStatus("Disponible");
                    p.setBranchId(branchId);
                    manager.addProduct(branchId, p.copy());
                    transfer.setPhase(Transfer.Phase.DELIVERED);
                    logEvent(branchId, p.getBarcode(), " Entregado en destino");
                    notifyGUI();
                    break;
                }

                // ── Cola de traspaso (sucursal intermedia) ───────────────
                bq.traspaso.enqueue(p);
                logEvent(branchId, p.getBarcode(), "En cola de TRASPASO");
                notifyGUI();
                sleep(branch.getTimeTraspaso() * 1000L);
                bq.traspaso.dequeue();

                // ── Cola de salida ───────────────────────────────────────
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
        // Aceleramos la simulación: máximo 3s de espera visual por etapa
        long capped = Math.min(ms, 3000);
        try { Thread.sleep(Math.max(500, capped)); }
        catch (InterruptedException ignored) {}
    }

    private void logEvent(String branchId, String barcode, String msg) {
        synchronized (log) {
            log.add(new SimulationEvent(branchId, barcode, msg,
                    System.currentTimeMillis()));
        }
    }

    private void notifyGUI() {
        if (onUpdate != null) {
            try { onUpdate.run(); } catch (Exception ignored) {}
        }
    }

    public List<SimulationEvent> getLog() { synchronized(log){ return new ArrayList<>(log); } }
    public Map<String, BranchQueues> getAllQueues() { return queues; }

    // ── Evento de simulación ──────────────────────────────────────────────
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
