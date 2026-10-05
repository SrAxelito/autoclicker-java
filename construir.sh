#!/usr/bin/env bash
# Compila AutoClicker en macOS o Linux.
# Uso: ./construir.sh [version]      Ejemplo: ./construir.sh 1.6.0
set -euo pipefail
cd "$(dirname "$0")"

VERSION="${1:-1.6.0}"
JNH="jnativehook-2.2.2.jar"
NOMBRE="AutoClicker-$VERSION"
ARQUITECTURA="$(uname -m)"
case "$ARQUITECTURA" in
    x86_64|amd64) ARQUITECTURA="x64" ;;
    aarch64)      ARQUITECTURA="arm64" ;;
esac

echo "Limpiando versiones anteriores..."
rm -rf build dist
mkdir -p build/clases build/app build/portable dist

echo "Compilando..."
javac --release 21 -encoding UTF-8 -cp "lib/$JNH" -d build/clases -sourcepath src src/autoclicker/Main.java

# JNativeHook (LGPL) va como archivo aparte, sin mezclarlo con el código de
# AutoClicker, para que cualquiera pueda reemplazarlo. Class-Path le dice a
# Java que lo busque junto a AutoClicker.jar.
echo "Armando AutoClicker.jar..."
printf 'Main-Class: autoclicker.Main\nClass-Path: %s\n' "$JNH" > build/MANIFEST.MF
jar cfm build/app/AutoClicker.jar build/MANIFEST.MF -C build/clases .
cp "lib/$JNH" LICENSE build/app/
cp -R licencias build/app/

echo "Armando la versión portable..."
cp -R build/app "build/portable/$NOMBRE-portable"
jar cMf "dist/$NOMBRE-portable.zip" -C build/portable "$NOMBRE-portable"

COMUNES=(--type app-image --input build/app --main-jar AutoClicker.jar --main-class autoclicker.Main
         --name AutoClicker --app-version "$VERSION" --vendor "AutoClicker Java"
         --description "Autoclicker y grabador de acciones" --dest build/salida
         # Solo los módulos de Java que usa el programa (la app pesa casi la mitad)
         --add-modules "java.desktop,java.prefs,java.logging,jdk.localedata"
         --jlink-options "--strip-debug --no-man-pages --no-header-files --strip-native-commands --include-locales=en,es")

if [ "$(uname -s)" = "Darwin" ]; then
    echo "Generando la aplicación para macOS (incluye Java)..."
    jpackage "${COMUNES[@]}" --icon recursos/icono.icns --mac-package-identifier autoclicker.java
    ditto -c -k --keepParent build/salida/AutoClicker.app "dist/$NOMBRE-macos-$ARQUITECTURA.zip"
    PAQUETE="$NOMBRE-macos-$ARQUITECTURA.zip   (descomprimir y abrir AutoClicker.app)"
else
    echo "Generando la aplicación para Linux (incluye Java)..."
    jpackage "${COMUNES[@]}" --icon recursos/icono.png
    tar -czf "dist/$NOMBRE-linux-$ARQUITECTURA.tar.gz" -C build/salida AutoClicker
    PAQUETE="$NOMBRE-linux-$ARQUITECTURA.tar.gz   (descomprimir y ejecutar AutoClicker/bin/AutoClicker)"
fi

echo
echo "Listo. En la carpeta dist:"
echo "  $PAQUETE"
echo "  $NOMBRE-portable.zip   (descomprimir y abrir AutoClicker.jar; necesita Java 21 o superior)"