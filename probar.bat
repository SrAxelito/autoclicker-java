@echo off
setlocal
rem Compila el programa y sus pruebas, y ejecuta las pruebas con JUnit 5.
rem Uso: probar.bat  (o doble clic)
set JNH=lib\jnativehook-2.2.2.jar
set JUNIT_VERSION=1.11.4
set JUNIT=lib\pruebas\junit-platform-console-standalone-%JUNIT_VERSION%.jar
set JUNIT_URL=https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/%JUNIT_VERSION%/junit-platform-console-standalone-%JUNIT_VERSION%.jar

rem JUnit no se guarda en el repositorio: se descarga la primera vez.
if exist "%JUNIT%" goto compilar
echo Descargando JUnit %JUNIT_VERSION%, solo la primera vez...
if not exist lib\pruebas mkdir lib\pruebas
curl -fsSL -o "%JUNIT%.descarga" "%JUNIT_URL%" || goto error
move /y "%JUNIT%.descarga" "%JUNIT%" >nul || goto error

:compilar
echo Compilando el programa...
if exist build\pruebas rmdir /s /q build\pruebas
mkdir build\pruebas\app build\pruebas\tests
javac --release 21 -encoding UTF-8 -cp %JNH% -d build\pruebas\app -sourcepath src src\autoclicker\Main.java || goto error

rem Si agregas pruebas en un paquete nuevo, anade aqui su carpeta.
echo Compilando las pruebas...
javac --release 21 -encoding UTF-8 -cp "build\pruebas\app;%JNH%;%JUNIT%" -d build\pruebas\tests ^
    test\autoclicker\modelo\*.java test\autoclicker\persistencia\*.java test\autoclicker\servicio\*.java || goto error

rem headless: las pruebas no abren ventanas ni mueven el mouse, asi corren en cualquier maquina.
echo Ejecutando las pruebas...
java -Djava.awt.headless=true -jar "%JUNIT%" execute ^
    --class-path "build\pruebas\tests;build\pruebas\app;%JNH%" ^
    --scan-class-path=build\pruebas\tests ^
    --fail-if-no-tests --details=tree --details-theme=ascii --disable-banner --disable-ansi-colors || goto error

echo.
echo Todas las pruebas pasaron.
if not defined CI pause
exit /b 0

:error
echo.
echo Hubo un error o alguna prueba fallo, revisa el mensaje de arriba.
if not defined CI pause
exit /b 1