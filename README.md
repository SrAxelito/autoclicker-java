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
recursos/
├── icono.ico / icono.icns / icono.png   Iconos de la app para cada sistema
construir.bat / construir.sh          Scripts de compilación
.github/workflows/compilar.yml        Compilación y publicación automática
```
## Descargar y usar

Descarga la última versión desde la sección **Releases** del repositorio. Hay una opción para cada sistema:

| Sistema | Archivo | Necesita Java |
|---|---|---|
| Windows | `AutoClicker-X.Y.Z-windows-x64.zip` | No |
| macOS (Apple Silicon) | `AutoClicker-X.Y.Z-macos-arm64.zip` | No |
| Linux | `AutoClicker-X.Y.Z-linux-x64.tar.gz` | No |
| Cualquiera (incluye Mac Intel) | `AutoClicker-X.Y.Z-portable.jar` | Sí, Java 21 o superior |

### Windows

1. Descomprime el `.zip` en cualquier carpeta (Escritorio, Documentos, una memoria USB…).
2. Abre `AutoClicker.exe`.
3. La primera vez, Windows puede mostrar "Windows protegió su PC" porque la aplicación no está firmada. Haz clic en **Más información → Ejecutar de todas formas**.

### macOS

1. Descomprime el `.zip` y mueve `AutoClicker.app` a Aplicaciones.
2. La primera vez, haz clic derecho sobre la app → **Abrir** (la app no está firmada por Apple).
3. Ve a **Configuración del Sistema → Privacidad y seguridad** y activa AutoClicker en **Accesibilidad** y en **Monitoreo de entrada**. Sin esos permisos no puede mover el mouse ni escuchar los atajos.

### Linux

1. Descomprime con `tar -xzf AutoClicker-X.Y.Z-linux-x64.tar.gz`.
2. Ejecuta `AutoClicker/bin/AutoClicker`.
3. Funciona en sesiones **X11**. En Wayland, el sistema bloquea que las aplicaciones controlen el mouse y lean el teclado de forma global; en ese caso elige "Ubuntu en Xorg" (o equivalente) en la pantalla de inicio de sesión.

### Versión portable (.jar)

Con Java 21 o superior instalado, se abre con doble clic o con `java -jar AutoClicker-X.Y.Z-portable.jar`. Es la opción para sistemas sin paquete propio, como los Mac con procesador Intel.

## Datos que guarda

- **Preferencias** (atajos y tema): con `java.util.prefs`, en el registro del usuario en Windows y en la carpeta de preferencias del usuario en macOS y Linux.
- **Librería nativa de JNativeHook**: se extrae en `%LOCALAPPDATA%\AutoClicker` (Windows), `~/Library/Application Support/AutoClicker` (macOS) o `~/.local/share/autoclicker` (Linux), para que funcione aunque el programa esté en una carpeta sin permisos de escritura.

## Compilar

Requisitos: JDK 21 o superior con `javac`, `jar` y `jpackage` en el PATH.

| Sistema | Comando |
|---|---|
| Windows | `construir.bat` (o doble clic) |
| macOS / Linux | `./construir.sh` |

Se puede indicar la versión: `construir.bat 1.5.0` o `./construir.sh 1.5.0`. El resultado queda en `dist/`, y la aplicación sin comprimir en `build/salida/` para probarla. `jpackage` solo genera la aplicación del sistema en el que se ejecuta.

## Publicar una versión

El workflow `.github/workflows/compilar.yml` compila en Windows, macOS y Linux en cada push a `main` o `develop` y en cada Pull Request. Al subir una etiqueta, además crea el Release con los archivos de los tres sistemas:

```
git checkout main
git pull
git tag -a v1.5.0 -m "Versión 1.5.0"
git push origin v1.5.0
```

## Créditos

El atajo global usa [JNativeHook](https://github.com/kwhat/jnativehook), distribuida bajo licencia LGPL-3.0.
