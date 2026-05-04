package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.graph.BranchGraph;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Transfer;
import edu.usac.edd.simulation.Dispatcher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GraphView {

    private final BranchManager manager;
    private Canvas    canvas;
    private GraphicsContext gc;
    private List<String>    highlightedRoute;
    private TextArea        taInfo;
    private Label           lblStats;

    private Branch  dragging;
    private double  dragOffX, dragOffY;

    public GraphView(BranchManager manager) { this.manager = manager; }

    public Node build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#1e1e2e;");

        HBox toolbar = buildToolbar();

        // Canvas + info split
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.75);
        split.setStyle("-fx-background-color:#1e1e2e;");

        canvas = new Canvas(800, 560);
        gc     = canvas.getGraphicsContext2D();

        layoutNodes();
        draw();

        // Drag support
        canvas.setOnMousePressed(e -> {
            for (Branch b : manager.getAllBranches()) {
                double dx = e.getX() - b.getX(), dy = e.getY() - b.getY();
                if (Math.sqrt(dx*dx + dy*dy) < 28) {
                    dragging = b; dragOffX = dx; dragOffY = dy; break;
                }
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (dragging != null) {
                dragging.setPosition(e.getX() - dragOffX, e.getY() - dragOffY);
                draw();
            }
        });
        canvas.setOnMouseReleased(e -> dragging = null);

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color:#181825;");

        VBox infoPanel = new VBox(8);
        infoPanel.setPadding(new Insets(12));
        infoPanel.setStyle("-fx-background-color:#181825;");

        lblStats = new Label();
        lblStats.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:11px;");
        updateStats();

        Label lRoute = new Label(" Ruta seleccionada:");
        lRoute.setStyle("-fx-text-fill:#cdd6f4; -fx-font-weight:bold;");

        taInfo = new TextArea("Usa el panel de Transferencias\npara ver rutas resaltadas.");
        taInfo.setEditable(false);
        taInfo.setWrapText(true);
        taInfo.setStyle("-fx-control-inner-background:#1e1e2e; -fx-text-fill:#89dceb; " +
                        "-fx-font-family:monospace; -fx-font-size:11px;");
        VBox.setVgrow(taInfo, Priority.ALWAYS);

        Button btnRefresh = new Button(" Redibujar");
        btnRefresh.setStyle("-fx-background-color:#89b4fa; -fx-text-fill:#1e1e2e;");
        btnRefresh.setOnAction(e -> { layoutNodes(); draw(); updateStats(); });

        Button btnDot = new Button(" Exportar DOT");
        btnDot.setStyle("-fx-background-color:#cba6f7; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        btnDot.setOnAction(e -> exportDot());

        Button btnPng = new Button(" Exportar PNG");
        btnPng.setStyle("-fx-background-color:#f38ba8; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        btnPng.setOnAction(e -> exportImage("png"));

        Button btnJpg = new Button(" Exportar JPG");
        btnJpg.setStyle("-fx-background-color:#fab387; -fx-text-fill:#1e1e2e; -fx-font-weight:bold;");
        btnJpg.setOnAction(e -> exportImage("jpg"));

        infoPanel.getChildren().addAll(lblStats, lRoute, taInfo, btnRefresh, btnDot, btnPng, btnJpg);

        split.getItems().addAll(canvasPane, infoPanel);

        VBox.setVgrow(split, Priority.ALWAYS);
        root.getChildren().addAll(toolbar, split);
        return root;
    }

    private HBox buildToolbar() {
        Label title = new Label(" Red de Sucursales");
        title.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:14px; -fx-font-weight:bold;");

        Label legend1 = new Label("● Sucursal");
        legend1.setStyle("-fx-text-fill:#f9e2af;");
        Label legend2 = new Label("● En tránsito");
        legend2.setStyle("-fx-text-fill:#f38ba8;");
        Label legend3 = new Label("─ Conexión");
        legend3.setStyle("-fx-text-fill:#585b70;");
        Label legend4 = new Label("─ Ruta óptima");
        legend4.setStyle("-fx-text-fill:#a6e3a1;");

        HBox bar = new HBox(16, title, legend1, legend2, legend3, legend4);
        bar.setPadding(new Insets(8 , 16, 8, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#181825;");
        return bar;
    }

    public void layoutNodes() {
        var branches = manager.getAllBranches();
        int total = branches.size();
        if (total == 0) return;
        double cx = 400, cy = 280, r = Math.min(220, 40 + total * 20);
        int i = 0;
        for (Branch b : branches) {
            double angle = 2 * Math.PI * i / total - Math.PI / 2;
            b.setPosition(cx + r * Math.cos(angle), cy + r * Math.sin(angle));
            i++;
        }
    }

    public void draw() {
        gc.setFill(Color.web("#181825"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        
        for (BranchGraph.Edge e : manager.getGraph().getEdges()) {
            Branch from = manager.getBranch(e.fromId);
            Branch to   = manager.getBranch(e.toId);
            if (from == null || to == null) continue;

            boolean onRoute = highlightedRoute != null &&
                              isOnRoute(e.fromId, e.toId);
            gc.setStroke(onRoute ? Color.web("#a6e3a1") : Color.web("#45475a"));
            gc.setLineWidth(onRoute ? 3 : 1.5);
            gc.strokeLine(from.getX(), from.getY(), to.getX(), to.getY());

            
            drawArrow(from.getX(), from.getY(), to.getX(), to.getY(), onRoute);

            
            double mx = (from.getX() + to.getX()) / 2;
            double my = (from.getY() + to.getY()) / 2;
            gc.setFill(Color.web("#a6adc8"));
            gc.setFont(Font.font(9));
            gc.fillText(String.format("t=%.0fs c=%.0f", e.time, e.cost), mx + 4, my - 4);
        }

        
        for (Branch b : manager.getAllBranches()) {
            boolean isActive = isInTransit(b.getId());
            boolean onRoute  = highlightedRoute != null &&
                               highlightedRoute.contains(b.getId());

            Color fill = onRoute  ? Color.web("#a6e3a1") :
                         isActive ? Color.web("#f38ba8") :
                                    Color.web("#f9e2af");

            gc.setFill(fill);
            gc.fillOval(b.getX()-22, b.getY()-22, 44, 44);
            gc.setStroke(Color.web("#cdd6f4"));
            gc.setLineWidth(onRoute ? 2.5 : 1.5);
            gc.strokeOval(b.getX()-22, b.getY()-22, 44, 44);

            gc.setFill(Color.web("#1e1e2e"));
            gc.setFont(Font.font("Arial", 10));
            String label = b.getId();
            gc.fillText(label, b.getX() - label.length()*3, b.getY() + 4);

            gc.setFill(Color.web("#cdd6f4"));
            gc.setFont(Font.font(9));
            String shortName = b.getName().length() > 12
                    ? b.getName().substring(0,12) : b.getName();
            gc.fillText(shortName, b.getX() - shortName.length()*3, b.getY() + 34);
        }
    }

    private void drawArrow(double x1, double y1, double x2, double y2, boolean highlight) {
        double angle = Math.atan2(y2-y1, x2-x1);
        double d = 24;
        double ax = x2 - d * Math.cos(angle);
        double ay = y2 - d * Math.sin(angle);
        double size = 8;
        gc.setFill(highlight ? Color.web("#a6e3a1") : Color.web("#45475a"));
        gc.fillPolygon(
            new double[]{ ax, ax - size*Math.cos(angle-0.4), ax - size*Math.cos(angle+0.4) },
            new double[]{ ay, ay - size*Math.sin(angle-0.4), ay - size*Math.sin(angle+0.4) },
            3
        );
    }

    private boolean isOnRoute(String from, String to) {
        if (highlightedRoute == null) return false;
        for (int i = 0; i < highlightedRoute.size()-1; i++)
            if (highlightedRoute.get(i).equals(from) &&
                highlightedRoute.get(i+1).equals(to)) return true;
        return false;
    }

    private boolean isInTransit(String branchId) {
        var queues = manager.getDispatcher().getAllQueues().get(branchId);
        if (queues == null) return false;
        return queues.ingreso.size() > 0 || queues.traspaso.size() > 0 || queues.salida.size() > 0;
    }

    public void highlightRoute(List<String> route, String info) {
        this.highlightedRoute = route;
        if (taInfo != null) taInfo.setText(info);
        draw();
    }

    public void refreshQueues() { draw(); updateStats(); }

    private void updateStats() {
        if (lblStats == null) return;
        lblStats.setText(String.format("Sucursales: %d  |  Conexiones: %d  |  Transferencias: %d",
                manager.getGraph().branchCount(),
                manager.getGraph().edgeCount(),
                manager.getTransfers().size()));
    }

    private void exportDot() {
        String dot = manager.getGraph().toDot();
        try {
            Files.createDirectories(Path.of("output"));
            Files.writeString(Path.of("output/graph.dot"), dot);
            boolean graphvizOk = tryGraphviz("output/graph.dot", "output/graph.png");
            String msg = " DOT guardado: output/graph.dot";
            if (graphvizOk) msg += "\n PNG generado: output/graph.png (Graphviz)";
            else msg += "\n Graphviz no disponible.\nConvertir manualmente:\n  dot -Tpng output/graph.dot -o output/graph.png";
            new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void exportImage(String format) {
        try {
            Files.createDirectories(Path.of("output"));
            String filename = "output/graph." + format;
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.web("#181825"));
            WritableImage img = canvas.snapshot(sp, null);
            boolean ok = ImageIO.write(SwingFXUtils.fromFXImage(img, null),
                    "jpg".equals(format) ? "jpg" : "png", new File(filename));
            if (ok) new Alert(Alert.AlertType.INFORMATION,
                    " Imagen guardada: " + filename, ButtonType.OK).showAndWait();
            else new Alert(Alert.AlertType.ERROR, "No se pudo guardar la imagen.", ButtonType.OK).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private boolean tryGraphviz(String dotFile, String pngFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile, "-o", pngFile);
            pb.redirectErrorStream(true);
            return pb.start().waitFor() == 0;
        } catch (Exception e) { return false; }
    }
}
