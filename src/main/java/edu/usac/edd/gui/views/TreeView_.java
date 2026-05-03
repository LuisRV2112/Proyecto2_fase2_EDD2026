package edu.usac.edd.gui.views;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.catalog.Catalog;
import edu.usac.edd.model.Branch;
import edu.usac.edd.structures.AVLTree;
import edu.usac.edd.structures.BTree;
import edu.usac.edd.structures.BPlusTree;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Visualizador de AVL, B-tree, B+ tree y HashTable.
 * Canvas con zoom, exporta .dot y .png.
 */
public class TreeView_ {

    private final BranchManager manager;
    private ComboBox<String> cbBranch, cbTree;
    private Canvas           canvas;
    private GraphicsContext  gc;
    private TextArea         taDot;
    private Label            lblInfo, lblZoom;
    private ScrollPane       canvasScroll;

    private double zoomLevel = 1.0;
    private static final double ZOOM_MIN = 0.3;
    private static final double ZOOM_MAX = 4.0;
    private static final double ZOOM_STEP = 0.15;

    // Canvas base size (virtual space before zoom)
    private static final double BASE_W = 1800;
    private static final double BASE_H = 1200;

    public TreeView_(BranchManager manager) { this.manager = manager; }

    public Node build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#1e1e2e;");
        root.getChildren().addAll(buildToolbar(), buildContent());
        return root;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────
    private HBox buildToolbar() {
        Label lbr = new Label("Sucursal:");
        lbr.setStyle("-fx-text-fill:#cdd6f4;");
        cbBranch = new ComboBox<>();
        cbBranch.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4;");
        cbBranch.setOnAction(e -> refresh());

        Label ltr = new Label("Árbol:");
        ltr.setStyle("-fx-text-fill:#cdd6f4;");
        cbTree = new ComboBox<>();
        cbTree.getItems().addAll("AVL (por nombre)", "B-tree (por caducidad)",
                                 "B+ tree (por categoría)", "Hash Table");
        cbTree.setValue("AVL (por nombre)");
        cbTree.setStyle("-fx-background-color:#313244; -fx-text-fill:#cdd6f4;");
        cbTree.setOnAction(e -> refresh());

        Button btnRefresh = toolBtn(" Refrescar", "#89b4fa");
        btnRefresh.setOnAction(e -> { refreshBranchCombo(); refresh(); });

        // Zoom controls
        Button btnZoomIn  = toolBtn("+", "#a6e3a1");
        Button btnZoomOut = toolBtn("-", "#f9e2af");
        Button btnZoomRst = toolBtn(" Reset", "#cba6f7");

        lblZoom = new Label("100%");
        lblZoom.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:11px; -fx-min-width:40px;");

        btnZoomIn.setOnAction(e  -> applyZoom(zoomLevel + ZOOM_STEP));
        btnZoomOut.setOnAction(e -> applyZoom(zoomLevel - ZOOM_STEP));
        btnZoomRst.setOnAction(e -> applyZoom(1.0));

        // Export buttons
        Button btnExportDot = toolBtn(" DOT", "#cba6f7");
        btnExportDot.setOnAction(e -> exportDot());

        Button btnExportPng = toolBtn(" PNG", "#f38ba8");
        btnExportPng.setOnAction(e -> exportImage("png"));

        Button btnExportJpg = toolBtn(" JPG", "#fab387");
        btnExportJpg.setOnAction(e -> exportImage("jpg"));

        Button btnExportAll = toolBtn(" Exportar Todo", "#a6e3a1");
        btnExportAll.setOnAction(e -> exportAll());

        lblInfo = new Label("");
        lblInfo.setStyle("-fx-text-fill:#a6adc8; -fx-font-size:11px;");

        HBox bar = new HBox(8, lbr, cbBranch, ltr, cbTree,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                btnRefresh,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                btnZoomOut, lblZoom, btnZoomIn, btnZoomRst,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                btnExportDot, btnExportPng, btnExportJpg, btnExportAll,
                lblInfo);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#181825;");

        refreshBranchCombo();
        return bar;
    }

    // ── Content ───────────────────────────────────────────────────────────
    private Node buildContent() {
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.68);
        split.setStyle("-fx-background-color:#1e1e2e;");

        
        canvas = new Canvas(BASE_W, BASE_H);
        gc     = canvas.getGraphicsContext2D();
        clearCanvas();

        // Wrap in a StackPane so the canvas keeps its size while scroll pane clips
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color:#181825;");

        canvasScroll = new ScrollPane(canvasPane);
        canvasScroll.setStyle("-fx-background-color:#181825; -fx-background:#181825;");
        canvasScroll.setPannable(true);
        canvasScroll.setFitToWidth(false);
        canvasScroll.setFitToHeight(false);

        // Zoom with scroll wheel (Ctrl+scroll)
        canvasScroll.setOnScroll(e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY() > 0 ? ZOOM_STEP : -ZOOM_STEP;
                applyZoom(zoomLevel + delta);
                e.consume();
            }
        });

        // Hint label
        Label hint = new Label(" Ctrl+Scroll para zoom  |  Arrastra para navegar");
        hint.setStyle("-fx-text-fill:#585b70; -fx-font-size:10px; -fx-padding:2 0 0 4;");

        VBox canvasBox = new VBox(0, canvasScroll, hint);
        VBox.setVgrow(canvasScroll, Priority.ALWAYS);

        
        VBox dotPanel = new VBox(8);
        dotPanel.setPadding(new Insets(10));
        dotPanel.setStyle("-fx-background-color:#181825;");
        Label dotLabel = new Label(" Archivo DOT generado:");
        dotLabel.setStyle("-fx-text-fill:#cdd6f4; -fx-font-weight:bold;");
        taDot = new TextArea();
        taDot.setEditable(false);
        taDot.setStyle("-fx-control-inner-background:#1e1e2e; -fx-text-fill:#a6e3a1; " +
                       "-fx-font-family:monospace; -fx-font-size:10px;");
        VBox.setVgrow(taDot, Priority.ALWAYS);
        dotPanel.getChildren().addAll(dotLabel, taDot);

        split.getItems().addAll(canvasBox, dotPanel);
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    // ── Zoom ──────────────────────────────────────────────────────────────
    private void applyZoom(double newZoom) {
        zoomLevel = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, newZoom));
        canvas.setScaleX(zoomLevel);
        canvas.setScaleY(zoomLevel);
        lblZoom.setText(String.format("%.0f%%", zoomLevel * 100));
    }

    // ── Dibujo ────────────────────────────────────────────────────────────
    private void refresh() {
        String branchId = cbBranch.getValue();
        if (branchId == null) return;
        Catalog cat = manager.getCatalog(branchId);
        if (cat == null) return;

        clearCanvas();
        String selected = cbTree.getValue();
        String dot = "";

        if (selected.startsWith("AVL")) {
            drawAVL(cat.getAVL().getRoot(), BASE_W / 2, 50, BASE_W / 3.5, 0);
            dot = cat.getAVL().toDot();
            lblInfo.setText("AVL: " + cat.getAVL().size() + " nodos");
        } else if (selected.startsWith("B-tree")) {
            drawBTree(cat.getBTree().getRoot(), 40, 60, (int)(BASE_W - 80), 0);
            dot = cat.getBTree().toDot();
            lblInfo.setText("B-tree: " + cat.getBTree().size() + " registros");
        } else if (selected.startsWith("B+ tree")) {
            drawBPlus(cat.getBPlusTree().getRoot(), 40, 60, (int)(BASE_W - 80), 0);
            dot = cat.getBPlusTree().toDot();
            lblInfo.setText("B+ tree: " + cat.getBPlusTree().size() + " registros");
        } else {
            drawHashTable(cat.getHash());
            dot = cat.getHash().toDot();
            lblInfo.setText("Hash: " + cat.getHash().size() + " elementos  " +
                    String.format("factor=%.2f", cat.getHash().loadFactor()));
        }
        taDot.setText(dot);
    }

    private void clearCanvas() {
        gc.setFill(Color.web("#181825"));
        gc.fillRect(0, 0, BASE_W, BASE_H);
    }

    // ── AVL drawing ───────────────────────────────────────────────────────
    private void drawAVL(AVLTree.Node node, double x, double y, double spread, int depth) {
        if (node == null || depth > 16) return;
        double r = 26;
        double levelH = 80;
        if (node.left != null) {
            double cx = x - spread, cy = y + levelH;
            gc.setStroke(Color.web("#45475a")); gc.setLineWidth(1.5);
            gc.strokeLine(x, y + r, cx, cy - r);
            drawAVL(node.left, cx, cy, spread / 2, depth + 1);
        }
        if (node.right != null) {
            double cx = x + spread, cy = y + levelH;
            gc.setStroke(Color.web("#45475a")); gc.setLineWidth(1.5);
            gc.strokeLine(x, y + r, cx, cy - r);
            drawAVL(node.right, cx, cy, spread / 2, depth + 1);
        }
        gc.setFill(Color.web("#89b4fa")); gc.fillOval(x - r, y - r, 2 * r, 2 * r);
        gc.setStroke(Color.web("#cdd6f4")); gc.setLineWidth(1.2);
        gc.strokeOval(x - r, y - r, 2 * r, 2 * r);

        gc.setFill(Color.web("#1e1e2e")); gc.setFont(Font.font(9));
        String label = node.data.getName().length() > 9
                ? node.data.getName().substring(0, 9) : node.data.getName();
        gc.fillText(label, x - label.length() * 3.0, y + 3);

        gc.setFill(Color.web("#a6adc8")); gc.setFont(Font.font(8));
        gc.fillText("h=" + node.height, x - 9, y + r + 12);
    }

    // ── B-Tree drawing ────────────────────────────────────────────────────
    private void drawBTree(BTree.BNode node, double x, double y, double w, int depth) {
        if (node == null || depth > 8) return;
        double h = 30, cellW = Math.min(90, w / Math.max(1, node.n));
        double totalW = cellW * node.n;
        double startX = x + (w - totalW) / 2;

        for (int i = 0; i < node.n; i++) {
            double cx = startX + i * cellW;
            gc.setFill(Color.web("#f9e2af")); gc.fillRect(cx, y, cellW - 2, h);
            gc.setStroke(Color.web("#45475a")); gc.strokeRect(cx, y, cellW - 2, h);
            gc.setFill(Color.web("#1e1e2e")); gc.setFont(Font.font(8.5));
            String k = node.keys[i] != null
                    ? (node.keys[i].length() > 9 ? node.keys[i].substring(2) : node.keys[i]) : "";
            gc.fillText(k, cx + 3, y + 19);
        }

        if (!node.isLeaf) {
            double childW = w / (node.n + 1);
            for (int i = 0; i <= node.n; i++) {
                double cx = x + i * childW;
                gc.setStroke(Color.web("#585b70")); gc.setLineWidth(1);
                gc.strokeLine(startX + i * cellW, y + h, cx + childW / 2, y + h + 55);
                drawBTree(node.children[i], cx, y + h + 55, childW, depth + 1);
            }
        }
    }

    // ── B+ Tree drawing ───────────────────────────────────────────────────
    private void drawBPlus(Object node, double x, double y, double w, int depth) {
        if (node == null || depth > 8) return;
        if (node instanceof BPlusTree.LeafNode leaf) {
            double cellW = Math.min(80, w / Math.max(1, leaf.n));
            for (int i = 0; i < leaf.n; i++) {
                double cx = x + i * cellW;
                gc.setFill(Color.web("#a6e3a1")); gc.fillRect(cx, y, cellW - 2, 28);
                gc.setStroke(Color.web("#45475a")); gc.strokeRect(cx, y, cellW - 2, 28);
                gc.setFill(Color.web("#1e1e2e")); gc.setFont(Font.font(8.5));
                String k = leaf.keys[i] != null
                        ? (leaf.keys[i].length() > 9 ? leaf.keys[i].substring(0, 9) : leaf.keys[i]) : "";
                gc.fillText(k, cx + 2, y + 18);
            }
            if (leaf.next != null) {
                double lx = x + leaf.n * Math.min(80, w / Math.max(1, leaf.n));
                gc.setStroke(Color.web("#89dceb")); gc.setLineDashes(4);
                gc.strokeLine(lx, y + 14, lx + 18, y + 14);
                gc.setLineDashes(0);
            }
        } else if (node instanceof BPlusTree.InternalNode nd) {
            double cellW = Math.min(90, w / Math.max(1, nd.n));
            double totalW = cellW * nd.n;
            double startX = x + (w - totalW) / 2;
            for (int i = 0; i < nd.n; i++) {
                double cx = startX + i * cellW;
                gc.setFill(Color.web("#f9e2af")); gc.fillRect(cx, y, cellW - 2, 28);
                gc.setStroke(Color.web("#45475a")); gc.strokeRect(cx, y, cellW - 2, 28);
                gc.setFill(Color.web("#1e1e2e")); gc.setFont(Font.font(8.5));
                String k = nd.keys[i] != null
                        ? (nd.keys[i].length() > 9 ? nd.keys[i].substring(0, 9) : nd.keys[i]) : "";
                gc.fillText(k, cx + 2, y + 18);
            }
            double childW = w / (nd.n + 1);
            for (int i = 0; i <= nd.n; i++) {
                double cx = x + i * childW;
                gc.setStroke(Color.web("#585b70")); gc.setLineWidth(1);
                gc.strokeLine(startX + i * cellW, y + 28, cx + childW / 2, y + 80);
                drawBPlus(nd.children[i], cx, y + 80, childW, depth + 1);
            }
        }
    }

    // ── HashTable drawing ─────────────────────────────────────────────────
    private void drawHashTable(edu.usac.edd.structures.HashTable hash) {
        // Show only occupied buckets, up to 40 for readability
        int maxBuckets = 40;
        int cap = hash.capacity();
        edu.usac.edd.structures.HashTable.Entry[] table = hash.getTable();

        // Collect occupied indices
        java.util.List<Integer> occupied = new java.util.ArrayList<>();
        for (int i = 0; i < cap && occupied.size() < maxBuckets; i++) {
            if (table[i] != null) occupied.add(i);
        }

        double colW = 120, rowH = 36, x0 = 30, y0 = 30;
        int cols = Math.max(1, (int)((BASE_W - x0 * 2) / colW));

        // Title
        gc.setFill(Color.web("#cdd6f4")); gc.setFont(Font.font(13));
        gc.fillText("Tabla Hash — " + hash.size() + " elementos, " +
                    occupied.size() + " buckets ocupados mostrados" +
                    (occupied.size() == maxBuckets ? " (máx " + maxBuckets + ")" : ""), x0, y0 - 10);

        for (int idx = 0; idx < occupied.size(); idx++) {
            int bucketIdx = occupied.get(idx);
            int row = idx / cols;
            int col = idx % cols;
            double bx = x0 + col * colW;
            double by = y0 + row * (rowH * 4 + 20);

            // Bucket header
            gc.setFill(Color.web("#89b4fa")); gc.fillRect(bx, by, colW - 6, rowH);
            gc.setStroke(Color.web("#cdd6f4")); gc.strokeRect(bx, by, colW - 6, rowH);
            gc.setFill(Color.web("#1e1e2e")); gc.setFont(Font.font(9));
            gc.fillText("[" + bucketIdx + "]", bx + 4, by + 14);
            int chainLen = hash.chainLength(bucketIdx);
            gc.fillText("n=" + chainLen, bx + 4, by + 28);

            // Chain entries
            edu.usac.edd.structures.HashTable.Entry cur = table[bucketIdx];
            int pos = 0;
            while (cur != null && pos < 3) {
                double ey = by + rowH + pos * (rowH - 4);
                Color entryColor = pos == 0 ? Color.web("#f9e2af") : Color.web("#fab387");
                gc.setFill(entryColor); gc.fillRect(bx + 8, ey, colW - 18, rowH - 6);
                gc.setStroke(Color.web("#45475a")); gc.strokeRect(bx + 8, ey, colW - 18, rowH - 6);
                gc.setFill(Color.web("#1e1e2e")); gc.setFont(Font.font(7.5));
                String bc = cur.data.getBarcode();
                String nm = cur.data.getName().length() > 10
                        ? cur.data.getName().substring(0, 10) : cur.data.getName();
                gc.fillText(bc, bx + 10, ey + 10);
                gc.fillText(nm, bx + 10, ey + 21);
                cur = cur.next;
                pos++;
            }
            if (cur != null) {
                gc.setFill(Color.web("#f38ba8")); gc.setFont(Font.font(8));
                gc.fillText("+" + (chainLen - 3) + " más…", bx + 10, by + rowH + pos * (rowH - 4) + 12);
            }
        }

        if (occupied.isEmpty()) {
            gc.setFill(Color.web("#a6adc8")); gc.setFont(Font.font(14));
            gc.fillText("Tabla Hash vacía — sin productos en esta sucursal", 80, BASE_H / 2);
        }
    }

    // ── Exportar DOT ──────────────────────────────────────────────────────
    private void exportDot() {
        String branchId = cbBranch.getValue();
        if (branchId == null) { alert("Selecciona una sucursal."); return; }
        Catalog cat = manager.getCatalog(branchId);
        if (cat == null) return;
        try {
            Files.createDirectories(Path.of("output"));
            String sel = cbTree.getValue();
            String dot; String filename;
            if (sel.startsWith("AVL")) {
                dot = cat.getAVL().toDot();    filename = "output/avl_" + branchId + ".dot";
            } else if (sel.startsWith("B-tree")) {
                dot = cat.getBTree().toDot();  filename = "output/btree_" + branchId + ".dot";
            } else if (sel.startsWith("B+ tree")) {
                dot = cat.getBPlusTree().toDot(); filename = "output/bplus_" + branchId + ".dot";
            } else {
                dot = cat.getHash().toDot();   filename = "output/hash_" + branchId + ".dot";
            }
            Files.writeString(Path.of(filename), dot);
            info("DOT guardado: " + filename);
        } catch (IOException ex) { alert(ex.getMessage()); }
    }

    // ── Exportar imagen (PNG o JPG) desde canvas ──────────────────────────
    private void exportImage(String format) {
        String branchId = cbBranch.getValue();
        if (branchId == null) { alert("Selecciona una sucursal."); return; }
        String sel = cbTree.getValue();
        String prefix = sel.startsWith("AVL") ? "avl"
                      : sel.startsWith("B-tree") ? "btree"
                      : sel.startsWith("B+ tree") ? "bplus" : "hash";
        String filename = "output/" + prefix + "_" + branchId + "." + format;
        saveCanvasAsImage(filename, format);
    }

    private void saveCanvasAsImage(String filename, String format) {
        try {
            Files.createDirectories(Path.of("output"));
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.web("#181825"));
            // Take snapshot at full canvas size (zoom independent)
            sp.setTransform(new Scale(1, 1));
            WritableImage img = canvas.snapshot(sp, null);
            File out = new File(filename);
            boolean ok = ImageIO.write(SwingFXUtils.fromFXImage(img, null),
                    "jpg".equals(format) ? "jpg" : "png", out);
            if (ok) info("Imagen guardada: " + filename);
            else     alert("No se pudo guardar la imagen.");
        } catch (IOException ex) { alert(ex.getMessage()); }
    }

    // ── Exportar TODO (DOT + PNG de todos los árboles + hash + grafo) ─────
    private void exportAll() {
        String branchId = cbBranch.getValue();
        if (branchId == null) { alert("Selecciona una sucursal para exportar sus árboles."); return; }
        Catalog cat = manager.getCatalog(branchId);
        if (cat == null) return;

        try {
            Files.createDirectories(Path.of("output"));
            StringBuilder report = new StringBuilder(" Exportación completa:\n\n");

            // --- DOT files ---
            String[][] exports = {
                { "output/avl_"   + branchId + ".dot", cat.getAVL().toDot()       },
                { "output/btree_" + branchId + ".dot", cat.getBTree().toDot()      },
                { "output/bplus_" + branchId + ".dot", cat.getBPlusTree().toDot()  },
                { "output/hash_"  + branchId + ".dot", cat.getHash().toDot()       },
                { "output/graph.dot",                   manager.getGraph().toDot()  }
            };
            for (String[] pair : exports) {
                Files.writeString(Path.of(pair[0]), pair[1]);
                report.append(" ").append(pair[0]).append("\n");
            }

            // --- PNG via canvas snapshot (each tree drawn, then snapshot) ---
            String[][] trees = {
                {"AVL (por nombre)",       "avl_"   + branchId},
                {"B-tree (por caducidad)", "btree_" + branchId},
                {"B+ tree (por categoría)","bplus_" + branchId},
                {"Hash Table",             "hash_"  + branchId}
            };
            for (String[] t : trees) {
                cbTree.setValue(t[0]);
                refresh();
                String pngFile = "output/" + t[1] + ".png";
                saveCanvasAsImage(pngFile, "png");
                report.append(" ").append(pngFile).append("\n");
            }

            // Grafo PNG
            cbTree.setValue("AVL (por nombre)"); // restore
            refresh();

            // Graphviz hint
            report.append("\n Para generar imágenes con Graphviz:\n");
            report.append("dot -Tpng output/graph.dot -o output/graph.png\n");
            report.append("Para todos:\n");
            report.append("for f in output/*.dot; do dot -Tpng \"$f\" -o \"${f%.dot}.png\"; done\n");

            // Try Graphviz for graph.dot automatically
            boolean graphvizOk = tryGraphviz("output/graph.dot", "output/graph.png");
            if (graphvizOk) report.append("\n Graphviz generó output/graph.png automáticamente");
            else            report.append("\n Graphviz no disponible — instálalo para auto-generar imágenes .dot");

            new Alert(Alert.AlertType.INFORMATION, report.toString(), ButtonType.OK).showAndWait();
        } catch (IOException ex) { alert(ex.getMessage()); }
    }

    /** Intenta ejecutar Graphviz (dot) para generar PNG desde un .dot file */
    private boolean tryGraphviz(String dotFile, String pngFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile, "-o", pngFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void info(String msg) {
        lblInfo.setText(" " + msg);
    }
    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void refreshBranchCombo() {
        String prev = cbBranch.getValue();
        cbBranch.getItems().setAll(manager.getAllBranches().stream()
                .map(Branch::getId).toList());
        if (prev != null && cbBranch.getItems().contains(prev)) cbBranch.setValue(prev);
        else if (!cbBranch.getItems().isEmpty()) cbBranch.setValue(cbBranch.getItems().get(0));
    }

    private Button toolBtn(String t, String color) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:#1e1e2e; -fx-font-weight:bold; -fx-font-size:11px;");
        return b;
    }
}
