# AutoClicker Java

Autoclicker de escritorio hecho en Java puro (Swing + `java.awt.Robot`), sin librerías externas.

## Funciones

- Intervalo configurable en milisegundos
- Número de clics limitado o infinito (0 = infinito)
- Botón izquierdo, derecho o central, con opción de doble clic
- Cuenta regresiva antes de empezar
- Freno de seguridad: llevar el mouse a la esquina superior izquierda lo detiene

## Estructura

```
src/autoclicker/
├── Main.java                      Punto de entrada
├── modelo/
│   ├── BotonMouse.java            Botones disponibles
│   └── ConfiguracionClics.java    Parámetros de una sesión de clics
├── servicio/
│   ├── ClickerListener.java       Notificaciones de progreso
│   └── ClickerService.java        Lógica de clics (Robot + hilo)
└── ui/
    └── VentanaAutoClicker.java    Interfaz gráfica Swing
```

## Requisitos

JDK 21 o superior con `javac`, `jar` y `jpackage` en el PATH.

## Generar el ejecutable (Windows)

Doble clic en `construir.bat`. El programa queda en `salida\AutoClicker\AutoClicker.exe`.
Para compartirlo, comprime la carpeta `salida\AutoClicker` completa.
