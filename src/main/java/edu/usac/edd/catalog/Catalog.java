package edu.usac.edd.catalog;

import edu.usac.edd.model.Product;
import edu.usac.edd.structures.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Catálogo de una sucursal.
 * Coordina AVL + Hash + BTree + BPlusTree + LinkedList de forma coherente.
 * Inserción atomizada con rollback.
 */
public class Catalog {

    private final LinkedList unsortedList = new LinkedList();
    private final AVLTree    avlTree      = new AVLTree();
    private final HashTable  hashTable    = new HashTable();
    private final BTree      bTree        = new BTree();
    private final BPlusTree  bPlusTree    = new BPlusTree();
    private final Stack      undoStack    = new Stack();
    private final String     branchId;

    public Catalog(String branchId) { this.branchId = branchId; }

    // ── Inserción atomizada con rollback ──────────────────────────────────
    public boolean addProduct(Product p) {
        if (hashTable.search(p.getBarcode()) != null) return false; // duplicado

        unsortedList.insert(p);
        if (!avlTree.insert(p)) {
            unsortedList.remove(p.getBarcode());
            return false;
        }
        if (!hashTable.insert(p)) {
            unsortedList.remove(p.getBarcode());
            avlTree.remove(p.getName(), p.getBarcode());
            return false;
        }
        bTree.insert(p);
        bPlusTree.insert(p);

        undoStack.push(new Stack.Entry(Stack.Entry.Op.INSERT, p.copy(), branchId));
        return true;
    }

    // ── Eliminación propagada ─────────────────────────────────────────────
    public boolean removeProduct(String barcode) {
        Product p = hashTable.search(barcode);
        if (p == null) return false;
        Product snap = p.copy();

        unsortedList.remove(barcode);
        avlTree.remove(p.getName(), barcode);
        hashTable.remove(barcode);
        bTree.remove(p.getExpiryDate(), barcode);
        bPlusTree.remove(p.getCategory(), barcode);

        undoStack.push(new Stack.Entry(Stack.Entry.Op.DELETE, snap, branchId));
        return true;
    }

    // ── Undo (rollback de la última operación) ────────────────────────────
    public boolean undo() {
        Stack.Entry entry = undoStack.pop();
        if (entry == null) return false;
        if (entry.operation == Stack.Entry.Op.INSERT)
            return removeProduct(entry.snapshot.getBarcode());
        if (entry.operation == Stack.Entry.Op.DELETE)
            return addProduct(entry.snapshot);
        return false;
    }

    // ── Búsquedas ─────────────────────────────────────────────────────────
    public Product searchByName(String name)       { return avlTree.search(name); }
    public Product searchByBarcode(String barcode) { return hashTable.search(barcode); }

    public List<Product> searchByCategory(String category) {
        List<Product> result = new ArrayList<>();
        bPlusTree.searchByCategory(category, result::add);
        return result;
    }

    public List<Product> searchByExpiryRange(String from, String to) {
        List<Product> result = new ArrayList<>();
        bTree.rangeSearch(from, to, result::add);
        return result;
    }

    // ── Listado ordenado por nombre (AVL in-order) ────────────────────────
    public void listByName(Consumer<Product> action) { avlTree.inOrder(action); }

    // ── Todos los productos ───────────────────────────────────────────────
    public List<Product> allProducts() {
        List<Product> list = new ArrayList<>();
        unsortedList.forEach(list::add);
        return list;
    }

    // ── Acceso a estructuras para benchmark y visualización ───────────────
    public LinkedList getUnsortedList() { return unsortedList; }
    public AVLTree    getAVL()          { return avlTree; }
    public HashTable  getHash()         { return hashTable; }
    public BTree      getBTree()        { return bTree; }
    public BPlusTree  getBPlusTree()    { return bPlusTree; }
    public Stack      getUndoStack()    { return undoStack; }

    public int  size()        { return hashTable.size(); }
    public boolean isEmpty()  { return hashTable.isEmpty(); }
    public String getBranchId(){ return branchId; }
}
