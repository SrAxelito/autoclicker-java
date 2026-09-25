#!/usr/bin/env bash
# Compila AutoClicker en macOS o Linux.
# Uso: ./construir.sh [version]      Ejemplo: ./construir.sh 1.5.0
set -euo pipefail
cd "$(dirname "$0")"

VERSION="${1:-1.5.0}"
JNH="lib/jnativehook-2.2.2.jar"
NOMBRE="AutoClicker-$VERSION"
ARQUITECTURA="$(uname -m)"
case "$ARQUITECTURA" in
    x86_64|amd64) ARQUITECTURA="x64" ;;
    aarch64)      ARQUITECTURA="arm64" ;;
esac

echo "Limpiando versiones anteriores..."
rm -rf build dist
mkdir -p build/clases build/jar build/app dist

echo "Compilando..."
javac --release 21 -encoding UTF-8 -cp "$JNH" -d build/clases -sourcepath src src/autoclicker/Main.java

echo "Armando el .jar portable (incluye JNativeHook)..."
(cd build/jar && jar xf "../../$JNH")
rm -f build/jar/META-INF/MANIFEST.MF
cp -R build/clases/autoclicker build/jar/
printf 'Main-Class: autoclicker.Main\nMulti-Release: true\n' > build/MANIFEST.MF
jar cfm "dist/$NOMBRE-portable.jar" build/MANIFEST.MF -C build/jar .
cp "dist/$NOMBRE-portable.jar" build/app/AutoClicker.jar

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
echo "  $NOMBRE-portable.jar   (cualquier sistema con Java 21 o superior)"
