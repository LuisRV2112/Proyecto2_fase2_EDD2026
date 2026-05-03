# Gestión de Catálogo de Supermercado — Fase 2
### Estructuras de Datos EDD 2026-1S | USAC CUNOC
### Lenguaje: Java 21 + JavaFX 21

---

## Nuevas estructuras (Fase 2)

| Estructura   | Propósito                                      | Big-O              |
|-------------|------------------------------------------------|--------------------|
| Cola (Queue) | Despacho FIFO por sucursal (3 etapas)          | O(1) enqueue/dequeue|
| Pila (Stack) | Rollback de operaciones                        | O(1) push/pop      |
| Grafo        | Red de sucursales ponderada                    | —                  |
| Dijkstra     | Ruta óptima (tiempo o costo)                   | O((V+E)·log V)     |

---

## Instalación de JavaFX en Fedora

```bash
# Opción 1: Con Maven (recomendado — el proyecto lo descarga automáticamente)
sudo dnf install maven
mvn javafx:run

# Opción 2: SDK manual
# Descargar JavaFX 21 SDK de: https://gluonhq.com/products/javafx/
# Extraer en ~/javafx-sdk-21
```

## Requisitos

- Java 21+ (`java -version`)
- Maven 3.8+ (`mvn -version`)
- Graphviz (opcional, para exportar .dot → .png): `sudo dnf install graphviz`

---

## Compilación y ejecución

```bash
cd supermarket-phase2

# Compilar y ejecutar (Maven descarga JavaFX automáticamente)
mvn javafx:run

# Solo compilar
mvn compile

# Crear JAR ejecutable
mvn package
```

### Abrir en IntelliJ IDEA

1. **File → Open** → seleccionar la carpeta `supermarket-phase2/`
2. IntelliJ detecta el `pom.xml` automáticamente
3. Ejecutar `MainApp.java` con botón ▶
4. O usar la configuración Maven: `javafx:run`

---

## Flujo de uso

### 1. Cargar datos (tab Dashboard)
```
sucursales.csv  → primero
conexiones.csv  → segundo
productos.csv   → tercero
```

### 2. Gestionar sucursales (tab Sucursales)
- Agregar / editar / eliminar sucursales
- Campos: ID, Nombre, Ubicación, tiempos de procesamiento

### 3. Gestionar productos (tab Productos)
- Buscar por: nombre (AVL), código (Hash), categoría (B+), rango fechas (B-tree)
- Agregar / eliminar / deshacer (pila rollback)

### 4. Transferencias (tab Transferencias)
- Elegir sucursal origen y destino
- Criterio: tiempo mínimo o costo mínimo
- Sistema calcula ruta con Dijkstra
- Simulación en tiempo real con las 3 colas (ingreso, traspaso, salida)

### 5. Red de sucursales (tab Red)
- Grafo visual interactivo (nodos arrastrables)
- Aristas con peso tiempo y costo
- Ruta óptima resaltada en verde
- Exportar DOT → `output/graph.dot`

### 6. Árboles (tab Árboles)
- Visualización gráfica de AVL, B-tree y B+ tree por sucursal
- Exportar DOT para cada árbol

### 7. Rendimiento (tab Rendimiento)
- Benchmark automático: N=20 consultas × M=5 repeticiones
- Compara: Lista no ordenada vs AVL vs Hash
- Gráfica de barras comparativa
- Tabla de Big-O teórico

---

## Formatos CSV

### sucursales.csv
```
"ID","Nombre","Ubicación","t_ingreso","t_traspaso","t_despacho"
"S01","Central","Ciudad de Guatemala",5,8,10
```

### conexiones.csv
```
"OrigenID","DestinoID","Tiempo","Costo"
"S01","S02",120,50
```

### productos.csv
```
"SucursalID","Nombre","CodigoBarra","Categoria","FechaCaducidad","Marca","Precio","Stock"
"S01","Leche entera NaturVida","1000001","Lacteos","2026-06-15","NaturVida",12.50,100
```

---

## Estructura del proyecto

```
supermarket-phase2/
├── src/main/java/edu/usac/edd/
│   ├── model/          Product, Branch, Transfer
│   ├── structures/     LinkedList, Stack, Queue, AVLTree, BTree, BPlusTree, HashTable
│   ├── graph/          BranchGraph (Dijkstra desde cero)
│   ├── catalog/        Catalog, BranchManager
│   ├── simulation/     Dispatcher (simulación de colas)
│   ├── csv/            CSVLoader
│   └── gui/
│       ├── MainApp.java
│       └── views/      Dashboard, Branch, Product, Transfer, Graph, Tree, Benchmark
├── src/main/resources/css/style.css
├── data/
│   ├── sucursales.csv
│   ├── conexiones.csv
│   └── productos.csv
├── output/             (archivos .dot generados aquí)
├── pom.xml
└── README.md
```

---

## Convertir .dot a imágenes

```bash
sudo dnf install graphviz
dot -Tpng output/graph.dot      -o output/graph.png
dot -Tpng output/avl_S01.dot    -o output/avl_S01.png
dot -Tpng output/btree_S01.dot  -o output/btree_S01.png
dot -Tpng output/bplus_S01.dot  -o output/bplus_S01.png
```
