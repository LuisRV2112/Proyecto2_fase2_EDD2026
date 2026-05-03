package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.csv.CSVLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * Panel de inicio: estadísticas globales y carga de CSV.
 */
public class DashboardView {

    private final BranchManager manager;
    private final CSVLoader      csvLoader;
    private Label lblBranchCount, lblProductCount, lblTransferCount;
    private TextArea logArea;
    private Runnable onDataLoaded;

    public DashboardView(BranchManager manager, CSVLoader csvLoader) {
        this.manager   = manager;
        this.csvLoader = csvLoader;
    }

    /** Callback que se invoca luego de cualquier carga CSV exitosa */
    public void setOnDataLoaded(Runnable r) { this.onDataLoaded = r; }

    public Node build() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color:#1e1e2e;");

        // Stat cards
        lblBranchCount   = new Label("0");
        lblProductCount  = new Label("0");
        lblTransferCount = new Label("0");
        HBox stats = new HBox(16,
            statCard("🏪 Sucursales",     lblBranchCount),
            statCard("📦 Productos",      lblProductCount),
            statCard("🚚 Transferencias", lblTransferCount)
        );
        stats.setAlignment(Pos.CENTER);

        // CSV loader panel
        TitledPane csvPane = buildCSVPane();

        // Log
        logArea = new TextArea("Sistema iniciado.\n");
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background:#181825; -fx-text-fill:#a6e3a1; -fx-font-family:monospace;");
        logArea.setPrefHeight(200);
        TitledPane logPane = new TitledPane("📋 Log del sistema", logArea);
        logPane.setCollapsible(false);
        logPane.setStyle("-fx-text-fill:#cdd6f4;");

        root.getChildren().addAll(stats, csvPane, logPane);
        return root;
    }

    private VBox statCard(String title, Label valueLabel) {
        valueLabel.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:28px; -fx-font-weight:bold;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:13px;");
        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 50, 20, 50));
        card.setStyle("-fx-background-color:#313244; -fx-background-radius:10;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private TitledPane buildCSVPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(12));

        TextField tfBranch = field("ruta/sucursales.csv");
        TextField tfConn   = field("ruta/conexiones.csv");
        TextField tfProd   = field("ruta/productos.csv");
        tfBranch.setPrefWidth(360); tfConn.setPrefWidth(360); tfProd.setPrefWidth(360);

        Button btnB = pickBtn("📂", tfBranch);
        Button btnC = pickBtn("📂", tfConn);
        Button btnP = pickBtn("📂", tfProd);

        Button btnLoadB = actionBtn("Cargar Sucursales", "#89b4fa");
        Button btnLoadC = actionBtn("Cargar Conexiones", "#89b4fa");
        Button btnLoadP = actionBtn("Cargar Productos",  "#89b4fa");
        Button btnAll   = actionBtn("⚡ Cargar Todo",    "#a6e3a1");
        btnAll.setStyle("-fx-background-color:#a6e3a1; -fx-text-fill:#1e1e2e; -fx-font-weight:bold; -fx-font-size:13px;");

        btnLoadB.setOnAction(e -> { if (!tfBranch.getText().isEmpty()) { log("Sucursales: " + csvLoader.loadBranches(tfBranch.getText()).message()); refreshStats(); notifyLoaded(); }});
        btnLoadC.setOnAction(e -> { if (!tfConn.getText().isEmpty())   { log("Conexiones: " + csvLoader.loadConnections(tfConn.getText()).message()); refreshStats(); notifyLoaded(); }});
        btnLoadP.setOnAction(e -> { if (!tfProd.getText().isEmpty())   { log("Productos: "  + csvLoader.loadProducts(tfProd.getText()).message()); refreshStats(); notifyLoaded(); }});
        btnAll.setOnAction(e -> {
            if (!tfBranch.getText().isEmpty()) log("Sucursales: " + csvLoader.loadBranches(tfBranch.getText()).message());
            if (!tfConn.getText().isEmpty())   log("Conexiones: " + csvLoader.loadConnections(tfConn.getText()).message());
            if (!tfProd.getText().isEmpty())   log("Productos: "  + csvLoader.loadProducts(tfProd.getText()).message());
            refreshStats(); notifyLoaded();
        });

        grid.addRow(0, lbl("Sucursales:"), tfBranch, btnB, btnLoadB);
        grid.addRow(1, lbl("Conexiones:"), tfConn,   btnC, btnLoadC);
        grid.addRow(2, lbl("Productos:"),  tfProd,   btnP, btnLoadP);
        grid.add(btnAll, 1, 3);

        TitledPane pane = new TitledPane("📁 Carga de Archivos CSV", grid);
        pane.setCollapsible(false);
        pane.setStyle("-fx-text-fill:#cdd6f4;");
        return pane;
    }

    public void refreshStats() {
        javafx.application.Platform.runLater(() -> {
            lblBranchCount.setText(String.valueOf(manager.getAllBranches().size()));
            lblProductCount.setText(String.valueOf(manager.totalProducts()));
            lblTransferCount.setText(String.valueOf(manager.getTransfers().size()));
        });
    }

    private void notifyLoaded() {
        if (onDataLoaded != null)
            javafx.application.Platform.runLater(onDataLoaded);
    }

    private void log(String msg) {
        javafx.application.Platform.runLater(() ->
            logArea.appendText("[" + java.time.LocalTime.now().withNano(0) + "] " + msg + "\n"));
    }

    private Button pickBtn(String icon, TextField tf) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:#585b70; -fx-text-fill:#cdd6f4;");
        b.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
            File f = fc.showOpenDialog(null);
            if (f != null) tf.setText(f.getAbsolutePath());
        });
        return b;
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        return b;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4; -fx-prompt-text-fill:#585b70;");
        return tf;
    }

    private Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill:#a6adc8;"); return l;
    }
}
