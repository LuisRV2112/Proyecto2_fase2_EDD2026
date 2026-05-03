package edu.usac.edd.csv;

import edu.usac.edd.catalog.BranchManager;
import edu.usac.edd.model.Branch;
import edu.usac.edd.model.Product;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * Cargador robusto de los 3 tipos de CSV:
 *   1. sucursales.csv
 *   2. conexiones.csv
 *   3. productos.csv
 *
 * Valida, loggea errores en errors.log, nunca detiene la carga por línea mala.
 */
public class CSVLoader {

    public static class LoadResult {
        public int loaded, skipped, errors;
        public String message() {
            return String.format("Cargados: %d | Omitidos: %d | Errores: %d",
                    loaded, skipped, errors);
        }
    }

    private final BranchManager manager;
    private final String        errorLogPath;

    public CSVLoader(BranchManager manager, String errorLogPath) {
        this.manager      = manager;
        this.errorLogPath = errorLogPath;
    }

    // ── Sucursales ────────────────────────────────────────────────────────
    // "ID","Nombre","Ubicación","t_ingreso","t_traspaso","t_despacho"
    public LoadResult loadBranches(String filepath) {
        LoadResult r = new LoadResult();
        processCSV(filepath, (fields, lineNum) -> {
            if (fields.length < 6) {
                log("SUCURSAL linea " + lineNum + ": campos insuficientes");
                r.errors++; return;
            }
            try {
                String id  = fields[0], name = fields[1], loc = fields[2];
                int ti = Integer.parseInt(fields[3]);
                int tt = Integer.parseInt(fields[4]);
                int td = Integer.parseInt(fields[5]);
                if (manager.getBranch(id) != null) { r.skipped++; return; }
                manager.addBranch(new Branch(id, name, loc, ti, tt, td));
                r.loaded++;
            } catch (Exception e) {
                log("SUCURSAL linea " + lineNum + ": " + e.getMessage());
                r.errors++;
            }
        });
        return r;
    }

    // ── Conexiones ────────────────────────────────────────────────────────
    // "OrigenID","DestinoID","Tiempo","Costo"
    public LoadResult loadConnections(String filepath) {
        LoadResult r = new LoadResult();
        processCSV(filepath, (fields, lineNum) -> {
            if (fields.length < 4) {
                log("CONEXION linea " + lineNum + ": campos insuficientes");
                r.errors++; return;
            }
            try {
                String from = fields[0], to = fields[1];
                double time = Double.parseDouble(fields[2]);
                double cost = Double.parseDouble(fields[3]);
                boolean bidir = fields.length >= 5 &&
                                fields[4].trim().equalsIgnoreCase("true");
                manager.addConnection(from, to, time, cost, bidir);
                r.loaded++;
            } catch (Exception e) {
                log("CONEXION linea " + lineNum + ": " + e.getMessage());
                r.errors++;
            }
        });
        return r;
    }

    // ── Productos ─────────────────────────────────────────────────────────
    // "SucursalID","Nombre","CodigoBarra","Categoria","FechaCaducidad","Marca","Precio","Stock"
    public LoadResult loadProducts(String filepath) {
        LoadResult r = new LoadResult();
        processCSV(filepath, (fields, lineNum) -> {
            if (fields.length < 8) {
                log("PRODUCTO linea " + lineNum + ": campos insuficientes");
                r.errors++; return;
            }
            try {
                String branchId = fields[0];
                String name     = fields[1];
                String barcode  = fields[2];
                String category = fields[3];
                String expiry   = fields[4];
                String brand    = fields[5];
                double price    = Double.parseDouble(fields[6]);
                int    stock    = Integer.parseInt(fields[7]);

                if (name.isEmpty() || barcode.isEmpty()) {
                    log("PRODUCTO linea " + lineNum + ": nombre/barcode vacíos");
                    r.errors++; return;
                }
                Product p = new Product(name, barcode, category, expiry, brand, price, stock);
                if (!manager.addProduct(branchId, p)) {
                    log("PRODUCTO linea " + lineNum + ": duplicado o sucursal no existe: " + barcode);
                    r.skipped++; return;
                }
                r.loaded++;
            } catch (NumberFormatException e) {
                log("PRODUCTO linea " + lineNum + ": precio/stock no numérico");
                r.errors++;
            } catch (Exception e) {
                log("PRODUCTO linea " + lineNum + ": " + e.getMessage());
                r.errors++;
            }
        });
        return r;
    }

    // ── Motor de procesamiento ────────────────────────────────────────────
    private void processCSV(String filepath, BiConsumer<String[], Integer> handler) {
        File f = new File(filepath);
        if (!f.exists() || !f.canRead()) {
            log("Archivo no encontrado o ilegible: " + filepath);
            return;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                if (firstLine) { firstLine = false; continue; } // saltar header
                String[] fields = parseLine(line);
                handler.accept(fields, lineNum);
            }
        } catch (IOException e) {
            log("Error leyendo " + filepath + ": " + e.getMessage());
        }
    }

    /** Parser CSV que respeta comillas dobles */
    private String[] parseLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuote = !inQuote; }
            else if (c == ',' && !inQuote) {
                fields.add(cur.toString().trim());
                cur.setLength(0);
            } else { cur.append(c); }
        }
        fields.add(cur.toString().trim());
        return fields.toArray(new String[0]);
    }

    private void log(String msg) {
        try (PrintWriter pw = new PrintWriter(
                new FileWriter(errorLogPath, true))) {
            pw.println("[" + java.time.LocalDateTime.now() + "] " + msg);
        } catch (IOException ignored) {}
        System.err.println("[CSV ERROR] " + msg);
    }
}
