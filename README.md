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

- Intervalo configurable en milisegundos, desde 1 ms
- Número de clics limitado o infinito (0 = infinito)
- Botón izquierdo, derecho o central, con opción de doble clic
- Cuenta regresiva antes de empezar

## Generales

- Atajos globales configurables con cualquier tecla, combinación con Ctrl/Alt/Shift o botón lateral del mouse
- Opciones con selección de tema claro u oscuro
- Los atajos, el tema y los parámetros de cada pestaña se recuerdan entre ejecuciones
- La ventana se puede cambiar de tamaño y se ajusta sola a pantallas pequeñas: si las opciones no caben, se desplazan, y el estado y los botones quedan siempre a la vista

## Descargar y usar

Descarga la última versión desde la sección **Releases** del repositorio. Hay una opción para cada sistema:

| Sistema | Archivo | Necesita Java |
|---|---|---|
| Windows | `AutoClicker-X.Y.Z-windows-x64.zip` | No |
| macOS (Apple Silicon) | `AutoClicker-X.Y.Z-macos-arm64.zip` | No |
| Linux | `AutoClicker-X.Y.Z-linux-x64.tar.gz` | No |
| Cualquiera (incluye Mac Intel) | `AutoClicker-X.Y.Z-portable.zip` | Sí, Java 21 o superior |

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

### Versión portable

Es la opción para sistemas sin paquete propio, como los Mac con procesador Intel. Necesita Java 21 o superior instalado.

1. Descomprime el `.zip`. Queda una carpeta con `AutoClicker.jar`, `jnativehook-2.2.2.jar` y las licencias.
2. Abre `AutoClicker.jar` con doble clic, o con `java -jar AutoClicker.jar` desde esa carpeta.

Los dos `.jar` deben quedar juntos: si mueves `AutoClicker.jar` a otro lugar sin el otro, no abre.

## Datos que guarda

- **Preferencias** (atajos, tema y parámetros de cada pestaña): con `java.util.prefs`, en el registro del usuario en Windows y en la carpeta de preferencias del usuario en macOS y Linux.
- **Librería nativa de JNativeHook**: se extrae en `%LOCALAPPDATA%\AutoClicker\nativo` (Windows), `~/Library/Application Support/AutoClicker/nativo` (macOS) o `~/.local/share/autoclicker/nativo` (Linux), para que funcione aunque el programa esté en una carpeta sin permisos de escritura.

Las grabaciones no se guardan: se pierden al cerrar el programa.

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
│   └── PreferenciasRepository.java   Guarda atajos, tema y parámetros (java.util.prefs)
├── servicio/
│   ├── AtajoService.java             Escucha global, atajos y captura (JNativeHook)
│   ├── ClickerService.java           Lógica del autoclicker (Robot + hilo)
│   ├── EntradaListener.java          Pulsaciones que no son atajos, con su hora de llegada
│   ├── EstadoListener.java           Notificaciones de progreso
│   ├── GrabadoraService.java         Graba mouse, teclado o ambos
│   └── ReproductorService.java       Reproduce grabaciones con Robot
└── ui/
    ├── VentanaPrincipal.java         Ventana con pestañas de modos
    ├── ModoPanel.java                Contrato de cada pestaña
    ├── PanelModo.java                Base con lo común de las pestañas
    ├── PanelAutoclicker.java         Pestaña Autoclicker
    ├── PanelMacro.java               Pestañas de grabación (Mouse, Teclado, Mouse + Teclado)
    ├── ModoGrabacion.java            Textos, claves, atajos y servicios de cada pestaña de grabación
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
        ├── PanelDesplazable.java     Zona que se desplaza cuando su contenido no cabe
        ├── PanelFondo.java           Fondo que sigue el tema
        ├── PanelTarjeta.java         Tarjeta con título
        └── SelectorAtajo.java        Captura y muestra un atajo
test/autoclicker/                     Pruebas, con los mismos paquetes que src
lib/
└── jnativehook-2.2.2.jar             Librería para la escucha global
licencias/
├── TERCEROS.md                       Software de terceros y cómo reemplazarlo
├── LGPL-3.0.md                       Licencia de JNativeHook
└── GPL-3.0.md                        Licencia en la que se apoya la LGPL
recursos/                             Iconos de la app para cada sistema
construir.bat / construir.sh          Compilan y empaquetan
probar.bat / probar.sh                Ejecutan las pruebas
.github/workflows/compilar.yml        Pruebas, compilación y publicación automáticas
LICENSE                               Licencia de este proyecto (MIT)
```

## Compilar

Requisitos: JDK 21 o superior con `javac`, `jar` y `jpackage` en el PATH.

| Sistema | Comando |
|---|---|
| Windows | `construir.bat` (o doble clic) |
| macOS / Linux | `./construir.sh` |

Se puede indicar la versión: `construir.bat 1.6.1` o `./construir.sh 1.6.1`. El resultado queda en `dist/`, y la aplicación sin comprimir en `build/salida/` para probarla. `jpackage` solo genera la aplicación del sistema en el que se ejecuta.

## Pruebas

| Sistema | Comando |
|---|---|
| Windows | `probar.bat` (o doble clic) |
| macOS / Linux | `./probar.sh` |

El script compila el programa y las pruebas de `test/` y las ejecuta con JUnit 5, que descarga la primera vez a `lib/pruebas/`. Las pruebas no abren ventanas ni mueven el mouse, así que se pueden ejecutar en cualquier momento.

## Publicar una versión

El workflow `.github/workflows/compilar.yml` ejecuta las pruebas y compila en Windows, macOS y Linux en cada push a `main` o `develop` y en cada Pull Request. Al subir una etiqueta, además crea el Release con los archivos de los tres sistemas y la versión portable:

```
git checkout main
git pull
git tag -a v1.6.1 -m "Versión 1.6.1"
git push origin v1.6.1
```

## Licencia

AutoClicker se publica bajo la [licencia MIT](LICENSE).

Usa [JNativeHook](https://github.com/kwhat/jnativehook) para los atajos globales y la grabación, que se distribuye sin modificar bajo la licencia LGPL-3.0. Va en un archivo aparte (`jnativehook-2.2.2.jar`) para que se pueda reemplazar por otra versión. Los textos de las licencias, el enlace a su código fuente y los pasos para reemplazarla están en [`licencias/TERCEROS.md`](licencias/TERCEROS.md), y se incluyen también dentro de cada paquete.