# AutoClicker Java

Herramienta de automatización de escritorio hecha en Java (Swing + `java.awt.Robot`): autoclicker y grabación/reproducción de acciones del mouse y el teclado, con atajos globales y temas claro/oscuro.

## Modos

La ventana tiene cuatro pestañas. Los atajos de cada modo solo responden mientras su pestaña está abierta.

| Modo | Qué hace | Atajos por defecto |
|---|---|---|
| Autoclicker | Clics repetidos en la posición del mouse | F6 iniciar / detener |
| Mouse | Graba y reproduce movimientos, clics y rueda | F7 grabar · F8 reproducir |
| Teclado | Graba y reproduce solo el teclado | F9 grabar · F10 reproducir |
| Mouse + Teclado | Graba y reproduce mouse y teclado a la vez | F11 grabar · F12 reproducir |

En los modos de grabación se elige cuántas veces repetir (0 = hasta detener). La reproducción respeta los tiempos originales y, si se detiene a mitad, suelta cualquier tecla o botón que hubiera quedado presionado.

## Funciones del autoclicker

- Intervalo configurable en milisegundos
- Número de clics limitado o infinito (0 = infinito)
- Botón izquierdo, derecho o central, con opción de doble clic
- Cuenta regresiva antes de empezar

## Generales

- Atajos globales configurables con cualquier tecla, combinación con Ctrl/Alt/Shift o botón lateral del mouse
- Opciones con selección de tema claro u oscuro
- Los atajos y el tema se recuerdan entre ejecuciones

## Estructura

```
src/autoclicker/
├── Main.java                         Punto de entrada
├── modelo/
│   ├── Atajo.java                    Tecla o botón de un atajo global
│   ├── BotonMouse.java               Botones disponibles
│   ├── ConfiguracionClics.java       Parámetros del autoclicker
│   ├── EventoMacro.java              Acciones grabables (mouse y teclado)
│   ├── Grabacion.java                Secuencia de acciones con su duración
│   └── TipoGrabacion.java            Qué graba cada modo: mouse, teclado o ambos
├── persistencia/
│   └── PreferenciasRepository.java   Guarda atajos y tema (java.util.prefs)
├── servicio/
│   ├── AtajoService.java             Escucha global, atajos y captura (JNativeHook)
│   ├── ClickerService.java           Lógica del autoclicker (Robot + hilo)
│   ├── EntradaListener.java          Pulsaciones que no son atajos
│   ├── EstadoListener.java           Notificaciones de progreso
│   ├── GrabadoraService.java         Graba mouse, teclado o ambos
│   └── ReproductorService.java       Reproduce grabaciones con Robot
└── ui/
    ├── VentanaPrincipal.java         Ventana con pestañas de modos
    ├── PanelAutoclicker.java         Pestaña Autoclicker
    ├── PanelMacro.java               Pestañas de grabación (Mouse, Teclado, Mouse + Teclado)
    ├── ModoPanel.java                Contrato de cada pestaña
    ├── AtajoConfigurable.java        Atajo + selector + preferencias
    ├── Diseno.java                   Utilidades de maquetación
    ├── DialogoOpciones.java          Ventana de opciones
    ├── tema/
    │   ├── Paleta.java               Colores de un tema
    │   ├── Tema.java                 Tema activo, tipografía y medidas
    │   └── TemaVisual.java           Temas claro y oscuro
    └── componentes/
        ├── BotonEngranaje.java       Botón de opciones
        ├── BotonModerno.java         Botón redondeado con estados
        ├── CampoNumerico.java        Campo numérico con botones − y +
        ├── ControlSegmentado.java    Selector segmentado (también las pestañas)
        ├── Etiqueta.java             Texto que sigue el tema
        ├── IndicadorEstado.java      Barra de estado animada
        ├── Logo.java                 Logo e icono de la ventana
        ├── PanelFondo.java           Fondo que sigue el tema
        ├── PanelTarjeta.java         Tarjeta con título
        └── SelectorAtajo.java        Captura y muestra un atajo
lib/
└── jnativehook-2.2.2.jar             Librería para la escucha global
```
## Requisitos

JDK 21 o superior con `javac`, `jar` y `jpackage` en el PATH.

## Generar el ejecutable (Windows)

Doble clic en `construir.bat`. El programa queda en `salida\AutoClicker\AutoClicker.exe`.
Para compartirlo, comprime la carpeta `salida\AutoClicker` completa.

## Créditos

El atajo global usa [JNativeHook](https://github.com/kwhat/jnativehook), distribuida bajo licencia LGPL-3.0.
