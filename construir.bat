@echo off
echo Limpiando versiones anteriores...
if exist out rmdir /s /q out
if exist app rmdir /s /q app
if exist salida rmdir /s /q salida

echo Compilando...
javac -d out -sourcepath src src\autoclicker\Main.java || goto error

echo Empaquetando .jar...
mkdir app
jar cfe app\AutoClicker.jar autoclicker.Main -C out . || goto error

echo Generando el .exe...
jpackage --type app-image --input app --main-jar AutoClicker.jar --name AutoClicker --dest salida || goto error

echo.
echo Listo: salida\AutoClicker\AutoClicker.exe
pause
exit /b 0

:error
echo.
echo Hubo un error, revisa el mensaje de arriba.
pause
exit /b 1
