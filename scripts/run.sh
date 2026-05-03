#!/bin/bash
# Script de ejecución para Fedora (Maven descarga JavaFX automáticamente)
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"
echo "Compilando y ejecutando Gestión de Catálogo — Fase 2..."
mvn javafx:run -q
