package edu.usac.edd.structures;

import edu.usac.edd.model.Product;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LinkedList {

    public static class Node {
        public Product data;
        public Node    next;
        Node(Product p) { this.data = p; }
    }

    private Node head;
    private int  size;

    
    public void insert(Product p) {
        Node n = new Node(p);
        n.next = head;
        head   = n;
        size++;
    }

    public boolean remove(String barcode) {
        Node prev = null, cur = head;
        while (cur != null) {
            if (cur.data.getBarcode().equals(barcode)) {
                if (prev != null) prev.next = cur.next;
                else              head      = cur.next;
                size--;
                return true;
            }
            prev = cur; cur = cur.next;
        }
        return false;
    }

    public Product searchByBarcode(String barcode) {
        Node cur = head;
        while (cur != null) {
            if (cur.data.getBarcode().equals(barcode)) return cur.data;
            cur = cur.next;
        }
        return null;
    }

    public Product searchByName(String name) {
        Node cur = head;
        while (cur != null) {
            if (cur.data.getName().equalsIgnoreCase(name)) return cur.data;
            cur = cur.next;
        }
        return null;
    }

    public void forEach(Consumer<Product> action) {
        Node cur = head;
        while (cur != null) { action.accept(cur.data); cur = cur.next; }
    }

    public LinkedList filter(Predicate<Product> pred) {
        LinkedList result = new LinkedList();
        Node cur = head;
        while (cur != null) {
            if (pred.test(cur.data)) result.insert(cur.data);
            cur = cur.next;
        }
        return result;
    }

    public int     size()  { return size; }
    public boolean isEmpty(){ return head == null; }
    public Node    getHead(){ return head; }

    public boolean update(Product updated) {
        Node cur = head;
        while (cur != null) {
            if (cur.data.getBarcode().equals(updated.getBarcode())) {
                cur.data = updated;
                return true;
            }
            cur = cur.next;
        }
        return false;
    }
}
