@echo off
setlocal
set JNH=jnativehook-2.2.2.jar

echo Limpiando versiones anteriores...
if exist out rmdir /s /q out
if exist app rmdir /s /q app
if exist salida rmdir /s /q salida

echo Compilando...
javac -encoding UTF-8 -cp lib\%JNH% -d out -sourcepath src src\autoclicker\Main.java || goto error

echo Empaquetando .jar...
mkdir app
> out\MANIFEST.MF echo Main-Class: autoclicker.Main
>> out\MANIFEST.MF echo Class-Path: %JNH%
jar cfm app\AutoClicker.jar out\MANIFEST.MF -C out autoclicker || goto error
copy /y lib\%JNH% app\ >nul || goto error

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
