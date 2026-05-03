package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Transfer;
import edu.usac.edd.simulation.Dispatcher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Panel de transferencias entre sucursales.
 * Muestra la ruta Dijkstra, el ETA y el estado de las colas en tiempo real.
 */
public class TransferView {

    private final BranchManager manager;
    private ComboBox<String>     cbFrom, cbTo;
    private ComboBox<String>     cbCriterion;
    private TextField            tfBarcode;
    private TextArea             taRoute, taLog;
    private Label                lblETA, lblCost;
    private TableView<Transfer>  tableTransfers;
    private ObservableList<Transfer> transferData;

    // Colas visuales por sucursal
    private VBox                 queuePanel;

    public TransferView(BranchManager manager) { this.manager = manager; }

    public Node build() {
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.45);
        split.setStyle("-fx-background-color:#1e1e2e;");
        split.getItems().addAll(buildLeft(), buildRight());
        return split;
    }

    // ── Panel izquierdo: formulario de transferencia ──────────────────────
    private Node buildLeft() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:#181825;");

        Label title = new Label("🚚 Nueva Transferencia");
        title.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:15px; -fx-font-weight:bold;");

        cbFrom = combo(); cbTo = combo();
        cbCriterion = new ComboBox<>(FXCollections.observableArrayList("Tiempo mínimo","Costo mínimo"));
        cbCriterion.setValue("Tiempo mínimo");
        styleCombo(cbCriterion);
        tfBarcode = field("Código de barra del producto");

        Button btnRefresh = btn("🔄 Actualizar sucursales", "#89b4fa");
        btnRefresh.setOnAction(e -> refreshCombos());

        Button btnPreview = btn("👁 Ver Ruta", "#cba6f7");
        btnPreview.setOnAction(e -> previewRoute());

        Button btnSend = btn("🚀 Iniciar Transferencia", "#a6e3a1");
        btnSend.setStyle("-fx-background-color:#a6e3a1; -fx-text-fill:#1e1e2e; " +
                         "-fx-font-weight:bold; -fx-font-size:13px;");
        btnSend.setOnAction(e -> initiateTransfer());

        taRoute = new TextArea();
        taRoute.setEditable(false);
        taRoute.setPrefHeight(130);
        taRoute.setStyle("-fx-control-inner-background:#1e1e2e; -fx-text-fill:#89dceb; " +
                         "-fx-font-family:monospace;");

        lblETA  = infoLabel("ETA: —");
        lblCost = infoLabel("Costo: —");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.addRow(0, lbl("Origen:"),   cbFrom);
        grid.addRow(1, lbl("Destino:"),  cbTo);
        grid.addRow(2, lbl("Criterio:"), cbCriterion);
        grid.addRow(3, lbl("Barcode:"),  tfBarcode);

        HBox btnRow = new HBox(8, btnPreview, btnSend);

        box.getChildren().addAll(title, btnRefresh, grid,
                new Separator(), taRoute, lblETA, lblCost,
                new Separator(), btnRow);

        refreshCombos();
        return box;
    }

    // ── Panel derecho: historial + colas en tiempo real ───────────────────
    private Node buildRight() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color:#1e1e2e;");

        // Tabla de transferencias
        Label tTitle = new Label("📋 Historial de Transferencias");
        tTitle.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:13px; -fx-font-weight:bold;");

        tableTransfers = new TableView<>();
        transferData   = FXCollections.observableArrayList();
        tableTransfers.setItems(transferData);
        tableTransfers.setStyle("-fx-background-color:#1e1e2e;");
        tableTransfers.setPrefHeight(200);

        TableColumn<Transfer,String> cOrig = tcol("Origen",  100, t -> t.getOriginId());
        TableColumn<Transfer,String> cDest = tcol("Destino", 100, t -> t.getDestId());
        TableColumn<Transfer,String> cProd = tcol("Producto",160, t -> t.getProduct().getName());
        TableColumn<Transfer,String> cETA  = tcol("ETA",      90, t -> t.etaFormatted());
        TableColumn<Transfer,String> cPhase= tcol("Estado",  100, t -> t.getPhase().toString());
        tableTransfers.getColumns().addAll(cOrig, cDest, cProd, cETA, cPhase);
        tableTransfers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Colas en tiempo real
        Label qTitle = new Label("📦 Estado de Colas (tiempo real)");
        qTitle.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:13px; -fx-font-weight:bold;");

        queuePanel = new VBox(6);
        queuePanel.setStyle("-fx-background-color:#181825; -fx-padding:8;");
        ScrollPane qScroll = new ScrollPane(queuePanel);
        qScroll.setFitToWidth(true);
        qScroll.setStyle("-fx-background-color:#181825;");
        VBox.setVgrow(qScroll, Priority.ALWAYS);

        // Log de eventos
        Label lTitle = new Label("📜 Log de simulación");
        lTitle.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:13px; -fx-font-weight:bold;");
        taLog = new TextArea();
        taLog.setEditable(false);
        taLog.setPrefHeight(150);
        taLog.setStyle("-fx-control-inner-background:#181825; -fx-text-fill:#a6e3a1; " +
                       "-fx-font-family:monospace; -fx-font-size:11px;");

        box.getChildren().addAll(tTitle, tableTransfers, qTitle, qScroll, lTitle, taLog);
        return box;
    }

    // ── Lógica ────────────────────────────────────────────────────────────
    private void previewRoute() {
        String from = cbFrom.getValue(), to = cbTo.getValue();
        if (from == null || to == null) { alert("Selecciona origen y destino."); return; }
        Transfer.Criterion criterion = criterion();
        List<String> route = manager.getGraph().dijkstra(from, to, criterion);
        if (route.isEmpty()) {
            taRoute.setText("❌ No existe ruta entre " + from + " y " + to);
            lblETA.setText("ETA: —"); lblCost.setText("Costo: —"); return;
        }
        double[] costs = manager.getGraph().routeCost(route);
        StringBuilder sb = new StringBuilder("🗺 Ruta óptima:\n");
        for (int i = 0; i < route.size(); i++) {
            Branch b = manager.getBranch(route.get(i));
            String name = b != null ? b.getName() : route.get(i);
            sb.append("  ").append(i+1).append(". ").append(name)
              .append(" [").append(route.get(i)).append("]");
            if (i < route.size()-1) sb.append(" →\n");
        }
        taRoute.setText(sb.toString());
        lblETA.setText(String.format("⏱ ETA: %.0f s (%.1f min)", costs[0], costs[0]/60));
        lblCost.setText(String.format("💰 Costo: Q%.2f", costs[1]));
    }

    private void initiateTransfer() {
        String from    = cbFrom.getValue();
        String to      = cbTo.getValue();
        String barcode = tfBarcode.getText().trim();
        if (from == null || to == null || barcode.isEmpty()) {
            alert("Completa todos los campos."); return;
        }
        Transfer.Criterion criterion = criterion();
        Transfer t = manager.initiateTransfer(barcode, from, to, criterion);
        if (t == null) {
            alert("No se pudo iniciar la transferencia.\n" +
                  "Verifica que el producto exista en la sucursal origen y que haya ruta.");
            return;
        }
        transferData.setAll(manager.getTransfers());
        log("▶ Transferencia iniciada: " + t);

        // Programar actualizaciones periódicas de la UI
        manager.getDispatcher().setOnUpdate(() ->
            Platform.runLater(this::refreshQueues)
        );
    }

    public void refreshQueues() {
        queuePanel.getChildren().clear();
        transferData.setAll(manager.getTransfers());

        // Mostrar log de simulación
        List<Dispatcher.SimulationEvent> events = manager.getDispatcher().getLog();
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, events.size() - 20);
        for (int i = start; i < events.size(); i++)
            sb.append(events.get(i)).append("\n");
        taLog.setText(sb.toString());
        taLog.setScrollTop(Double.MAX_VALUE);

        // Mostrar colas por sucursal
        for (var entry : manager.getDispatcher().getAllQueues().entrySet()) {
            String branchId = entry.getKey();
            Dispatcher.BranchQueues bq = entry.getValue();
            Branch b = manager.getBranch(branchId);
            String branchName = b != null ? b.getName() : branchId;

            VBox branchBox = new VBox(4);
            branchBox.setStyle("-fx-background-color:#313244; -fx-padding:6; -fx-background-radius:6;");
            Label bLabel = new Label("🏪 " + branchName);
            bLabel.setStyle("-fx-text-fill:#cdd6f4; -fx-font-weight:bold;");

            HBox queues = new HBox(8,
                queueVBox("📥 Ingreso",  bq.ingreso.size()),
                queueVBox("🔄 Traspaso", bq.traspaso.size()),
                queueVBox("📤 Salida",   bq.salida.size())
            );
            branchBox.getChildren().addAll(bLabel, queues);
            queuePanel.getChildren().add(branchBox);
        }
    }

    private VBox queueVBox(String label, int count) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:#45475a; -fx-padding:6 12; -fx-background-radius:4;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:10px;");
        Label num = new Label(String.valueOf(count));
        num.setStyle("-fx-text-fill:" + (count > 0 ? "#f9e2af" : "#585b70") +
                     "; -fx-font-size:18px; -fx-font-weight:bold;");
        box.getChildren().addAll(lbl, num);
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void refreshCombos() {
        List<String> ids = manager.getAllBranches().stream()
                                  .map(Branch::getId).toList();
        cbFrom.setItems(FXCollections.observableArrayList(ids));
        cbTo.setItems(FXCollections.observableArrayList(ids));
        if (!ids.isEmpty()) {
            cbFrom.setValue(ids.get(0));
            cbTo.setValue(ids.size() > 1 ? ids.get(1) : ids.get(0));
        }
    }

    private Transfer.Criterion criterion() {
        return "Costo mínimo".equals(cbCriterion.getValue())
               ? Transfer.Criterion.COST : Transfer.Criterion.TIME;
    }

    private void log(String msg) {
        Platform.runLater(() -> taLog.appendText(msg + "\n"));
    }

    private ComboBox<String> combo() {
        ComboBox<String> cb = new ComboBox<>();
        styleCombo(cb); return cb;
    }
    private void styleCombo(ComboBox<?> cb) {
        cb.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4;");
        cb.setMaxWidth(Double.MAX_VALUE);
    }
    private TextField field(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4; -fx-prompt-text-fill:#585b70;");
        return tf;
    }
    private Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill:#a6adc8;"); return l;
    }
    private Label infoLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:#89dceb; -fx-font-size:13px; -fx-font-weight:bold;");
        return l;
    }
    private Button btn(String t, String c) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + c + "; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }
    private <T> TableColumn<Transfer, T> tcol(String title, int w,
            java.util.function.Function<Transfer, T> getter) {
        TableColumn<Transfer, T> c = new TableColumn<>(title);
        c.setCellValueFactory(f -> new javafx.beans.property.SimpleObjectProperty<>(
                getter.apply(f.getValue())));
        c.setPrefWidth(w);
        return c;
    }
    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
