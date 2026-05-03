package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.catalog.Catalog;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;
import edu.usac.edd.structures.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * Panel de medición de rendimiento.
 * Compara: Lista no ordenada vs Lista ordenada vs AVL vs Hash.
 * N=20 consultas, M=5 repeticiones → promedio.
 * Muestra tabla + gráfica de barras.
 */
public class BenchmarkView {

    private static final int N = 20, M = 5;

    private final BranchManager manager;
    private ComboBox<String> cbBranch;
    private TextArea         taResults;
    private Canvas           chartCanvas;
    private GraphicsContext  gc;
    private Label            lblStatus;

    // Resultados para la gráfica
    private double[] avgResults;
    private String[] labels;

    public BenchmarkView(BranchManager manager) { this.manager = manager; }

    public Node build() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color:#1e1e2e;");

        // Toolbar
        Label lbr = new Label("Sucursal:");
        lbr.setStyle("-fx-text-fill:#cdd6f4;");
        cbBranch = new ComboBox<>();
        cbBranch.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4;");
        refreshCombo();

        Button btnRefresh = new Button("Refrescar");
        btnRefresh.setStyle("-fx-background-color:#89b4fa; -fx-text-fill:#1e1e2e;");
        btnRefresh.setOnAction(e -> refreshCombo());

        Button btnRun = new Button("⚡ Ejecutar Benchmark");
        btnRun.setStyle("-fx-background-color:#a6e3a1; -fx-text-fill:#1e1e2e; " +
                        "-fx-font-weight:bold; -fx-font-size:13px;");
        btnRun.setOnAction(e -> runBenchmark());

        lblStatus = new Label("Selecciona una sucursal y ejecuta el benchmark.");
        lblStatus.setStyle("-fx-text-fill:#a6adc8;");

        HBox toolbar = new HBox(10, lbr, cbBranch, btnRefresh, btnRun, lblStatus);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Resultados texto
        taResults = new TextArea();
        taResults.setEditable(false);
        taResults.setPrefHeight(280);
        taResults.setStyle("-fx-control-inner-background:#181825; -fx-text-fill:#89dceb; " +
                           "-fx-font-family:monospace; -fx-font-size:12px;");

        // Gráfica de barras
        chartCanvas = new Canvas(800, 200);
        gc = chartCanvas.getGraphicsContext2D();
        clearChart();

        // Big-O info
        TitledPane bigO = buildBigOPane();

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.55);
        split.setStyle("-fx-background-color:#1e1e2e;");
        split.getItems().addAll(
            wrapScrolled(taResults),
            new VBox(8, chartCanvas, bigO) {{
                setStyle("-fx-background-color:#1e1e2e;");
                setPadding(new Insets(0,0,0,8));
            }}
        );
        VBox.setVgrow(split, Priority.ALWAYS);

        root.getChildren().addAll(toolbar, split);
        return root;
    }

    // ── Benchmark ─────────────────────────────────────────────────────────
    private void runBenchmark() {
        String branchId = cbBranch.getValue();
        if (branchId == null) { alert("Selecciona una sucursal."); return; }
        Catalog cat = manager.getCatalog(branchId);
        if (cat == null || cat.isEmpty()) {
            alert("La sucursal no tiene productos."); return;
        }

        // Tomar una muestra: primer producto
        List<Product> all = cat.allProducts();
        Product sample = all.get(0);
        String sampleName    = sample.getName();
        String sampleBarcode = sample.getBarcode();
        String notExist      = "___NO_EXISTE___";

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("  BENCHMARK DE RENDIMIENTO — Sucursal: ").append(branchId).append("\n");
        sb.append("  N=").append(N).append(" consultas × M=").append(M)
          .append(" repeticiones = ").append(N*M).append(" mediciones\n");
        sb.append("═══════════════════════════════════════════════════════\n\n");

        // ── Búsqueda exitosa ─────────────────────────────────────────────
        sb.append("[ BÚSQUEDA EXITOSA — nombre: \"").append(sampleName).append("\" ]\n");
        double dUL  = bench(() -> cat.getUnsortedList().searchByName(sampleName));
        double dAVL = bench(() -> cat.getAVL().search(sampleName));
        double dH   = bench(() -> cat.getHash().search(sampleBarcode));
        printRow(sb, "Lista no ordenada", dUL,  "O(n)");
        printRow(sb, "Árbol AVL",         dAVL, "O(log n)");
        printRow(sb, "Tabla Hash",        dH,   "O(1) prom.");
        sb.append("\n");

        // ── Búsqueda fallida ─────────────────────────────────────────────
        sb.append("[ BÚSQUEDA FALLIDA — nombre: \"").append(notExist).append("\" ]\n");
        double fUL  = bench(() -> cat.getUnsortedList().searchByName(notExist));
        double fAVL = bench(() -> cat.getAVL().search(notExist));
        double fH   = bench(() -> cat.getHash().search("000000000"));
        printRow(sb, "Lista no ordenada", fUL,  "O(n)");
        printRow(sb, "Árbol AVL",         fAVL, "O(log n)");
        printRow(sb, "Tabla Hash",        fH,   "O(1) prom.");
        sb.append("\n");

        // ── Inserción ────────────────────────────────────────────────────
        Product tmp = new Product("_Temp","_TMPBC"+System.nanoTime(),"Test",
                                  "2026-12-31","Test",1.0,1);
        double iUL  = benchOnce(() -> { cat.getUnsortedList().insert(tmp);
                                        cat.getUnsortedList().remove(tmp.getBarcode()); });
        double iAVL = benchOnce(() -> { cat.getAVL().insert(tmp);
                                        cat.getAVL().remove(tmp.getName(), tmp.getBarcode()); });
        double iH   = benchOnce(() -> { cat.getHash().insert(tmp);
                                        cat.getHash().remove(tmp.getBarcode()); });
        sb.append("[ INSERCIÓN + ELIMINACIÓN ]\n");
        printRow(sb, "Lista no ordenada", iUL,  "O(1) ins / O(n) del");
        printRow(sb, "Árbol AVL",         iAVL, "O(log n)");
        printRow(sb, "Tabla Hash",        iH,   "O(1) prom.");

        sb.append("\n═══════════════════════════════════════════════════════\n");
        sb.append("Total productos en catálogo: ").append(cat.size()).append("\n");

        taResults.setText(sb.toString());
        lblStatus.setText("Benchmark completado ✓  |  Productos: " + cat.size());

        // Actualizar gráfica
        avgResults = new double[]{ dUL, dAVL, dH };
        labels     = new String[]{ "Lista", "AVL", "Hash" };
        drawChart();
    }

    private double bench(Runnable op) {
        double total = 0;
        for (int i = 0; i < N * M; i++) {
            long t0 = System.nanoTime();
            op.run();
            total += (System.nanoTime() - t0) / 1000.0; // µs
        }
        return total / (N * M);
    }

    private double benchOnce(Runnable op) {
        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) op.run();
        return (System.nanoTime() - t0) / 1000.0 / N;
    }

    private void printRow(StringBuilder sb, String struct, double micros, String bigO) {
        sb.append(String.format("  %-22s  %8.4f µs  (%6.4f ms)  %s\n",
                struct, micros, micros / 1000.0, bigO));
    }

    // ── Gráfica de barras ─────────────────────────────────────────────────
    private void drawChart() {
        if (avgResults == null) return;
        gc.setFill(Color.web("#181825"));
        gc.fillRect(0, 0, chartCanvas.getWidth(), chartCanvas.getHeight());

        double max = 0;
        for (double v : avgResults) max = Math.max(max, v);
        if (max == 0) max = 1;

        double chartH = 150, chartY = 20, barW = 80, gap = 60;
        double startX = 60;

        Color[] colors = {Color.web("#89b4fa"), Color.web("#a6e3a1"), Color.web("#f9e2af")};

        for (int i = 0; i < avgResults.length; i++) {
            double h = (avgResults[i] / max) * chartH;
            double x = startX + i * (barW + gap);
            double y = chartY + chartH - h;

            gc.setFill(colors[i % colors.length]);
            gc.fillRect(x, y, barW, h);
            gc.setStroke(Color.web("#cdd6f4")); gc.strokeRect(x, y, barW, h);

            // Etiqueta valor
            gc.setFill(Color.web("#cdd6f4")); gc.setFont(Font.font(10));
            gc.fillText(String.format("%.4f µs", avgResults[i]), x, y - 4);

            // Etiqueta estructura
            gc.setFill(Color.web("#a6adc8")); gc.setFont(Font.font(11));
            gc.fillText(labels[i], x + barW/2 - labels[i].length()*3,
                        chartY + chartH + 15);
        }

        // Eje
        gc.setStroke(Color.web("#45475a")); gc.setLineWidth(1);
        gc.strokeLine(startX - 10, chartY + chartH, startX + 320, chartY + chartH);

        gc.setFill(Color.web("#cdd6f4")); gc.setFont(Font.font(11));
        gc.fillText("Búsqueda exitosa (µs) — menor es mejor", 50, 190);
    }

    private void clearChart() {
        gc.setFill(Color.web("#181825"));
        gc.fillRect(0, 0, chartCanvas.getWidth(), chartCanvas.getHeight());
        gc.setFill(Color.web("#585b70")); gc.setFont(Font.font(12));
        gc.fillText("Ejecuta el benchmark para ver la gráfica", 60, 100);
    }

    // ── Big-O panel ───────────────────────────────────────────────────────
    private TitledPane buildBigOPane() {
        String info =
            "Lista no ordenada : Búsqueda O(n), Inserción O(1), Eliminación O(n)\n" +
            "Lista ordenada    : Búsqueda O(n), Inserción O(n), Eliminación O(n)\n" +
            "Árbol AVL         : Búsqueda/Ins/Del O(log n), In-order O(n)\n" +
            "Tabla Hash (djb2) : Búsqueda/Ins/Del O(1) prom, O(n) peor caso\n" +
            "Árbol B (T=3)     : Búsqueda/Ins/Del O(T·log_T n), Rango O(+k)\n" +
            "Árbol B+ (ord=4)  : Búsqueda O(log n), Recorrido hojas O(n)\n" +
            "Grafo (Dijkstra)  : O((V+E)·log V) con MinHeap\n";
        TextArea ta = new TextArea(info);
        ta.setEditable(false);
        ta.setPrefHeight(120);
        ta.setStyle("-fx-control-inner-background:#181825; -fx-text-fill:#fab387; " +
                    "-fx-font-family:monospace; -fx-font-size:10px;");
        TitledPane pane = new TitledPane("📊 Complejidades Big-O", ta);
        pane.setCollapsible(false);
        pane.setStyle("-fx-text-fill:#cdd6f4;");
        return pane;
    }

    private void refreshCombo() {
        String prev = cbBranch.getValue();
        cbBranch.getItems().setAll(manager.getAllBranches().stream()
                .map(Branch::getId).toList());
        if (prev != null && cbBranch.getItems().contains(prev)) cbBranch.setValue(prev);
        else if (!cbBranch.getItems().isEmpty()) cbBranch.setValue(cbBranch.getItems().get(0));
    }

    private ScrollPane wrapScrolled(javafx.scene.Node n) {
        ScrollPane sp = new ScrollPane(n);
        sp.setFitToWidth(true); sp.setFitToHeight(true);
        return sp;
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
