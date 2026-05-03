package edu.usac.edd.structures;

import edu.usac.edd.model.Product;
import java.util.function.Consumer;

/**
 * Árbol B de orden T=3.
 * Clave: fecha de caducidad "YYYY-MM-DD" (comparación lexicográfica).
 * Big-O: búsqueda/inserción/eliminación O(T·log_T n).
 *        Rango: O(T·log_T n + k).
 */
public class BTree {

    private static final int T = 3;

    public static class BNode {
        public String[]  keys     = new String[2*T-1];
        public Product[] products = new Product[2*T-1];
        public BNode[]   children = new BNode[2*T];
        public int       n        = 0;
        public boolean   isLeaf   = true;
    }

    private BNode root;
    private int   size;

    public void insert(Product p) {
        if (root == null) {
            root             = new BNode();
            root.keys[0]     = p.getExpiryDate();
            root.products[0] = p;
            root.n           = 1;
            size++;
            return;
        }
        if (root.n == 2*T-1) {
            BNode s = new BNode();
            s.isLeaf      = false;
            s.children[0] = root;
            splitChild(s, 0, root);
            root = s;
        }
        insertNonFull(root, p);
        size++;
    }

    private void splitChild(BNode parent, int i, BNode child) {
        BNode right  = new BNode();
        right.isLeaf = child.isLeaf;
        right.n      = T - 1;
        for (int j = 0; j < T-1; j++) {
            right.keys[j]     = child.keys[j+T];
            right.products[j] = child.products[j+T];
        }
        if (!child.isLeaf)
            for (int j = 0; j < T; j++) right.children[j] = child.children[j+T];
        child.n = T - 1;
        for (int j = parent.n; j >= i+1; j--) parent.children[j+1] = parent.children[j];
        parent.children[i+1] = right;
        for (int j = parent.n-1; j >= i; j--) {
            parent.keys[j+1]     = parent.keys[j];
            parent.products[j+1] = parent.products[j];
        }
        parent.keys[i]     = child.keys[T-1];
        parent.products[i] = child.products[T-1];
        parent.n++;
    }

    private void insertNonFull(BNode node, Product p) {
        int i = node.n - 1;
        if (node.isLeaf) {
            while (i >= 0 && p.getExpiryDate().compareTo(node.keys[i]) < 0) {
                node.keys[i+1]     = node.keys[i];
                node.products[i+1] = node.products[i];
                i--;
            }
            node.keys[i+1]     = p.getExpiryDate();
            node.products[i+1] = p;
            node.n++;
        } else {
            while (i >= 0 && p.getExpiryDate().compareTo(node.keys[i]) < 0) i--;
            i++;
            if (node.children[i].n == 2*T-1) {
                splitChild(node, i, node.children[i]);
                if (p.getExpiryDate().compareTo(node.keys[i]) > 0) i++;
            }
            insertNonFull(node.children[i], p);
        }
    }

    public boolean remove(String expiryDate, String barcode) {
        if (root == null) return false;
        boolean[] found = {false};
        root = removeFromNode(root, expiryDate, barcode, found);
        if (root != null && root.n == 0)
            root = root.isLeaf ? null : root.children[0];
        if (found[0]) size--;
        return found[0];
    }

    private BNode removeFromNode(BNode node, String key, String barcode, boolean[] found) {
        int idx = findKey(node, key);
        if (idx < node.n && node.keys[idx].equals(key)) {
            if (node.products[idx].getBarcode().equals(barcode)) {
                if (node.isLeaf) { removeFromLeaf(node, idx); found[0] = true; }
                else             { removeFromInternal(node, idx, barcode, found); }
            }
        } else {
            if (node.isLeaf) return node;
            boolean last = (idx == node.n);
            if (node.children[idx].n < T) fill(node, idx);
            if (last && idx > node.n)
                removeFromNode(node.children[idx-1], key, barcode, found);
            else
                removeFromNode(node.children[idx], key, barcode, found);
        }
        return node;
    }

    private void removeFromLeaf(BNode node, int idx) {
        for (int i = idx+1; i < node.n; i++) {
            node.keys[i-1]     = node.keys[i];
            node.products[i-1] = node.products[i];
        }
        node.n--;
    }

    private void removeFromInternal(BNode node, int idx, String barcode, boolean[] found) {
        if (node.children[idx].n >= T) {
            BNode pred = node.children[idx];
            while (!pred.isLeaf) pred = pred.children[pred.n];
            node.keys[idx]     = pred.keys[pred.n-1];
            node.products[idx] = pred.products[pred.n-1];
            removeFromNode(node.children[idx], node.keys[idx], barcode, found);
        } else if (node.children[idx+1].n >= T) {
            BNode succ = node.children[idx+1];
            while (!succ.isLeaf) succ = succ.children[0];
            node.keys[idx]     = succ.keys[0];
            node.products[idx] = succ.products[0];
            removeFromNode(node.children[idx+1], node.keys[idx], barcode, found);
        } else {
            merge(node, idx);
            removeFromNode(node.children[idx], node.keys[idx], barcode, found);
        }
    }

    private int findKey(BNode node, String key) {
        int idx = 0;
        while (idx < node.n && node.keys[idx].compareTo(key) < 0) idx++;
        return idx;
    }

    private void fill(BNode node, int idx) {
        if (idx != 0 && node.children[idx-1].n >= T)          borrowFromPrev(node, idx);
        else if (idx != node.n && node.children[idx+1].n >= T) borrowFromNext(node, idx);
        else { if (idx != node.n) merge(node, idx); else merge(node, idx-1); }
    }

    private void borrowFromPrev(BNode node, int idx) {
        BNode child = node.children[idx], sib = node.children[idx-1];
        for (int i = child.n-1; i >= 0; i--) {
            child.keys[i+1]     = child.keys[i];
            child.products[i+1] = child.products[i];
        }
        if (!child.isLeaf)
            for (int i = child.n; i >= 0; i--) child.children[i+1] = child.children[i];
        child.keys[0]        = node.keys[idx-1];
        child.products[0]    = node.products[idx-1];
        if (!child.isLeaf) child.children[0] = sib.children[sib.n];
        node.keys[idx-1]     = sib.keys[sib.n-1];
        node.products[idx-1] = sib.products[sib.n-1];
        child.n++; sib.n--;
    }

    private void borrowFromNext(BNode node, int idx) {
        BNode child = node.children[idx], sib = node.children[idx+1];
        child.keys[child.n]     = node.keys[idx];
        child.products[child.n] = node.products[idx];
        if (!child.isLeaf) child.children[child.n+1] = sib.children[0];
        node.keys[idx]     = sib.keys[0];
        node.products[idx] = sib.products[0];
        for (int i = 1; i < sib.n; i++) {
            sib.keys[i-1]     = sib.keys[i];
            sib.products[i-1] = sib.products[i];
        }
        if (!sib.isLeaf)
            for (int i = 1; i <= sib.n; i++) sib.children[i-1] = sib.children[i];
        child.n++; sib.n--;
    }

    private void merge(BNode node, int idx) {
        BNode child = node.children[idx], sib = node.children[idx+1];
        child.keys[T-1]     = node.keys[idx];
        child.products[T-1] = node.products[idx];
        for (int i = 0; i < sib.n; i++) {
            child.keys[i+T]     = sib.keys[i];
            child.products[i+T] = sib.products[i];
        }
        if (!child.isLeaf)
            for (int i = 0; i <= sib.n; i++) child.children[i+T] = sib.children[i];
        for (int i = idx+1; i < node.n; i++) {
            node.keys[i-1]     = node.keys[i];
            node.products[i-1] = node.products[i];
        }
        for (int i = idx+2; i <= node.n; i++) node.children[i-1] = node.children[i];
        child.n += sib.n + 1;
        node.n--;
    }

    public void rangeSearch(String from, String to, Consumer<Product> action) {
        rangeSearch(root, from, to, action);
    }
    private void rangeSearch(BNode node, String from, String to, Consumer<Product> action) {
        if (node == null) return;
        int i = 0;
        while (i < node.n && node.keys[i].compareTo(from) < 0) i++;
        for (; i < node.n && node.keys[i].compareTo(to) <= 0; i++) {
            if (!node.isLeaf) rangeSearch(node.children[i], from, to, action);
            action.accept(node.products[i]);
        }
        if (!node.isLeaf) rangeSearch(node.children[i], from, to, action);
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder("digraph BTree {\n");
        sb.append("  node [shape=record, style=filled, fillcolor=lightyellow];\n");
        int[] id = {0};
        toDot(root, sb, id);
        sb.append("}\n");
        return sb.toString();
    }
    private void toDot(BNode node, StringBuilder sb, int[] id) {
        if (node == null) return;
        int myId = id[0]++;
        sb.append("  node").append(myId).append(" [label=\"");
        for (int i = 0; i < node.n; i++) {
            if (i > 0) sb.append("|");
            sb.append("<f").append(i).append(">").append(node.keys[i]);
        }
        sb.append("\"];\n");
        if (!node.isLeaf)
            for (int i = 0; i <= node.n; i++) {
                int childId = id[0];
                toDot(node.children[i], sb, id);
                sb.append("  node").append(myId).append(" -> node").append(childId).append(";\n");
            }
    }

    public int     size()    { return size; }
    public boolean isEmpty() { return root == null; }
    public BNode   getRoot() { return root; }
}