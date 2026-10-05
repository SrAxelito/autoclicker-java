#!/usr/bin/env bash
# Compila el programa y sus pruebas, y ejecuta las pruebas con JUnit 5 (macOS o Linux).
# Uso: ./probar.sh
set -euo pipefail
cd "$(dirname "$0")"

JNH="lib/jnativehook-2.2.2.jar"
JUNIT_VERSION="1.11.4"
JUNIT="lib/pruebas/junit-platform-console-standalone-$JUNIT_VERSION.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"

# JUnit no se guarda en el repositorio: se descarga la primera vez.
if [ ! -f "$JUNIT" ]; then
    echo "Descargando JUnit $JUNIT_VERSION (solo la primera vez)..."
    mkdir -p lib/pruebas
    curl -fsSL -o "$JUNIT.descarga" "$JUNIT_URL"
    mv "$JUNIT.descarga" "$JUNIT"
fi

echo "Compilando el programa..."
rm -rf build/pruebas
mkdir -p build/pruebas/app build/pruebas/tests
javac --release 21 -encoding UTF-8 -cp "$JNH" -d build/pruebas/app -sourcepath src src/autoclicker/Main.java

# Si agregas pruebas en un paquete nuevo, añade aquí su carpeta.
echo "Compilando las pruebas..."
javac --release 21 -encoding UTF-8 -cp "build/pruebas/app:$JNH:$JUNIT" -d build/pruebas/tests \
    test/autoclicker/modelo/*.java test/autoclicker/persistencia/*.java test/autoclicker/servicio/*.java

# headless: las pruebas no abren ventanas ni mueven el mouse, así corren en cualquier máquina.
echo "Ejecutando las pruebas..."
java -Djava.awt.headless=true -jar "$JUNIT" execute \
    --class-path "build/pruebas/tests:build/pruebas/app:$JNH" \
    --scan-class-path=build/pruebas/tests \
    --fail-if-no-tests --details=tree --details-theme=ascii --disable-banner --disable-ansi-colors

echo
echo "Todas las pruebas pasaron."