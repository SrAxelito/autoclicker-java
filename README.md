# AutoClicker Java

Autoclicker de escritorio hecho en Java (Swing + `java.awt.Robot`), con interfaz moderna de componentes propios, atajo global y temas claro/oscuro.

## Funciones

- Intervalo configurable en milisegundos
- Número de clics limitado o infinito (0 = infinito)
- Botón izquierdo, derecho o central, con opción de doble clic
- Cuenta regresiva antes de empezar
- Atajo global para iniciar y detener (F6 por defecto), configurable con cualquier tecla, combinación con Ctrl/Alt/Shift o botón lateral del mouse
- Opciones con selección de tema claro u oscuro
- El atajo y el tema se recuerdan entre ejecuciones

## Estructura

```
src/autoclicker/
├── Main.java                         Punto de entrada
├── modelo/
│   ├── Atajo.java                    Tecla o botón del atajo global
│   ├── BotonMouse.java               Botones disponibles
│   └── ConfiguracionClics.java       Parámetros de una sesión de clics
├── persistencia/
│   └── PreferenciasRepository.java   Guarda atajo y tema (java.util.prefs)
├── servicio/
│   ├── AtajoService.java             Escucha global de teclado y mouse (JNativeHook)
│   ├── ClickerListener.java          Notificaciones de progreso
│   └── ClickerService.java           Lógica de clics (Robot + hilo)
└── ui/
    ├── VentanaAutoClicker.java       Ventana principal
    ├── DialogoOpciones.java          Ventana de opciones
    ├── tema/
    │   ├── Paleta.java               Colores de un tema
    │   ├── Tema.java                 Tema activo, tipografía y medidas
    │   └── TemaVisual.java           Temas claro y oscuro
    └── componentes/
        ├── BotonEngranaje.java       Botón de opciones
        ├── BotonModerno.java         Botón redondeado con estados
        ├── CampoNumerico.java        Campo numérico con botones − y +
        ├── ControlSegmentado.java    Selector de opciones en segmentos
        ├── Etiqueta.java             Texto que sigue el tema
        ├── IndicadorEstado.java      Barra de estado animada
        ├── Logo.java                 Logo e icono de la ventana
        ├── PanelFondo.java           Fondo que sigue el tema
        ├── PanelTarjeta.java         Tarjeta con título
        └── SelectorAtajo.java        Captura y muestra el atajo
lib/
└── jnativehook-2.2.2.jar             Librería para el atajo global
```

## Requisitos

JDK 21 o superior con `javac`, `jar` y `jpackage` en el PATH.

## Generar el ejecutable (Windows)

Doble clic en `construir.bat`. El programa queda en `salida\AutoClicker\AutoClicker.exe`.
Para compartirlo, comprime la carpeta `salida\AutoClicker` completa.

## Créditos

El atajo global usa [JNativeHook](https://github.com/kwhat/jnativehook), distribuida bajo licencia LGPL-3.0.
