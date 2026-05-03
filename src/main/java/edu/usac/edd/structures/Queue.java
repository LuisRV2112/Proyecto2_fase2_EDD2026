package edu.usac.edd.structures;

import edu.usac.edd.model.Product;

/**
 * Queue FIFO para simulación de despacho.
 *
 * Big-O: enqueue/dequeue/peek O(1).
 */
public class Queue {

    public static class Node {
        public Product data;
        public Node    next;
        Node(Product p) { this.data = p; }
    }

    private Node   head;   // frente (dequeue aquí)
    private Node   tail;   // final  (enqueue aquí)
    private int    size;
    private final String label;  // "Ingreso" | "Traspaso" | "Salida"

    public Queue(String label) { this.label = label; }
    public Queue()             { this("Cola"); }

    /** Enqueue O(1). */
    public void enqueue(Product p) {
        Node n = new Node(p);
        if (tail != null) tail.next = n;
        tail = n;
        if (head == null) head = n;
        size++;
    }

    /** Dequeue O(1). */
    public Product dequeue() {
        if (head == null) return null;
        Product p = head.data;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return p;
    }

    /** Peek O(1). */
    public Product peek() {
        return head != null ? head.data : null;
    }

    public boolean isEmpty() { return head == null; }
    public int     size()    { return size; }
    public String  getLabel(){ return label; }
    public Node    getHead() { return head; }

    /** Elimina por barcode O(n). */
    public boolean remove(String barcode) {
        Node prev = null, cur = head;
        while (cur != null) {
            if (cur.data.getBarcode().equals(barcode)) {
                if (prev != null) prev.next = cur.next;
                else              head      = cur.next;
                if (cur == tail)  tail      = prev;
                size--;
                return true;
            }
            prev = cur; cur = cur.next;
        }
        return false;
    }
}
