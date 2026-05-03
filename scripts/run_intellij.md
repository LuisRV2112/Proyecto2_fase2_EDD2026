# Instrucciones para IntelliJ IDEA 2025.3.3

## Pasos para ejecutar en IntelliJ

1. **Abrir proyecto:**
   - File → Open → seleccionar carpeta `supermarket-phase2/`
   - IntelliJ detecta el `pom.xml` automáticamente
   - Clic en "Trust Project" si se lo pide

2. **Esperar descarga de dependencias:**
   - Maven descargará JavaFX 21 automáticamente (~50 MB)
   - Ver progreso en la barra inferior

3. **Ejecutar:**
   - Opción A: Clic derecho sobre `MainApp.java` → Run 'MainApp.main()'
   - Opción B: Maven panel (derecha) → Plugins → javafx → javafx:run
   - Opción C: Terminal integrada → `mvn javafx:run`

4. **Si pide configurar SDK:**
   - File → Project Structure → Project → SDK → Java 21

5. **Cargar datos al iniciar:**
   - Tab "Dashboard" → sección "Carga de Archivos CSV"
   - Seleccionar en orden: sucursales.csv, conexiones.csv, productos.csv
   - O usar "Cargar Todo" con rutas manuales
