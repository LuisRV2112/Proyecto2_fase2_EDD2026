package edu.usac.edd.graph;

import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Transfer;
import java.util.*;
import java.util.function.Consumer;

/**
 * Grafo ponderado de sucursales.
 * Nodos=sucursales, aristas=conexiones con tiempo y costo.
 * Implementado con lista de adyacencia.
 *
 * Dijkstra con MinHeap manual.
 * Big-O Dijkstra: O((V+E) log V).
 */
public class BranchGraph {

    // Arista
    public static class Edge {
        public final String fromId;
        public final String toId;
        public final double time;        // segundos
        public final double cost;        // unidades monetarias
        public final boolean bidirectional;

        public Edge(String fromId, String toId, double time,
                    double cost, boolean bidirectional) {
            this.fromId        = fromId;
            this.toId          = toId;
            this.time          = time;
            this.cost          = cost;
            this.bidirectional = bidirectional;
        }
    }

    // Nodo de adyacencia
    private static class AdjNode {
        String toId;
        double weight;  // time o cost según criterio activo
        double time, cost;
        AdjNode next;
        AdjNode(String toId, double time, double cost) {
            this.toId = toId; this.time = time; this.cost = cost;
        }
    }

    // Almacenamiento
    private final Map<String, Branch>   branches  = new LinkedHashMap<>();
    private final Map<String, AdjNode>  adjList   = new LinkedHashMap<>();
    private final List<Edge>            edges     = new ArrayList<>();

    // Gestión de sucursales
    public void addBranch(Branch b) {
        branches.put(b.getId(), b);
        adjList.put(b.getId(), null);
    }

    public Branch getBranch(String id)            { return branches.get(id); }
    public Collection<Branch> getBranches()        { return branches.values(); }
    public boolean containsBranch(String id)       { return branches.containsKey(id); }
    public void removeBranch(String id)            { branches.remove(id); adjList.remove(id); }

    // Gestión de aristas
    public void addEdge(String fromId, String toId,
                        double time, double cost, boolean bidirectional) {
        if (!branches.containsKey(fromId) || !branches.containsKey(toId)) return;
        edges.add(new Edge(fromId, toId, time, cost, bidirectional));
        addAdj(fromId, toId, time, cost);
        if (bidirectional) addAdj(toId, fromId, time, cost);
    }

    private void addAdj(String from, String to, double time, double cost) {
        AdjNode node = new AdjNode(to, time, cost);
        node.next    = adjList.get(from);
        adjList.put(from, node);
    }

    public List<Edge> getEdges() { return edges; }

    /** Elimina arista (y su inversa si es bidireccional). */
    public void removeEdge(String fromId, String toId, boolean bidirectional) {
        edges.removeIf(e -> e.fromId.equals(fromId) && e.toId.equals(toId));
        removeAdj(fromId, toId);
        if (bidirectional) {
            edges.removeIf(e -> e.fromId.equals(toId) && e.toId.equals(fromId));
            removeAdj(toId, fromId);
        }
    }

    private void removeAdj(String from, String to) {
        AdjNode cur = adjList.get(from);
        AdjNode prev = null;
        while (cur != null) {
            if (cur.toId.equals(to)) {
                if (prev == null) adjList.put(from, cur.next);
                else              prev.next = cur.next;
                return;
            }
            prev = cur; cur = cur.next;
        }
    }

    // Vecinos
    public List<AdjNode> neighbors(String id) {
        List<AdjNode> list = new ArrayList<>();
        AdjNode cur = adjList.get(id);
        while (cur != null) { list.add(cur); cur = cur.next; }
        return list;
    }

    // Dijkstra con MinHeap manual

    /** Entrada del heap */
    private static class HeapEntry {
        String nodeId;
        double dist;
        HeapEntry(String id, double d) { nodeId = id; dist = d; }
    }

    /** MinHeap mínimo implementado con array */
    private static class MinHeap {
        private final HeapEntry[] data;
        private int size;
        MinHeap(int cap) { data = new HeapEntry[cap]; }

        void insert(HeapEntry e) {
            data[size] = e;
            siftUp(size++);
        }
        HeapEntry extractMin() {
            HeapEntry min = data[0];
            data[0] = data[--size];
            siftDown(0);
            return min;
        }
        boolean isEmpty() { return size == 0; }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i-1)/2;
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

    /** Dijkstra: ruta optima de originId a destId.
     * @param criterion TIME o COST
     * @return IDs en orden (origen a destino), vacia si no hay ruta.
     */
    public List<String> dijkstra(String originId, String destId,
                                  Transfer.Criterion criterion) {
        if (!branches.containsKey(originId) || !branches.containsKey(destId))
            return new ArrayList<>();

        Map<String, Double> dist  = new HashMap<>();
        Map<String, String> prev  = new HashMap<>();
        Set<String>         vis   = new HashSet<>();

        for (String id : branches.keySet()) dist.put(id, Double.MAX_VALUE);
        dist.put(originId, 0.0);

        MinHeap heap = new MinHeap(branches.size() * 2 + 1);
        heap.insert(new HeapEntry(originId, 0.0));

        while (!heap.isEmpty()) {
            HeapEntry cur = heap.extractMin();
            if (vis.contains(cur.nodeId)) continue;
            vis.add(cur.nodeId);

            if (cur.nodeId.equals(destId)) break;

            for (AdjNode adj : neighbors(cur.nodeId)) {
                if (vis.contains(adj.toId)) continue;
                double w = criterion == Transfer.Criterion.TIME ? adj.time : adj.cost;
                double nd = dist.get(cur.nodeId) + w;
                if (nd < dist.get(adj.toId)) {
                    dist.put(adj.toId, nd);
                    prev.put(adj.toId, cur.nodeId);
                    heap.insert(new HeapEntry(adj.toId, nd));
                }
            }
        }

        // Reconstruir ruta
        List<String> path = new ArrayList<>();
        String cur = destId;
        while (cur != null) {
            path.add(0, cur);
            cur = prev.get(cur);
        }
        if (path.isEmpty() || !path.get(0).equals(originId)) return new ArrayList<>();
        return path;
    }

    /** Costo total de ruta.
     * @return [tiempo, costo]
     */
    public double[] routeCost(List<String> path) {
        double totalTime = 0, totalCost = 0;
        for (int i = 0; i < path.size()-1; i++) {
            String from = path.get(i), to = path.get(i+1);
            AdjNode cur = adjList.get(from);
            while (cur != null) {
                if (cur.toId.equals(to)) {
                    totalTime += cur.time;
                    totalCost += cur.cost;
                    break;
                }
                cur = cur.next;
            }
        }
        return new double[]{totalTime, totalCost};
    }

    /** Genera DOT del grafo de sucursales */
    public String toDot() {
        StringBuilder sb = new StringBuilder("digraph Sucursales {\n");
        sb.append("  graph [label=\"Red de Sucursales\"];\n");
        sb.append("  node [shape=box, style=filled, fillcolor=lightyellow];\n");
        for (Branch b : branches.values())
            sb.append("  \"").append(b.getId()).append("\" [label=\"")
              .append(b.getName()).append("\\n").append(b.getLocation()).append("\"];\n");
        for (Edge e : edges)
            sb.append("  \"").append(e.fromId).append("\" -> \"").append(e.toId)
              .append("\" [label=\"t=").append((int)e.time).append("s c=Q")
              .append(String.format("%.0f", e.cost)).append("\"")
              .append(e.bidirectional ? ", dir=both" : "").append("];\n");
        sb.append("}\n");
        return sb.toString();
    }

    public int branchCount() { return branches.size(); }
    public int edgeCount()   { return edges.size(); }
}
