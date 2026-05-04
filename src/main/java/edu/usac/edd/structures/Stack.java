package edu.usac.edd.structures;

import edu.usac.edd.model.Product;


public class Stack {

    public static class Entry {
        public enum Op { INSERT, DELETE, UPDATE }
        public final Op      operation;
        public final Product snapshot;
        public final String  branchId;

        public Entry(Op op, Product snapshot, String branchId) {
            this.operation = op;
            this.snapshot  = snapshot;
            this.branchId  = branchId;
        }
    }

    private static class Node {
        Entry data;
        Node  next;
        Node(Entry e) { this.data = e; }
    }

    private Node top;
    private int  size;
    private final int maxSize;

    public Stack(int maxSize) { this.maxSize = maxSize; }
    public Stack()            { this(200); }

    public void push(Entry entry) {
        if (size >= maxSize) removeBottom();
        Node n = new Node(entry);
        n.next = top;
        top    = n;
        size++;
    }

    public Entry pop() {
        if (top == null) return null;
        Entry e = top.data;
        top = top.next;
        size--;
        return e;
    }

    public Entry peek() {
        return top != null ? top.data : null;
    }

    public boolean isEmpty() { return top == null; }
    public int     size()    { return size; }

    private void removeBottom() {
        if (top == null) return;
        if (top.next == null) { top = null; size--; return; }
        Node cur = top;
        while (cur.next.next != null) cur = cur.next;
        cur.next = null;
        size--;
    }
}
