package edu.usac.edd.gui;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.csv.CSVLoader;
import edu.usac.edd.gui.views.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 */
public class MainApp extends Application {

    private BranchManager manager;
    private CSVLoader      csvLoader;

    @Override
    public void start(Stage primaryStage) {
        manager   = new BranchManager();
        csvLoader = new CSVLoader(manager, "errors.log");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#1e1e2e;");
        root.setTop(buildHeader());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:#1e1e2e;");

        DashboardView dashView  = new DashboardView(manager, csvLoader);
        BranchView    branchView= new BranchView(manager);
        ProductView   prodView  = new ProductView(manager);
        TransferView  transView = new TransferView(manager);
        GraphView     graphView = new GraphView(manager);
        TreeView_     treeView  = new TreeView_(manager);
        BenchmarkView benchView = new BenchmarkView(manager);

        tabPane.getTabs().addAll(
            tab("🏠 Dashboard",        dashView.build()),
            tab("🏪 Sucursales",        branchView.build()),
            tab("📦 Productos",         prodView.build()),
            tab("🚚 Transferencias",    transView.build()),
            tab("🗺 Red de Sucursales", graphView.build()),
            tab("🌳 Árboles",           treeView.build()),
            tab("⚡ Rendimiento",       benchView.build())
        );

        // Vincular dispatcher con actualizaciones de UI
        manager.getDispatcher().setOnUpdate(() ->
            Platform.runLater(() -> {
                transView.refreshQueues();
                graphView.refreshQueues();
                dashView.refreshStats();
            })
        );

        // Al cargar CSV actualizar BranchView, GraphView y TransferView
        dashView.setOnDataLoaded(() -> {
            branchView.refresh();
            graphView.layoutNodes();
            graphView.draw();
            graphView.refreshQueues();
        });

        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1280, 800);
        try {
            var css = getClass().getResource("/css/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        primaryStage.setTitle("🛒 Gestión de Catálogo de Supermercado — Fase 2 | EDD 2026");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private HBox buildHeader() {
        Label title = new Label("Gestión de Catálogo de Supermercado — Fase 2");
        title.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#cdd6f4; -fx-padding:10 20;");
        Label sub = new Label("EDD 2026  |  USAC CUNOC  |  Java + JavaFX");
        sub.setStyle("-fx-font-size:11px; -fx-text-fill:#a6adc8; -fx-padding:10 0;");
        HBox h = new HBox(10, title, sub);
        h.setStyle("-fx-background-color:#181825; -fx-padding:2 10;");
        HBox.setHgrow(title, Priority.ALWAYS);
        return h;
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab t = new Tab(title, content);
        t.setStyle("-fx-font-size:12px;");
        return t;
    }

    public static void main(String[] args) { launch(args); }
}
