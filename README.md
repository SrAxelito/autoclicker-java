# AutoClicker Java

Autoclicker de escritorio hecho en Java puro (Swing + `java.awt.Robot`), sin librerías externas, con una interfaz moderna de componentes propios.

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
    ├── VentanaAutoClicker.java    Ventana principal
    ├── tema/
    │   └── Tema.java              Colores, tipografía y medidas
    └── componentes/
        ├── BotonModerno.java      Botón redondeado con estados
        ├── CampoNumerico.java     Campo numérico con botones − y +
        ├── ControlSegmentado.java Selector de opciones en segmentos
        ├── IndicadorEstado.java   Barra de estado animada
        ├── Logo.java              Logo e icono de la ventana
        └── PanelTarjeta.java      Tarjeta con título
```
## Requisitos

JDK 21 o superior con `javac`, `jar` y `jpackage` en el PATH.

## Generar el ejecutable (Windows)

Doble clic en `construir.bat`. El programa queda en `salida\AutoClicker\AutoClicker.exe`.
Para compartirlo, comprime la carpeta `salida\AutoClicker` completa.
