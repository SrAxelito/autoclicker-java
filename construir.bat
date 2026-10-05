@echo off
setlocal
rem Uso: construir.bat [version]      Ejemplo: construir.bat 1.6.0

rem Trabaja siempre desde la carpeta de este archivo, se ejecute desde donde se ejecute.
cd /d "%~dp0"

set VERSION=%~1
if "%VERSION%"=="" set VERSION=1.6.0
set JNH=jnativehook-2.2.2.jar
set NOMBRE=AutoClicker-%VERSION%

echo Limpiando versiones anteriores...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\clases build\app build\portable dist

echo Compilando...
javac --release 21 -encoding UTF-8 -cp lib\%JNH% -d build\clases -sourcepath src src\autoclicker\Main.java || goto error

rem JNativeHook (LGPL) va como archivo aparte, sin mezclarlo con el codigo de
rem AutoClicker, para que cualquiera pueda reemplazarlo. Class-Path le dice a
rem Java que lo busque junto a AutoClicker.jar.
echo Armando AutoClicker.jar...
> build\MANIFEST.MF echo Main-Class: autoclicker.Main
>> build\MANIFEST.MF echo Class-Path: %JNH%
jar cfm build\app\AutoClicker.jar build\MANIFEST.MF -C build\clases . || goto error
copy /y lib\%JNH% build\app\ >nul || goto error
copy /y LICENSE build\app\ >nul || goto error
xcopy /e /i /q licencias build\app\licencias >nul || goto error

echo Armando la version portable...
xcopy /e /i /q build\app build\portable\%NOMBRE%-portable >nul || goto error
jar cMf dist\%NOMBRE%-portable.zip -C build\portable %NOMBRE%-portable || goto error

echo Generando la aplicacion para Windows (incluye Java)...
jpackage --type app-image --input build\app --main-jar AutoClicker.jar --main-class autoclicker.Main ^
    --name AutoClicker --app-version %VERSION% --vendor "AutoClicker Java" ^
    --description "Autoclicker y grabador de acciones" --icon recursos\icono.ico ^
    --add-modules "java.desktop,java.prefs,java.logging,jdk.localedata" ^
    --jlink-options "--strip-debug --no-man-pages --no-header-files --strip-native-commands --include-locales=en,es" ^
    --dest build\salida || goto error

echo Comprimiendo...
powershell -NoProfile -Command "Compress-Archive -Path 'build\salida\AutoClicker' -DestinationPath 'dist\%NOMBRE%-windows-x64.zip' -Force" || goto error

echo.
echo Listo. En la carpeta dist:
echo   %NOMBRE%-windows-x64.zip   (descomprimir y abrir AutoClicker.exe, no necesita Java)
echo   %NOMBRE%-portable.zip      (descomprimir y abrir AutoClicker.jar; necesita Java 21 o superior)
echo Para probar sin comprimir: build\salida\AutoClicker\AutoClicker.exe
if not defined CI pause
exit /b 0

:error
echo.
echo Hubo un error, revisa el mensaje de arriba.
if not defined CI pause
exit /b 1