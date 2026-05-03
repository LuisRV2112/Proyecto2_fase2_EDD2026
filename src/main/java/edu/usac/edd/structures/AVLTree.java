package edu.usac.edd.structures;

import edu.usac.edd.model.Product;
import java.util.function.Consumer;

/**
 * Árbol AVL ordenado por NOMBRE de producto.
 * Big-O: búsqueda/inserción/eliminación O(log n).
 *        Recorrido in-order O(n).
 */
public class AVLTree {

    public static class Node {
        public Product data;
        public Node    left, right;
        public int     height;
        Node(Product p) { data = p; height = 1; }
    }

    private Node root;

    // ── Utilidades ────────────────────────────────────────────────────────
    private int height(Node n)        { return n == null ? 0 : n.height; }
    private int bf(Node n)            { return n == null ? 0 : height(n.left) - height(n.right); }
    private void updateHeight(Node n) {
        if (n != null) n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    // ── Rotaciones ────────────────────────────────────────────────────────
    private Node rotRight(Node y) {
        Node x = y.left, T2 = x.right;
        x.right = y; y.left = T2;
        updateHeight(y); updateHeight(x);
        return x;
    }
    private Node rotLeft(Node x) {
        Node y = x.right, T2 = y.left;
        y.left = x; x.right = T2;
        updateHeight(x); updateHeight(y);
        return y;
    }
    private Node rebalance(Node n) {
        updateHeight(n);
        int b = bf(n);
        if (b >  1 && bf(n.left)  >= 0) return rotRight(n);
        if (b >  1 && bf(n.left)  <  0) { n.left  = rotLeft(n.left);  return rotRight(n); }
        if (b < -1 && bf(n.right) <= 0) return rotLeft(n);
        if (b < -1 && bf(n.right) >  0) { n.right = rotRight(n.right); return rotLeft(n); }
        return n;
    }

    // ── Inserción O(log n) ────────────────────────────────────────────────
    public boolean insert(Product p) {
        int before = size(root);
        root = insert(root, p);
        return size(root) > before;
    }
    private Node insert(Node n, Product p) {
        if (n == null) return new Node(p);
        int cmp = p.getName().compareToIgnoreCase(n.data.getName());
        if      (cmp < 0) n.left  = insert(n.left,  p);
        else if (cmp > 0) n.right = insert(n.right, p);
        else return n; // duplicado de nombre ignorado
        return rebalance(n);
    }

    // ── Eliminación O(log n) ──────────────────────────────────────────────
    public boolean remove(String name, String barcode) {
        int before = size(root);
        root = remove(root, name, barcode);
        return size(root) < before;
    }
    private Node remove(Node n, String name, String barcode) {
        if (n == null) return null;
        int cmp = name.compareToIgnoreCase(n.data.getName());
        if      (cmp < 0) n.left  = remove(n.left,  name, barcode);
        else if (cmp > 0) n.right = remove(n.right, name, barcode);
        else {
            if (!n.data.getBarcode().equals(barcode)) return n;
            if (n.left == null || n.right == null)
                return n.left != null ? n.left : n.right;
            Node succ = minNode(n.right);
            n.data  = succ.data;
            n.right = remove(n.right, succ.data.getName(), succ.data.getBarcode());
        }
        return rebalance(n);
    }
    private Node minNode(Node n) {
        while (n.left != null) n = n.left;
        return n;
    }

    // ── Búsqueda O(log n) ─────────────────────────────────────────────────
    public Product search(String name) {
        Node cur = root;
        while (cur != null) {
            int cmp = name.compareToIgnoreCase(cur.data.getName());
            if      (cmp == 0) return cur.data;
            else if (cmp <  0) cur = cur.left;
            else               cur = cur.right;
        }
        return null;
    }

    // ── In-order O(n) ─────────────────────────────────────────────────────
    public void inOrder(Consumer<Product> action) { inOrder(root, action); }
    private void inOrder(Node n, Consumer<Product> action) {
        if (n == null) return;
        inOrder(n.left, action);
        action.accept(n.data);
        inOrder(n.right, action);
    }

    // ── DOT export ────────────────────────────────────────────────────────
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph AVL {\n");
        sb.append("  node [shape=ellipse, style=filled, fillcolor=lightblue];\n");
        toDot(root, sb);
        sb.append("}\n");
        return sb.toString();
    }
    private void toDot(Node n, StringBuilder sb) {
        if (n == null) return;
        String id = n.data.getName().replace("\"","").replace(" ","_");
        sb.append("  \"").append(id).append("\" [label=\"")
          .append(n.data.getName().length() > 15
                  ? n.data.getName().substring(0,15)+"..." : n.data.getName())
          .append("\\nh=").append(n.height).append("\"];\n");
        if (n.left  != null) {
            String lid = n.left.data.getName().replace("\"","").replace(" ","_");
            sb.append("  \"").append(id).append("\" -> \"").append(lid).append("\" [label=L];\n");
            toDot(n.left, sb);
        }
        if (n.right != null) {
            String rid = n.right.data.getName().replace("\"","").replace(" ","_");
            sb.append("  \"").append(id).append("\" -> \"").append(rid).append("\" [label=R];\n");
            toDot(n.right, sb);
        }
    }

    public int  size()         { return size(root); }
    private int size(Node n)   { return n == null ? 0 : 1 + size(n.left) + size(n.right); }
    public boolean isEmpty()   { return root == null; }
    public Node  getRoot()     { return root; }
}
