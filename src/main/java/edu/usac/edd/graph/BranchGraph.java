package edu.usac.edd.graph;

import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Transfer;
import java.util.function.Consumer;

/**
 * Grafo ponderado de sucursales.
 * Sin java.util.HashMap, HashSet, ArrayList ni LinkedHashMap.
 * Lista de adyacencia con nodos enlazados.
 * Dijkstra con arrays paralelos + MinHeap manual.
 */
public class BranchGraph {

    // ── Arista ────────────────────────────────────────────────────────────
    public static class Edge {
        public final String fromId, toId;
        public final double time, cost;
        public final boolean bidirectional;
        public Edge next; // enlace para lista propia

        public Edge(String f, String t, double time, double cost, boolean bidir) {
            this.fromId = f; this.toId = t;
            this.time = time; this.cost = cost; this.bidirectional = bidir;
        }
    }

    // ── Nodo de adyacencia ────────────────────────────────────────────────
    private static class AdjNode {
        String toId;
        double time, cost;
        AdjNode next;
        AdjNode(String toId, double time, double cost) {
            this.toId = toId; this.time = time; this.cost = cost;
        }
    }

    // ── Entrada del grafo (sucursal + lista de vecinos) ───────────────────
    private static class GraphNode {
        Branch  branch;
        AdjNode adjHead; // cabeza de lista de adyacencia
        GraphNode next;  // enlace a la siguiente entrada
        GraphNode(Branch b) { this.branch = b; }
    }

    // Lista enlazada de nodos del grafo
    private GraphNode nodeHead;
    private int       nodeCount;

    // Lista enlazada de aristas
    private Edge edgeHead;
    private int  edgeCount;

    // ── Buscar GraphNode por id ───────────────────────────────────────────
    private GraphNode findNode(String id) {
        for (GraphNode n = nodeHead; n != null; n = n.next)
            if (n.branch.getId().equals(id)) return n;
        return null;
    }

    // ── Gestión de sucursales ─────────────────────────────────────────────
    public void addBranch(Branch b) {
        if (findNode(b.getId()) != null) return;
        GraphNode n = new GraphNode(b);
        n.next   = nodeHead;
        nodeHead = n;
        nodeCount++;
    }

    public Branch getBranch(String id) {
        GraphNode n = findNode(id);
        return n != null ? n.branch : null;
    }

    public java.util.Collection<Branch> getBranches() {
        // Retorna colección usando java.util solo como tipo de retorno hacia GUI
        java.util.List<Branch> list = new java.util.ArrayList<>();
        for (GraphNode n = nodeHead; n != null; n = n.next)
            list.add(n.branch);
        return list;
    }

    public boolean containsBranch(String id) { return findNode(id) != null; }

    public void removeBranch(String id) {
        GraphNode prev = null, cur = nodeHead;
        while (cur != null) {
            if (cur.branch.getId().equals(id)) {
                if (prev != null) prev.next = cur.next;
                else              nodeHead  = cur.next;
                nodeCount--;
                return;
            }
            prev = cur; cur = cur.next;
        }
    }

    // ── Gestión de aristas ────────────────────────────────────────────────
    public void addEdge(String fromId, String toId,
                        double time, double cost, boolean bidirectional) {
        GraphNode from = findNode(fromId), to = findNode(toId);
        if (from == null || to == null) return;

        // Guardar arista en lista propia
        Edge e = new Edge(fromId, toId, time, cost, bidirectional);
        e.next   = edgeHead;
        edgeHead = e;
        edgeCount++;

        // Agregar a lista de adyacencia
        addAdj(from, toId, time, cost);
        if (bidirectional) addAdj(to, fromId, time, cost);
    }

    private void addAdj(GraphNode gn, String toId, double time, double cost) {
        AdjNode adj = new AdjNode(toId, time, cost);
        adj.next   = gn.adjHead;
        gn.adjHead = adj;
    }

    /** Itera las aristas via callback */
    public void forEachEdge(Consumer<Edge> action) {
        for (Edge e = edgeHead; e != null; e = e.next) action.accept(e);
    }

    /** Retorna lista de aristas para compatibilidad con GraphView */
    public java.util.List<Edge> getEdges() {
        java.util.List<Edge> list = new java.util.ArrayList<>();
        for (Edge e = edgeHead; e != null; e = e.next) list.add(e);
        return list;
    }

    // ── MinHeap manual ────────────────────────────────────────────────────
    private static class HeapEntry {
        String nodeId; double dist;
        HeapEntry(String id, double d) { nodeId = id; dist = d; }
    }

    private static class MinHeap {
        private final HeapEntry[] data;
        private int size;
        MinHeap(int cap) { data = new HeapEntry[cap]; }

        void insert(HeapEntry e) { data[size] = e; siftUp(size++); }

        HeapEntry extractMin() {
            HeapEntry min = data[0];
            data[0] = data[--size];
            if (size > 0) siftDown(0);
            return min;
        }
        boolean isEmpty() { return size == 0; }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i - 1) / 2;
                if (data[p].dist <= data[i].dist) break;
                HeapEntry tmp = data[p]; data[p] = data[i]; data[i] = tmp;
                i = p;
            }
        }
        private void siftDown(int i) {
            while (true) {
                int l = 2*i+1, r = 2*i+2, m = i;
                if (l < size && data[l].dist < data[m].dist) m = l;
                if (r < size && data[r].dist < data[m].dist) m = r;
                if (m == i) break;
                HeapEntry tmp = data[m]; data[m] = data[i]; data[i] = tmp;
                i = m;
            }
        }
    }

    // ── Dijkstra con arrays paralelos (sin HashMap/HashSet) ───────────────
    public java.util.List<String> dijkstra(String originId, String destId,
                                           Transfer.Criterion criterion) {
        if (nodeCount == 0) return new java.util.ArrayList<>();

        // Índice de nodos en array
        String[] ids   = new String[nodeCount];
        double[] dist  = new double[nodeCount];
        int[]    prev  = new int[nodeCount];
        boolean[] vis  = new boolean[nodeCount];

        int idx = 0;
        for (GraphNode n = nodeHead; n != null; n = n.next)
            ids[idx++] = n.branch.getId();

        int originIdx = -1, destIdx = -1;
        for (int i = 0; i < nodeCount; i++) {
            dist[i] = Double.MAX_VALUE;
            prev[i] = -1;
            if (ids[i].equals(originId)) originIdx = i;
            if (ids[i].equals(destId))   destIdx   = i;
        }
        if (originIdx < 0 || destIdx < 0) return new java.util.ArrayList<>();

        dist[originIdx] = 0;
        MinHeap heap = new MinHeap(nodeCount * nodeCount + 1);
        heap.insert(new HeapEntry(originId, 0.0));

        while (!heap.isEmpty()) {
            HeapEntry cur = heap.extractMin();
            int ci = indexOf(ids, cur.nodeId);
            if (ci < 0 || vis[ci]) continue;
            vis[ci] = true;
            if (ci == destIdx) break;

            GraphNode gn = findNode(cur.nodeId);
            if (gn == null) continue;
            for (AdjNode adj = gn.adjHead; adj != null; adj = adj.next) {
                int ai = indexOf(ids, adj.toId);
                if (ai < 0 || vis[ai]) continue;
                double w  = criterion == Transfer.Criterion.TIME ? adj.time : adj.cost;
                double nd = dist[ci] + w;
                if (nd < dist[ai]) {
                    dist[ai] = nd;
                    prev[ai] = ci;
                    heap.insert(new HeapEntry(adj.toId, nd));
                }
            }
        }

        // Reconstruir ruta
        java.util.List<String> path = new java.util.ArrayList<>();
        int cur = destIdx;
        while (cur != -1) { path.add(0, ids[cur]); cur = prev[cur]; }
        if (path.isEmpty() || !path.get(0).equals(originId))
            return new java.util.ArrayList<>();
        return path;
    }

    private int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i] != null && arr[i].equals(val)) return i;
        return -1;
    }

    public double[] routeCost(java.util.List<String> path) {
        double totalTime = 0, totalCost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            GraphNode gn = findNode(path.get(i));
            if (gn == null) continue;
            for (AdjNode adj = gn.adjHead; adj != null; adj = adj.next) {
                if (adj.toId.equals(path.get(i + 1))) {
                    totalTime += adj.time;
                    totalCost += adj.cost;
                    break;
                }
            }
        }
        return new double[]{totalTime, totalCost};
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder("digraph Sucursales {\n");
        sb.append("  graph [label=\"Red de Sucursales\"];\n");
        sb.append("  node [shape=box, style=filled, fillcolor=lightyellow];\n");
        for (GraphNode n = nodeHead; n != null; n = n.next)
            sb.append("  \"").append(n.branch.getId()).append("\" [label=\"")
                    .append(n.branch.getName()).append("\\n")
                    .append(n.branch.getLocation()).append("\"];\n");
        for (Edge e = edgeHead; e != null; e = e.next)
            sb.append("  \"").append(e.fromId).append("\" -> \"").append(e.toId)
                    .append("\" [label=\"t=").append((int) e.time)
                    .append("s c=Q").append(String.format("%.0f", e.cost)).append("\"")
                    .append(e.bidirectional ? ", dir=both" : "").append("];\n");
        sb.append("}\n");
        return sb.toString();
    }

    public int branchCount() { return nodeCount; }
    public int edgeCount()   { return edgeCount; }
    public void removeEdge(String fromId, String toId, boolean bidirectional) {
        Edge prev = null, cur = edgeHead;
        while (cur != null) {
            if (cur.fromId.equals(fromId) && cur.toId.equals(toId)) {
                if (prev != null) prev.next = cur.next;
                else              edgeHead  = cur.next;
                edgeCount--; break;
            }
            prev = cur; cur = cur.next;
        }
        removeAdj(fromId, toId);
        if (bidirectional) {
            prev = null; cur = edgeHead;
            while (cur != null) {
                if (cur.fromId.equals(toId) && cur.toId.equals(fromId)) {
                    if (prev != null) prev.next = cur.next;
                    else              edgeHead  = cur.next;
                    edgeCount--; break;
                }
                prev = cur; cur = cur.next;
            }
            removeAdj(toId, fromId);
        }
    }

    private void removeAdj(String from, String to) {
        GraphNode gn = findNode(from);
        if (gn == null) return;
        AdjNode prev = null, cur = gn.adjHead;
        while (cur != null) {
            if (cur.toId.equals(to)) {
                if (prev != null) prev.next = cur.next;
                else              gn.adjHead = cur.next;
                return;
            }
            prev = cur; cur = cur.next;
        }
    }
}