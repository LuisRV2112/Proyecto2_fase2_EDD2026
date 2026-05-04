package edu.usac.edd.structures;

import edu.usac.edd.model.Product;
import java.util.function.Consumer;

public class HashTable {

    private static final int DEFAULT_CAP = 2048;

    public static class Entry {
        public Product data;
        public Entry   next;
        Entry(Product p) { this.data = p; }
    }

    private final Entry[] table;
    private final int     capacity;
    private int           count;

    public HashTable()              { this(DEFAULT_CAP); }
    public HashTable(int capacity)  {
        this.capacity = capacity;
        this.table    = new Entry[capacity];
    }

    private int hash(String key) {
        long h = 5381;
        for (char c : key.toCharArray())
            h = ((h << 5) + h) + c;
        return (int)(Math.abs(h) % capacity);
    }

    public boolean insert(Product p) {
        int idx = hash(p.getBarcode());
        Entry cur = table[idx];
        while (cur != null) {
            if (cur.data.getBarcode().equals(p.getBarcode())) return false;
            cur = cur.next;
        }
        Entry e = new Entry(p);
        e.next    = table[idx];
        table[idx] = e;
        count++;
        return true;
    }

    public Product search(String barcode) {
        int idx = hash(barcode);
        Entry cur = table[idx];
        while (cur != null) {
            if (cur.data.getBarcode().equals(barcode)) return cur.data;
            cur = cur.next;
        }
        return null;
    }

    public boolean remove(String barcode) {
        int idx = hash(barcode);
        Entry prev = null, cur = table[idx];
        while (cur != null) {
            if (cur.data.getBarcode().equals(barcode)) {
                if (prev != null) prev.next = cur.next;
                else              table[idx] = cur.next;
                count--;
                return true;
            }
            prev = cur; cur = cur.next;
        }
        return false;
    }

    public void forEach(Consumer<Product> action) {
        for (int i = 0; i < capacity; i++) {
            Entry cur = table[i];
            while (cur != null) {
                action.accept(cur.data);
                cur = cur.next;
            }
        }
    }

    public int    size()       { return count; }
    public boolean isEmpty()   { return count == 0; }
    public double loadFactor() { return (double) count / capacity; }
    public int    capacity()   { return capacity; }

    public int chainLength(int idx) {
        int len = 0;
        Entry cur = table[idx];
        while (cur != null) { len++; cur = cur.next; }
        return len;
    }

    public Entry[] getTable() { return table; }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph HashTable {\n");
        sb.append("  graph [label=\"Tabla Hash (djb2, cap=").append(capacity).append(", n=").append(count).append(")\"];\n");
        sb.append("  node [shape=record, style=filled];\n");
        sb.append("  rankdir=LR;\n");

        
        for (int i = 0; i < capacity; i++) {
            if (table[i] == null) continue;
            
            sb.append("  bucket").append(i)
              .append(" [label=\"[").append(i).append("]\", fillcolor=lightblue, shape=box];\n");
            
            Entry cur = table[i];
            int pos = 0;
            String prev = "bucket" + i;
            while (cur != null) {
                String nodeId = "n" + i + "_" + pos;
                String name = cur.data.getName().replace("\"","'");
                String barcode = cur.data.getBarcode();
                sb.append("  ").append(nodeId)
                  .append(" [label=\"{").append(barcode).append("|").append(name).append("}\", fillcolor=")
                  .append(pos == 0 ? "lightyellow" : "lightsalmon").append("];\n");
                sb.append("  ").append(prev).append(" -> ").append(nodeId).append(";\n");
                prev = nodeId;
                cur = cur.next;
                pos++;
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
