@echo off
setlocal
rem Uso: construir.bat [version]      Ejemplo: construir.bat 1.5.0
set VERSION=%~1
if "%VERSION%"=="" set VERSION=1.5.0
set JNH=lib\jnativehook-2.2.2.jar
set NOMBRE=AutoClicker-%VERSION%

echo Limpiando versiones anteriores...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\clases build\jar build\app dist

echo Compilando...
javac --release 21 -encoding UTF-8 -cp %JNH% -d build\clases -sourcepath src src\autoclicker\Main.java || goto error

echo Armando el .jar portable (incluye JNativeHook)...
pushd build\jar
jar xf ..\..\%JNH%
if errorlevel 1 (popd & goto error)
popd
del /q build\jar\META-INF\MANIFEST.MF
xcopy /e /i /q build\clases\autoclicker build\jar\autoclicker >nul || goto error
> build\MANIFEST.MF echo Main-Class: autoclicker.Main
>> build\MANIFEST.MF echo Multi-Release: true
jar cfm dist\%NOMBRE%-portable.jar build\MANIFEST.MF -C build\jar . || goto error
copy /y dist\%NOMBRE%-portable.jar build\app\AutoClicker.jar >nul || goto error

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
echo   %NOMBRE%-portable.jar      (cualquier sistema con Java 21 o superior)
echo Para probar sin comprimir: build\salida\AutoClicker\AutoClicker.exe
if not defined CI pause
exit /b 0

:error
echo.
echo Hubo un error, revisa el mensaje de arriba.
if not defined CI pause
exit /b 1
