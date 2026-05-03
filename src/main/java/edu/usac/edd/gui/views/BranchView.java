package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.graph.BranchGraph;
import edu.usac.edd.model.Branch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * CRUD de sucursales y conexiones.
 */
public class BranchView {

    private final BranchManager manager;
    private TableView<Branch>            table;
    private ObservableList<Branch>       data;

    // Campos del formulario sucursal
    private TextField tfId, tfName, tfLocation, tfIngreso, tfTraspaso, tfDespacho;

    // Campos del formulario conexión
    private ComboBox<String> cbFrom, cbTo;
    private TextField tfTime, tfCost;
    private CheckBox  cbBidir;
    private TableView<BranchGraph.Edge> edgeTable;
    private ObservableList<BranchGraph.Edge> edgeData;

    public BranchView(BranchManager manager) { this.manager = manager; }

    public Node build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#1e1e2e;");

        
        SplitPane topSplit = new SplitPane();
        topSplit.setDividerPositions(0.55);
        topSplit.setStyle("-fx-background-color:#1e1e2e;");
        topSplit.getItems().addAll(buildTable(), buildForm());
        topSplit.setPrefHeight(340);

        
        TitledPane connPane = buildConnectionPanel();
        connPane.setStyle("-fx-text-fill:#cdd6f4;");

        VBox.setVgrow(topSplit, Priority.ALWAYS);
        root.getChildren().addAll(topSplit, connPane);
        return root;
    }

    // ── Tabla de sucursales ───────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Node buildTable() {
        table = new TableView<>();
        table.setStyle("-fx-background-color:#1e1e2e; -fx-text-fill:#cdd6f4;");
        data  = FXCollections.observableArrayList(manager.getAllBranches());
        table.setItems(data);

        TableColumn<Branch,String>  cId  = col("ID",          "id",           80);
        TableColumn<Branch,String>  cNm  = col("Nombre",      "name",         160);
        TableColumn<Branch,String>  cLoc = col("Ubicación",   "location",     160);
        TableColumn<Branch,Integer> cIn  = colInt("T.Ingreso", "timeIngreso",  90);
        TableColumn<Branch,Integer> cTr  = colInt("T.Traspaso","timeTraspaso", 90);
        TableColumn<Branch,Integer> cDe  = colInt("T.Despacho","timeDespacho", 90);

        table.getColumns().addAll(cId, cNm, cLoc, cIn, cTr, cDe);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) fillForm(sel);
        });

        Button btnRefresh = btn(" Refrescar", "#89b4fa");
        btnRefresh.setOnAction(e -> refresh());

        HBox header = new HBox(8,
            new Label("   Sucursales registradas") {{
                setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:14px; -fx-padding:8 0 0 8;");
            }},
            btnRefresh
        );
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#1e1e2e;");

        VBox box = new VBox(8, header, table);
        box.setStyle("-fx-background-color:#1e1e2e;");
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ── Formulario sucursal ───────────────────────────────────────────────
    private Node buildForm() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color:#181825;");

        Label title = new Label("✏️ Gestionar Sucursal");
        title.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:15px; -fx-font-weight:bold;");

        tfId       = field("ID (ej: S01)");
        tfName     = field("Nombre");
        tfLocation = field("Ubicación");
        tfIngreso  = field("Tiempo de ingreso (seg)");
        tfTraspaso = field("Tiempo de traspaso (seg)");
        tfDespacho = field("Intervalo de despacho (seg)");

        Button btnAdd  = btn("➕ Agregar",   "#a6e3a1");
        Button btnEdit = btn("✏️ Actualizar", "#89b4fa");
        Button btnDel  = btn("🗑 Eliminar",   "#f38ba8");

        btnAdd.setOnAction(e  -> onAdd());
        btnEdit.setOnAction(e -> onEdit());
        btnDel.setOnAction(e  -> onDelete());

        HBox buttons = new HBox(8, btnAdd, btnEdit, btnDel);

        form.getChildren().addAll(title,
            lbl("ID:"),        tfId,
            lbl("Nombre:"),    tfName,
            lbl("Ubicación:"), tfLocation,
            lbl("T. Ingreso (s):"),  tfIngreso,
            lbl("T. Traspaso (s):"), tfTraspaso,
            lbl("T. Despacho (s):"), tfDespacho,
            buttons
        );
        return form;
    }

    // ── Panel de conexiones ───────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private TitledPane buildConnectionPanel() {
        HBox content = new HBox(16);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color:#181825;");

        
        VBox formConn = new VBox(8);
        formConn.setMinWidth(300);
        formConn.setStyle("-fx-background-color:#1e1e2e; -fx-padding:10; -fx-background-radius:6;");

        Label lTitle = new Label(" Nueva Conexión");
        lTitle.setStyle("-fx-text-fill:#cdd6f4; -fx-font-weight:bold; -fx-font-size:13px;");

        cbFrom = new ComboBox<>(); styleCombo(cbFrom);
        cbTo   = new ComboBox<>(); styleCombo(cbTo);
        tfTime = field("Tiempo (seg)");
        tfCost = field("Costo (Q)");
        cbBidir = new CheckBox("Bidireccional");
        cbBidir.setStyle("-fx-text-fill:#cdd6f4;");
        cbBidir.setSelected(true);

        refreshConnCombos();

        Button btnAddConn = btn("➕ Agregar Conexión", "#a6e3a1");
        btnAddConn.setMaxWidth(Double.MAX_VALUE);
        btnAddConn.setOnAction(e -> onAddConnection());

        Button btnDelConn = btn("🗑 Eliminar Seleccionada", "#f38ba8");
        btnDelConn.setMaxWidth(Double.MAX_VALUE);
        btnDelConn.setOnAction(e -> onDeleteConnection());

        Button btnRefreshConn = btn(" Refrescar", "#89b4fa");
        btnRefreshConn.setMaxWidth(Double.MAX_VALUE);
        btnRefreshConn.setOnAction(e -> refreshConnections());

        formConn.getChildren().addAll(lTitle,
            lbl("Origen:"),  cbFrom,
            lbl("Destino:"), cbTo,
            lbl("Tiempo (s):"), tfTime,
            lbl("Costo (Q):"),  tfCost,
            cbBidir,
            btnAddConn, btnDelConn, btnRefreshConn
        );

        
        edgeTable = new TableView<>();
        edgeTable.setStyle("-fx-background-color:#1e1e2e;");
        edgeData  = FXCollections.observableArrayList(manager.getGraph().getEdges());
        edgeTable.setItems(edgeData);
        edgeTable.setPrefWidth(500);

        TableColumn<BranchGraph.Edge, String> cFrom  = edgeCol("Origen",         e -> e.fromId,          90);
        TableColumn<BranchGraph.Edge, String> cTo    = edgeCol("Destino",         e -> e.toId,            90);
        TableColumn<BranchGraph.Edge, String> cTime  = edgeCol("Tiempo (s)",      e -> String.format("%.0f", e.time),  90);
        TableColumn<BranchGraph.Edge, String> cCost  = edgeCol("Costo (Q)",       e -> String.format("%.2f", e.cost),  90);
        TableColumn<BranchGraph.Edge, String> cBidir = edgeCol("Bidireccional",   e -> e.bidirectional ? "Sí" : "No", 100);

        edgeTable.getColumns().addAll(cFrom, cTo, cTime, cCost, cBidir);
        edgeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox tableBox = new VBox(6);
        Label lEdges = new Label(" Conexiones registradas (" + edgeData.size() + ")");
        lEdges.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:12px;");
        tableBox.getChildren().addAll(lEdges, edgeTable);
        VBox.setVgrow(edgeTable, Priority.ALWAYS);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        content.getChildren().addAll(formConn, tableBox);

        TitledPane pane = new TitledPane(" Red de Conexiones entre Sucursales", content);
        pane.setCollapsible(true);
        pane.setExpanded(true);
        pane.setPrefHeight(280);
        return pane;
    }

    // ── Acciones sucursal ────────────────────────────────────────────────
    private void onAdd() {
        try {
            Branch b = fromForm();
            if (manager.getBranch(b.getId()) != null) {
                alert("El ID '" + b.getId() + "' ya existe."); return;
            }
            manager.addBranch(b);
            refresh();
            refreshConnCombos();
            clearForm();
            alert("Sucursal '" + b.getId() + "' agregada.\n" +
                  "Recuerda agregar conexiones en el panel de abajo para que aparezca en la Red.");
        } catch (Exception ex) { alert("Error: " + ex.getMessage()); }
    }

    private void onEdit() {
        Branch sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Selecciona una sucursal."); return; }
        try {
            Branch b = fromForm();
            sel.setName(b.getName());
            sel.setLocation(b.getLocation());
            sel.setTimeIngreso(b.getTimeIngreso());
            sel.setTimeTraspaso(b.getTimeTraspaso());
            sel.setTimeDespacho(b.getTimeDespacho());
            refresh();
        } catch (Exception ex) { alert("Error: " + ex.getMessage()); }
    }

    private void onDelete() {
        Branch sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Selecciona una sucursal."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar sucursal '" + sel.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                manager.removeBranch(sel.getId());
                refresh();
                refreshConnCombos();
                refreshConnections();
                clearForm();
            }
        });
    }

    // ── Acciones conexiones ──────────────────────────────────────────────
    private void onAddConnection() {
        String from = cbFrom.getValue();
        String to   = cbTo.getValue();
        if (from == null || to == null) { alert("Selecciona origen y destino."); return; }
        if (from.equals(to)) { alert("Origen y destino deben ser diferentes."); return; }
        try {
            double time = Double.parseDouble(tfTime.getText().trim());
            double cost = Double.parseDouble(tfCost.getText().trim());
            manager.addConnection(from, to, time, cost, cbBidir.isSelected());
            refreshConnections();
            tfTime.clear(); tfCost.clear();
        } catch (NumberFormatException ex) {
            alert("Tiempo y Costo deben ser números.");
        }
    }

    private void onDeleteConnection() {
        BranchGraph.Edge sel = edgeTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Selecciona una conexión."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar conexión " + sel.fromId + " → " + sel.toId + "?",
                ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                manager.getGraph().removeEdge(sel.fromId, sel.toId, sel.bidirectional);
                refreshConnections();
            }
        });
    }

    private void refreshConnections() {
        edgeData.setAll(manager.getGraph().getEdges());
        edgeTable.refresh();
    }

    private void refreshConnCombos() {
        String prevFrom = cbFrom != null ? cbFrom.getValue() : null;
        String prevTo   = cbTo   != null ? cbTo.getValue()   : null;
        java.util.List<String> ids = manager.getAllBranches().stream()
                .map(Branch::getId).toList();
        if (cbFrom != null) {
            cbFrom.setItems(FXCollections.observableArrayList(ids));
            if (prevFrom != null && ids.contains(prevFrom)) cbFrom.setValue(prevFrom);
            else if (!ids.isEmpty()) cbFrom.setValue(ids.get(0));
        }
        if (cbTo != null) {
            cbTo.setItems(FXCollections.observableArrayList(ids));
            if (prevTo != null && ids.contains(prevTo)) cbTo.setValue(prevTo);
            else if (ids.size() > 1) cbTo.setValue(ids.get(1));
            else if (!ids.isEmpty()) cbTo.setValue(ids.get(0));
        }
    }

    private Branch fromForm() {
        String id   = tfId.getText().trim();
        String name = tfName.getText().trim();
        String loc  = tfLocation.getText().trim();
        int ti = Integer.parseInt(tfIngreso.getText().trim());
        int tt = Integer.parseInt(tfTraspaso.getText().trim());
        int td = Integer.parseInt(tfDespacho.getText().trim());
        if (id.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("ID y Nombre obligatorios");
        return new Branch(id, name, loc, ti, tt, td);
    }

    private void fillForm(Branch b) {
        tfId.setText(b.getId());
        tfName.setText(b.getName());
        tfLocation.setText(b.getLocation());
        tfIngreso.setText(String.valueOf(b.getTimeIngreso()));
        tfTraspaso.setText(String.valueOf(b.getTimeTraspaso()));
        tfDespacho.setText(String.valueOf(b.getTimeDespacho()));
    }

    private void clearForm() {
        tfId.clear(); tfName.clear(); tfLocation.clear();
        tfIngreso.clear(); tfTraspaso.clear(); tfDespacho.clear();
    }

    public void refresh() {
        data.setAll(manager.getAllBranches());
        table.refresh();
        refreshConnCombos();
        refreshConnections();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4; -fx-prompt-text-fill:#585b70;");
        return tf;
    }
    private Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:11px;");
        return l;
    }
    private Button btn(String t, String color) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        b.setPrefWidth(140);
        return b;
    }
    private void styleCombo(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4;");
        cb.setMaxWidth(Double.MAX_VALUE);
    }
    private <T> TableColumn<Branch,T> col(String title, String prop, int w) {
        TableColumn<Branch,T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }
    private TableColumn<Branch,Integer> colInt(String title, String prop, int w) {
        return col(title, prop, w);
    }
    private TableColumn<BranchGraph.Edge, String> edgeCol(
            String title, java.util.function.Function<BranchGraph.Edge, String> getter, int w) {
        TableColumn<BranchGraph.Edge, String> c = new TableColumn<>(title);
        c.setCellValueFactory(f -> new javafx.beans.property.SimpleStringProperty(
                getter.apply(f.getValue())));
        c.setPrefWidth(w);
        return c;
    }
    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
