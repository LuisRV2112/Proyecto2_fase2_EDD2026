package edu.usac.edd.structures;

import edu.usac.edd.model.Product;
import java.util.function.Consumer;

/**
 * Árbol B+ de orden ORDER=4. Clave: categoría.
 * Hojas enlazadas para recorrido eficiente por categoría.
 * Big-O: búsqueda/inserción O(log n). Recorrido por categoría O(log n + k).
 */
public class BPlusTree {

    public static final int ORDER = 4;

    // ── Nodo hoja ─────────────────────────────────────────────────────────
    public static class LeafNode {
        public final boolean isLeaf = true;
        public String[]   keys    = new String[ORDER];
        public Product[]  records = new Product[ORDER];
        public int        n       = 0;
        public LeafNode   next    = null;
    }

    // ── Nodo interno ──────────────────────────────────────────────────────
    public static class InternalNode {
        public final boolean isLeaf = false;
        public String[] keys     = new String[ORDER];
        public Object[] children = new Object[ORDER + 1];
        public int      n        = 0;
    }

    private Object root;
    private int    size;

    private boolean isLeaf(Object node) {
        return node instanceof LeafNode;
    }

    // ── findLeaf ──────────────────────────────────────────────────────────
    private LeafNode findLeaf(String key) {
        if (root == null) return null;
        Object cur = root;
        while (!isLeaf(cur)) {
            InternalNode nd = (InternalNode) cur;
            int i = 0;
            while (i < nd.n && key.compareTo(nd.keys[i]) >= 0) i++;
            cur = nd.children[i];
        }
        return (LeafNode) cur;
    }

    // ── Inserción ─────────────────────────────────────────────────────────
    public void insert(Product p) {
        if (root == null) {
            LeafNode leaf = new LeafNode();
            leaf.keys[0] = p.getCategory(); leaf.records[0] = p; leaf.n = 1;
            root = leaf; size++; return;
        }
        Object[] result = insertInto(root, p.getCategory(), p);
        if (result != null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys[0]     = (String) result[0];
            newRoot.children[0] = root;
            newRoot.children[1] = result[1];
            newRoot.n           = 1;
            root                = newRoot;
        }
        size++;
    }

    /** Returns [promotedKey, newRightChild] or null if no split */
    private Object[] insertInto(Object node, String key, Product p) {
        if (isLeaf(node)) return insertLeaf((LeafNode) node, key, p);
        InternalNode nd = (InternalNode) node;
        int i = 0;
        while (i < nd.n && key.compareTo(nd.keys[i]) >= 0) i++;
        Object[] res = insertInto(nd.children[i], key, p);
        if (res == null) return null;
        return insertInternal(nd, (String) res[0], res[1]);
    }

    private Object[] insertLeaf(LeafNode leaf, String key, Product p) {
        if (leaf.n < ORDER - 1) {
            int i = leaf.n - 1;
            while (i >= 0 && leaf.keys[i].compareTo(key) > 0) {
                leaf.keys[i+1] = leaf.keys[i]; leaf.records[i+1] = leaf.records[i]; i--;
            }
            leaf.keys[i+1] = key; leaf.records[i+1] = p; leaf.n++;
            return null;
        }
        // Split
        String[] tk = new String[ORDER]; Product[] tr = new Product[ORDER];
        int total = 0; boolean placed = false;
        for (int k = 0; k < leaf.n; k++) {
            if (!placed && leaf.keys[k].compareTo(key) > 0) {
                tk[total] = key; tr[total] = p; total++; placed = true;
            }
            tk[total] = leaf.keys[k]; tr[total] = leaf.records[k]; total++;
        }
        if (!placed) { tk[total] = key; tr[total] = p; total++; }
        int mid = total / 2;
        leaf.n = mid;
        for (int k = 0; k < mid; k++) { leaf.keys[k] = tk[k]; leaf.records[k] = tr[k]; }
        LeafNode right = new LeafNode();
        right.n = total - mid;
        for (int k = 0; k < right.n; k++) { right.keys[k] = tk[mid+k]; right.records[k] = tr[mid+k]; }
        right.next = leaf.next; leaf.next = right;
        return new Object[]{right.keys[0], right};
    }

    private Object[] insertInternal(InternalNode nd, String key, Object rightChild) {
        if (nd.n < ORDER - 1) {
            int i = nd.n - 1;
            while (i >= 0 && nd.keys[i].compareTo(key) > 0) {
                nd.keys[i+1] = nd.keys[i]; nd.children[i+2] = nd.children[i+1]; i--;
            }
            nd.keys[i+1] = key; nd.children[i+2] = rightChild; nd.n++;
            return null;
        }
        // Split internal
        String[] tk = new String[ORDER]; Object[] tc = new Object[ORDER+1];
        tc[0] = nd.children[0];
        int total = 0; boolean placed = false;
        for (int k = 0; k < nd.n; k++) {
            if (!placed && nd.keys[k].compareTo(key) > 0) {
                tk[total] = key; tc[total+1] = rightChild; total++; placed = true;
            }
            tk[total] = nd.keys[k]; tc[total+1] = nd.children[k+1]; total++;
        }
        if (!placed) { tk[total] = key; tc[total+1] = rightChild; total++; }
        int mid = total / 2;
        String prom = tk[mid];
        nd.n = mid; nd.children[0] = tc[0];
        for (int k = 0; k < mid; k++) { nd.keys[k] = tk[k]; nd.children[k+1] = tc[k+1]; }
        InternalNode right = new InternalNode();
        right.n = total - mid - 1; right.children[0] = tc[mid+1];
        for (int k = 0; k < right.n; k++) { right.keys[k] = tk[mid+1+k]; right.children[k+1] = tc[mid+2+k]; }
        return new Object[]{prom, right};
    }

    // ── Búsqueda por categoría O(log n + k) ───────────────────────────────
    public void searchByCategory(String category, Consumer<Product> action) {
        LeafNode leaf = findLeaf(category);
        while (leaf != null) {
            for (int i = 0; i < leaf.n; i++)
                if (leaf.keys[i].equals(category)) action.accept(leaf.records[i]);
            if (leaf.next != null && leaf.next.keys[0].compareTo(category) <= 0)
                leaf = leaf.next;
            else break;
        }
    }

    // ── Eliminación simplificada ──────────────────────────────────────────
    public boolean remove(String category, String barcode) {
        LeafNode leaf = findLeaf(category);
        if (leaf == null) return false;
        for (int i = 0; i < leaf.n; i++) {
            if (leaf.keys[i].equals(category) && leaf.records[i].getBarcode().equals(barcode)) {
                for (int j = i+1; j < leaf.n; j++) {
                    leaf.keys[j-1] = leaf.keys[j]; leaf.records[j-1] = leaf.records[j];
                }
                leaf.n--; size--; return true;
            }
        }
        return false;
    }

    // ── DOT export ────────────────────────────────────────────────────────
    public String toDot() {
        StringBuilder sb = new StringBuilder("digraph BPlusTree {\n  rankdir=TB;\n");
        sb.append("  node [shape=record];\n");
        int[] id = {0};
        toDot(root, sb, id);
        sb.append("}\n");
        return sb.toString();
    }
    private void toDot(Object node, StringBuilder sb, int[] id) {
        if (node == null) return;
        int myId = id[0]++;
        if (isLeaf(node)) {
            LeafNode leaf = (LeafNode) node;
            sb.append("  node").append(myId)
              .append(" [style=filled, fillcolor=lightgreen, label=\"{");
            for (int i = 0; i < leaf.n; i++) { if (i>0) sb.append("|"); sb.append(leaf.keys[i]); }
            sb.append("}\"];\n");
            if (leaf.next != null)
                sb.append("  node").append(myId).append(" -> node").append(id[0])
                  .append(" [style=dashed, constraint=false];\n");
        } else {
            InternalNode nd = (InternalNode) node;
            sb.append("  node").append(myId)
              .append(" [style=filled, fillcolor=lightyellow, label=\"");
            for (int i = 0; i < nd.n; i++) sb.append("<f").append(i).append(">|").append(nd.keys[i]).append("|");
            sb.append("<f").append(nd.n).append(">\"];\n");
            for (int i = 0; i <= nd.n; i++) {
                int childId = id[0];
                toDot(nd.children[i], sb, id);
                sb.append("  node").append(myId).append(":f").append(i)
                  .append(" -> node").append(childId).append(";\n");
            }
        }
    }

    public int     size()    { return size; }
    public boolean isEmpty() { return root == null; }
    public Object  getRoot() { return root; }

    /** Iteración completa por hojas enlazadas O(n) */
    public void forEach(Consumer<Product> action) {
        if (root == null) return;
        Object cur = root;
        while (!isLeaf(cur)) cur = ((InternalNode) cur).children[0];
        LeafNode leaf = (LeafNode) cur;
        while (leaf != null) {
            for (int i = 0; i < leaf.n; i++) action.accept(leaf.records[i]);
            leaf = leaf.next;
        }
    }
}
