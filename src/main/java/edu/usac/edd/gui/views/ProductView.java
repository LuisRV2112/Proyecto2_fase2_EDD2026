package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.catalog.Catalog;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ProductView {

    private final BranchManager   manager;
    private ComboBox<String>      cbBranch;
    private TableView<Product>    table;
    private ObservableList<Product> data;
    private TextField tfName, tfBarcode, tfCategory, tfExpiry, tfBrand, tfPrice, tfStock;
    private TextField tfSearchName, tfSearchBarcode, tfSearchCat, tfFrom, tfTo;
    private Label     lblStatus;

    public ProductView(BranchManager manager) { this.manager = manager; }

    public Node build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#1e1e2e;");

        // Selector de sucursal (top bar)
        HBox topBar = buildTopBar();
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.60);
        split.getItems().addAll(buildTablePanel(), buildSidePanel());
        VBox.setVgrow(split, Priority.ALWAYS);
        root.getChildren().addAll(topBar, split);
        return root;
    }

    // ── Top bar: selector sucursal + búsquedas ────────────────────────────
    private HBox buildTopBar() {
        Label lbl = new Label("Sucursal:");
        lbl.setStyle("-fx-text-fill:#cdd6f4;");
        cbBranch = new ComboBox<>();
        refreshBranchCombo();
        cbBranch.setOnAction(e -> refreshTable());
        cbBranch.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4;");

        Button btnRefresh = btn("Refrescar", "#89b4fa");
        btnRefresh.setOnAction(e -> { refreshBranchCombo(); refreshTable(); });

        // Búsquedas rápidas
        tfSearchName    = miniField("Buscar por nombre");
        tfSearchBarcode = miniField("Buscar por código");
        tfSearchCat     = miniField("Buscar por categoría");
        tfFrom = miniField("Desde (YYYY-MM-DD)");
        tfTo   = miniField("Hasta (YYYY-MM-DD)");

        Button btnByName = btn("🔍 Nombre",    "#cba6f7");
        Button btnByCode = btn("🔍 Código",    "#89dceb");
        Button btnByCat  = btn("🔍 Categoría", "#fab387");
        Button btnRange  = btn("🔍 Rango",     "#a6e3a1");

        btnByName.setOnAction(e -> searchByName());
        btnByCode.setOnAction(e -> searchByBarcode());
        btnByCat .setOnAction(e -> searchByCategory());
        btnRange .setOnAction(e -> searchByRange());

        HBox bar = new HBox(6,
            lbl, cbBranch, btnRefresh,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            tfSearchName, btnByName,
            tfSearchBarcode, btnByCode,
            tfSearchCat, btnByCat,
            tfFrom, tfTo, btnRange
        );
        bar.setPadding(new Insets(8));
        bar.setStyle("-fx-background-color:#181825;");
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return bar;
    }

    // ── Tabla de productos ────────────────────────────────────────────────
    private Node buildTablePanel() {
        table = new TableView<>();
        table.setStyle("-fx-background-color:#1e1e2e;");
        data = FXCollections.observableArrayList();
        table.setItems(data);

        table.getColumns().addAll(
            tcol("Código",    "barcode",    110),
            tcol("Nombre",    "name",       200),
            tcol("Categoría", "category",   110),
            tcol("Caducidad", "expiryDate", 100),
            tcol("Marca",     "brand",      100),
            tcol("Precio",    "price",       80),
            tcol("Stock",     "stock",       60),
            tcol("Estado",    "status",      90)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Selección rellena formulario
        table.getSelectionModel().selectedItemProperty()
             .addListener((o, old, sel) -> { if (sel != null) fillForm(sel); });

        lblStatus = new Label("Seleccione una sucursal");
        lblStatus.setStyle("-fx-text-fill:#a6adc8; -fx-padding:4 8;");

        VBox box = new VBox(4, lblStatus, table);
        box.setStyle("-fx-background-color:#1e1e2e;");
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ── Panel lateral: formulario + acciones ──────────────────────────────
    private Node buildSidePanel() {
        VBox side = new VBox(8);
        side.setPadding(new Insets(12));
        side.setStyle("-fx-background-color:#181825;");

        Label title = new Label("📦 Datos del Producto");
        title.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:14px; -fx-font-weight:bold;");

        tfName     = field("Nombre");
        tfBarcode  = field("Código de barra");
        tfCategory = field("Categoría");
        tfExpiry   = field("Caducidad (YYYY-MM-DD)");
        tfBrand    = field("Marca");
        tfPrice    = field("Precio");
        tfStock    = field("Stock");

        Button btnAdd  = btn("➕ Agregar",  "#a6e3a1");
        Button btnDel  = btn("🗑 Eliminar", "#f38ba8");
        Button btnUndo = btn("↩ Deshacer", "#fab387");
        Button btnClear= btn("🧹 Limpiar",  "#a6adc8");

        btnAdd.setOnAction(e  -> onAdd());
        btnDel.setOnAction(e  -> onDelete());
        btnUndo.setOnAction(e -> onUndo());
        btnClear.setOnAction(e-> clearForm());

        HBox row1 = new HBox(6, btnAdd, btnDel);
        HBox row2 = new HBox(6, btnUndo, btnClear);

        side.getChildren().addAll(title,
            lbl("Nombre:"),    tfName,
            lbl("Código:"),    tfBarcode,
            lbl("Categoría:"), tfCategory,
            lbl("Caducidad:"), tfExpiry,
            lbl("Marca:"),     tfBrand,
            lbl("Precio:"),    tfPrice,
            lbl("Stock:"),     tfStock,
            row1, row2
        );
        return side;
    }

    // ── Acciones ──────────────────────────────────────────────────────────
    private void onAdd() {
        String branchId = selectedBranch();
        if (branchId == null) { alert("Selecciona una sucursal."); return; }
        try {
            Product p = fromForm();
            if (!manager.addProduct(branchId, p))
                alert("No se pudo agregar (barcode duplicado o sucursal inexistente).");
            else { refreshTable(); clearForm(); status("Producto agregado ✓"); }
        } catch (Exception ex) { alert("Error: " + ex.getMessage()); }
    }

    private void onDelete() {
        String branchId = selectedBranch();
        Product sel = table.getSelectionModel().getSelectedItem();
        if (branchId == null || sel == null) { alert("Selecciona sucursal y producto."); return; }
        if (manager.removeProduct(branchId, sel.getBarcode())) {
            refreshTable(); status("Producto eliminado ✓");
        } else alert("No se encontró el producto.");
    }

    private void onUndo() {
        String branchId = selectedBranch();
        if (branchId == null) { alert("Selecciona una sucursal."); return; }
        if (manager.undoLastOperation(branchId)) {
            refreshTable(); status("Operación deshecha ✓");
        } else alert("No hay operaciones para deshacer.");
    }

    // ── Búsquedas ─────────────────────────────────────────────────────────
    private void searchByName() {
        String branchId = selectedBranch(); if (branchId == null) return;
        String name = tfSearchName.getText().trim(); if (name.isEmpty()) return;
        Catalog cat = manager.getCatalog(branchId); if (cat == null) return;
        Product p = cat.searchByName(name);
        if (p != null) { data.setAll(p); status("Encontrado: " + p.getName()); }
        else           { data.clear();   status("No encontrado: " + name); }
    }

    private void searchByBarcode() {
        String branchId = selectedBranch(); if (branchId == null) return;
        String code = tfSearchBarcode.getText().trim(); if (code.isEmpty()) return;
        Catalog cat = manager.getCatalog(branchId); if (cat == null) return;
        Product p = cat.searchByBarcode(code);
        if (p != null) { data.setAll(p); status("Encontrado: " + p.getBarcode()); }
        else           { data.clear();   status("No encontrado: " + code); }
    }

    private void searchByCategory() {
        String branchId = selectedBranch(); if (branchId == null) return;
        String cat_ = tfSearchCat.getText().trim(); if (cat_.isEmpty()) return;
        Catalog cat = manager.getCatalog(branchId); if (cat == null) return;
        List<Product> results = cat.searchByCategory(cat_);
        data.setAll(results);
        status("Categoría '" + cat_ + "': " + results.size() + " productos");
    }

    private void searchByRange() {
        String branchId = selectedBranch(); if (branchId == null) return;
        String from = tfFrom.getText().trim(), to = tfTo.getText().trim();
        if (from.isEmpty() || to.isEmpty()) { alert("Ingresa ambas fechas."); return; }
        Catalog cat = manager.getCatalog(branchId); if (cat == null) return;
        List<Product> results = cat.searchByExpiryRange(from, to);
        data.setAll(results);
        status("Rango [" + from + " → " + to + "]: " + results.size() + " productos");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String selectedBranch() {
        return cbBranch.getValue();
    }

    private void refreshBranchCombo() {
        String prev = cbBranch.getValue();
        cbBranch.getItems().setAll(
            manager.getAllBranches().stream()
                   .map(Branch::getId).toList()
        );
        if (prev != null && cbBranch.getItems().contains(prev))
            cbBranch.setValue(prev);
        else if (!cbBranch.getItems().isEmpty())
            cbBranch.setValue(cbBranch.getItems().get(0));
    }

    private void refreshTable() {
        String branchId = selectedBranch();
        if (branchId == null) { data.clear(); return; }
        Catalog cat = manager.getCatalog(branchId);
        if (cat == null) { data.clear(); return; }
        data.setAll(cat.allProducts());
        status(branchId + ": " + cat.size() + " productos");
    }

    private Product fromForm() throws Exception {
        String name    = tfName.getText().trim();
        String barcode = tfBarcode.getText().trim();
        String cat     = tfCategory.getText().trim();
        String expiry  = tfExpiry.getText().trim();
        String brand   = tfBrand.getText().trim();
        double price   = Double.parseDouble(tfPrice.getText().trim());
        int    stock   = Integer.parseInt(tfStock.getText().trim());
        if (name.isEmpty() || barcode.isEmpty())
            throw new IllegalArgumentException("Nombre y código obligatorios");
        return new Product(name, barcode, cat, expiry, brand, price, stock);
    }

    private void fillForm(Product p) {
        tfName.setText(p.getName());      tfBarcode.setText(p.getBarcode());
        tfCategory.setText(p.getCategory()); tfExpiry.setText(p.getExpiryDate());
        tfBrand.setText(p.getBrand());
        tfPrice.setText(String.valueOf(p.getPrice()));
        tfStock.setText(String.valueOf(p.getStock()));
    }

    private void clearForm() {
        tfName.clear(); tfBarcode.clear(); tfCategory.clear();
        tfExpiry.clear(); tfBrand.clear(); tfPrice.clear(); tfStock.clear();
    }

    private void status(String msg) { lblStatus.setText(msg); }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4; -fx-prompt-text-fill:#585b70;");
        return tf;
    }
    private TextField miniField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); tf.setPrefWidth(150);
        tf.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4; -fx-prompt-text-fill:#585b70;");
        return tf;
    }
    private Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:11px;"); return l;
    }
    private Button btn(String t, String c) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + c + "; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        return b;
    }
    @SuppressWarnings("unchecked")
    private <T> TableColumn<Product, T> tcol(String title, String prop, int w) {
        TableColumn<Product,T> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }
    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
