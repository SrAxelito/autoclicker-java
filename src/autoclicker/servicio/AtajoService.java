package autoclicker.servicio;

import autoclicker.modelo.Atajo;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Escucha el teclado y el mouse de forma global (aunque la ventana no
 * tenga el foco) usando JNativeHook. Se encarga de:
 * <ul>
 *   <li>disparar los atajos registrados que estén activos,</li>
 *   <li>capturar un atajo nuevo cuando el usuario lo está cambiando,</li>
 *   <li>entregar el resto de pulsaciones a los oyentes (la grabadora).</li>
 * </ul>
 * Todos los eventos se atienden en el hilo de Swing, pero cada uno conserva
 * el instante en que llegó del sistema (ver {@link DespachoConHora}).
 */
public class AtajoService implements NativeKeyListener, NativeMouseListener, NativeMouseWheelListener {

    public static final Atajo F6  = tecla(NativeKeyEvent.VC_F6);
    public static final Atajo F7  = tecla(NativeKeyEvent.VC_F7);
    public static final Atajo F8  = tecla(NativeKeyEvent.VC_F8);
    public static final Atajo F9  = tecla(NativeKeyEvent.VC_F9);
    public static final Atajo F10 = tecla(NativeKeyEvent.VC_F10);
    public static final Atajo F11 = tecla(NativeKeyEvent.VC_F11);
    public static final Atajo F12 = tecla(NativeKeyEvent.VC_F12);

    /**
     * Al mantener una tecla, el sistema repite el aviso de "presionada" cada
     * pocos milisegundos (como mucho uno o dos segundos entre el primero y el
     * segundo). Si entre dos avisos pasa más que esto, el segundo no es una
     * repetición: es una pulsación nueva cuyo "soltar" anterior se perdió.
     */
    private static final long CADUCIDAD_PULSACION_NS = TimeUnit.SECONDS.toNanos(3);

    private final Map<String, Atajo> atajos = new HashMap<>();
    private final Map<String, Runnable> acciones = new HashMap<>();
    private final Set<String> activos = new HashSet<>();
    private final List<EntradaListener> oyentes = new CopyOnWriteArrayList<>();

    /** Teclas que el sistema reporta como presionadas, con el instante de su último aviso. */
    private final Map<Integer, Long> teclasPresionadas = new HashMap<>();
    private final Set<Integer> teclasDeAtajo = new HashSet<>();
    private final Set<Integer> botonesDeAtajo = new HashSet<>();

    private Consumer<Atajo> capturaPendiente;
    private boolean tolerarModificadores = false;
    private boolean disponible = false;
    private FalloEscucha fallo = null;

    private final LongSupplier reloj;
    // Instante de llegada del evento que se está atendiendo (solo hilo de Swing)
    private long instanteEnCurso;
    private boolean hayInstanteEnCurso = false;

    public AtajoService() {
        this(System::nanoTime);
    }

    /** Permite inyectar el reloj, en nanosegundos (útil para pruebas). */
    AtajoService(LongSupplier reloj) {
        this.reloj = reloj;
    }

    /**
     * Por qué no se pudo activar la escucha global.
     * @param resumen una línea corta, para mostrar en la ventana
     * @param ayuda   explicación y pasos para resolverlo, para un diálogo
     */
    public record FalloEscucha(String resumen, String ayuda) { }

    /**
     * Activa la escucha global.
     * @return false si el sistema no lo permite (el resto de la app sigue funcionando)
     */
    public boolean iniciar() {
        prepararCarpetaNativa();
        Logger registro = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        registro.setLevel(Level.WARNING);
        registro.setUseParentHandlers(false);
        try {
            GlobalScreen.setEventDispatcher(new DespachoConHora());
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeMouseWheelListener(this);
            disponible = true;
            fallo = null;
        } catch (NativeHookException e) {
            disponible = false;
            fallo = describir(e);
        } catch (LinkageError e) {
            disponible = false;
            fallo = describir(e);
        }
        return disponible;
    }

    public void cerrar() {
        if (!disponible) return;
        disponible = false;
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.removeNativeMouseListener(this);
            GlobalScreen.removeNativeMouseWheelListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {
        }
    }

    public boolean estaDisponible() {
        return disponible;
    }

    /** Por qué falló la escucha global; vacío si está activa. */
    public Optional<FalloEscucha> fallo() {
        return Optional.ofNullable(fallo);
    }

    // ---- Registro de atajos ----

    /** Registra un atajo con su acción. Queda inactivo hasta que se llame a activarSolo. */
    public void registrar(String clave, Atajo atajo, Runnable accion) {
        atajos.put(clave, atajo);
        acciones.put(clave, accion);
    }

    public void cambiarAtajo(String clave, Atajo nuevo) {
        atajos.put(clave, nuevo);
    }

    public Atajo atajo(String clave) {
        return atajos.get(clave);
    }

    /** Solo estos atajos responderán (los del modo visible). */
    public void activarSolo(Collection<String> claves) {
        activos.clear();
        activos.addAll(claves);
    }

    /**
     * Mientras se reproduce una grabación, el teclado puede tener teclas como
     * Shift sostenidas por la propia grabación. En ese caso el atajo se
     * reconoce solo por la tecla, sin mirar los modificadores.
     */
    public void setTolerarModificadores(boolean tolerar) {
        this.tolerarModificadores = tolerar;
    }

    public void agregarOyente(EntradaListener oyente) {
        oyentes.add(oyente);
    }

    // ---- Captura de un atajo nuevo ----

    /** La siguiente tecla o botón lateral se entrega al callback (null si se cancela con Esc). */
    public void capturar(Consumer<Atajo> alCapturar) {
        capturaPendiente = alCapturar;
    }

    public void cancelarCaptura() {
        terminarCaptura(null);
    }

    public boolean estaCapturando() {
        return capturaPendiente != null;
    }

    private void terminarCaptura(Atajo resultado) {
        Consumer<Atajo> callback = capturaPendiente;
        capturaPendiente = null;
        if (callback != null) callback.accept(resultado);
    }

    // ---- Teclado ----

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int codigo = e.getKeyCode();
        long instante = instante();
        Long anterior = teclasPresionadas.put(codigo, instante);
        boolean repeticion = anterior != null && instante - anterior < CADUCIDAD_PULSACION_NS;
        // Pulsación nueva de una tecla que figuraba como atajo en curso: se perdió
        // su aviso de soltar (por ejemplo, al bloquear la sesión). Se olvida esa
        // marca para que el atajo responda ya, y no a la segunda pulsación.
        if (!repeticion) teclasDeAtajo.remove(codigo);
        if (teclasDeAtajo.contains(codigo)) return; // repetición de una tecla usada como atajo

        if (estaCapturando()) {
            if (repeticion) return;
            if (codigo == NativeKeyEvent.VC_ESCAPE) {
                teclasDeAtajo.add(codigo);
                terminarCaptura(null);
            } else if (!esModificador(codigo)) {
                teclasDeAtajo.add(codigo);
                int mods = modificadores(e);
                terminarCaptura(new Atajo(Atajo.Tipo.TECLA, codigo, mods, nombreTecla(codigo, mods)));
            }
            return;
        }

        if (!repeticion) {
            Runnable accion = buscarTecla(codigo, modificadores(e));
            if (accion != null) {
                teclasDeAtajo.add(codigo);
                accion.run();
                return;
            }
        }
        oyentes.forEach(o -> o.teclaPresionada(e, instante));
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int codigo = e.getKeyCode();
        long instante = instante();
        teclasPresionadas.remove(codigo);
        if (teclasDeAtajo.remove(codigo)) return;
        oyentes.forEach(o -> o.teclaSoltada(e, instante));
    }

    // ---- Mouse (solo los botones laterales pueden ser atajo) ----

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        int boton = e.getButton();
        boolean lateral = boton == NativeMouseEvent.BUTTON4 || boton == NativeMouseEvent.BUTTON5;

        if (estaCapturando()) {
            if (lateral) {
                botonesDeAtajo.add(boton);
                String nombre = boton == NativeMouseEvent.BUTTON4 ? "Botón lateral 1" : "Botón lateral 2";
                terminarCaptura(new Atajo(Atajo.Tipo.MOUSE, boton, 0, nombre));
            }
            return;
        }

        if (lateral) {
            Runnable accion = buscarBoton(boton);
            if (accion != null) {
                botonesDeAtajo.add(boton);
                accion.run();
                return;
            }
        }
        long instante = instante();
        oyentes.forEach(o -> o.botonPresionado(e, instante));
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (botonesDeAtajo.remove(e.getButton())) return;
        long instante = instante();
        oyentes.forEach(o -> o.botonSoltado(e, instante));
    }

    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        long instante = instante();
        oyentes.forEach(o -> o.ruedaMovida(e, instante));
    }

    // ---- Hora de los eventos ----

    /**
     * Instante (System.nanoTime) en que llegó del sistema el evento que se está
     * atendiendo. Si el aviso no pasó por {@link DespachoConHora} (por ejemplo,
     * en una prueba), es el instante actual.
     */
    private long instante() {
        return hayInstanteEnCurso ? instanteEnCurso : reloj.getAsLong();
    }

    /**
     * Entrega los eventos en el hilo de Swing, igual que el SwingDispatchService
     * de JNativeHook, pero antes anota cuándo llegaron. JNativeHook llama a
     * execute() desde su propio hilo en cuanto el sistema le avisa, así que esa
     * hora no depende de lo ocupado que esté el hilo de Swing.
     *
     * No se usa NativeInputEvent.getWhen() porque su reloj cambia según el
     * sistema: en Linux es la hora del servidor X y en Windows el contador de
     * ticks, que puede avanzar a saltos de varios milisegundos.
     */
    private final class DespachoConHora extends AbstractExecutorService {

        private volatile boolean cerrado = false;

        @Override
        public void execute(Runnable entrega) {
            long llegada = reloj.getAsLong();   // hilo de JNativeHook
            SwingUtilities.invokeLater(() -> {
                instanteEnCurso = llegada;
                hayInstanteEnCurso = true;
                try {
                    entrega.run();
                } finally {
                    hayInstanteEnCurso = false;
                }
            });
        }

        @Override public void shutdown() { cerrado = true; }
        @Override public List<Runnable> shutdownNow() { cerrado = true; return List.of(); }
        @Override public boolean isShutdown() { return cerrado; }
        @Override public boolean isTerminated() { return cerrado; }
        @Override public boolean awaitTermination(long tiempo, TimeUnit unidad) { return true; }
    }

    // ---- Búsqueda de atajos activos ----

    private Runnable buscarTecla(int codigo, int mods) {
        for (String clave : activos) {
            Atajo a = atajos.get(clave);
            if (a != null && a.coincideConTecla(codigo, mods)) return acciones.get(clave);
        }
        if (tolerarModificadores) {
            for (String clave : activos) {
                Atajo a = atajos.get(clave);
                if (a != null && a.tipo() == Atajo.Tipo.TECLA && a.codigo() == codigo) return acciones.get(clave);
            }
        }
        return null;
    }

    private Runnable buscarBoton(int boton) {
        for (String clave : activos) {
            Atajo a = atajos.get(clave);
            if (a != null && a.coincideConBoton(boton)) return acciones.get(clave);
        }
        return null;
    }

    // ---- Diagnóstico de fallos ----

    /** Traduce el código de error de JNativeHook a un mensaje que el usuario pueda entender. */
    static FalloEscucha describir(NativeHookException e) {
        String tecnico = "\n\nDetalle técnico: "
                + (e.getMessage() != null ? e.getMessage() : "código " + e.getCode());
        return switch (e.getCode()) {
            case NativeHookException.DARWIN_AXAPI_DISABLED,
                 NativeHookException.DARWIN_CREATE_EVENT_PORT -> new FalloEscucha(
                    "Faltan permisos de macOS para los atajos",
                    "Activa AutoClicker en Configuración del Sistema → Privacidad y seguridad, "
                            + "en Accesibilidad y en Monitoreo de entrada, y vuelve a abrir la aplicación."
                            + tecnico);
            case NativeHookException.X11_OPEN_DISPLAY -> new FalloEscucha(
                    "No se pudo abrir la pantalla X11",
                    "La escucha global de Linux necesita una sesión X11. En Wayland no funciona: "
                            + "en la pantalla de inicio de sesión elige \"Ubuntu en Xorg\" "
                            + "(o el equivalente de tu distribución)." + tecnico);
            case NativeHookException.X11_RECORD_NOT_FOUND,
                 NativeHookException.X11_RECORD_ALLOC_RANGE,
                 NativeHookException.X11_RECORD_CREATE_CONTEXT,
                 NativeHookException.X11_RECORD_ENABLE_CONTEXT,
                 NativeHookException.X11_RECORD_GET_CONTEXT -> new FalloEscucha(
                    "X11 no ofrece la extensión para los atajos",
                    "El servidor X11 de esta sesión no permite la extensión XRecord, que la escucha "
                            + "global necesita. Prueba con una sesión X11 estándar (Xorg)." + tecnico);
            case NativeHookException.WIN_SET_HOOK -> new FalloEscucha(
                    "Windows no permitió activar los atajos",
                    "Windows rechazó registrar la escucha de teclado y mouse. Puede estar bloqueada "
                            + "por un antivirus o una directiva del sistema." + tecnico);
            default -> new FalloEscucha(
                    "No se pudo activar la escucha global",
                    "El resto de la aplicación sigue funcionando, pero los atajos globales y la "
                            + "grabación de clics y teclas pueden no estar disponibles." + tecnico);
        };
    }

    /** Falla al cargar la librería nativa (sistema o procesador no soportado, carpeta sin permisos...). */
    static FalloEscucha describir(LinkageError e) {
        return new FalloEscucha(
                "No se pudo cargar la librería nativa",
                "JNativeHook no pudo cargar su librería para este sistema o procesador, o la carpeta "
                        + "de datos de la aplicación no permite escribir."
                        + "\n\nDetalle técnico: " + e);
    }

    // ---- Utilidades ----

    /**
     * JNativeHook extrae su librería nativa (.dll, .dylib o .so) junto al .jar.
     * Si el programa está instalado en una carpeta protegida (por ejemplo,
     * Archivos de programa) eso falla, así que se usa una carpeta del usuario.
     */
    private static void prepararCarpetaNativa() {
        if (System.getProperty("jnativehook.lib.path") != null) return;
        try {
            Path carpeta = carpetaDatosUsuario().resolve("nativo");
            Files.createDirectories(carpeta);
            System.setProperty("jnativehook.lib.path", carpeta.toString());
        } catch (IOException | SecurityException | InvalidPathException ignorada) {
            // Se deja el comportamiento por defecto de JNativeHook
        }
    }

    /** Carpeta de datos de la aplicación según el sistema operativo. */
    static Path carpetaDatosUsuario() {
        String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String inicio = System.getProperty("user.home");
        if (sistema.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return Path.of(local != null && !local.isBlank() ? local : inicio, "AutoClicker");
        }
        if (sistema.contains("mac")) {
            return Path.of(inicio, "Library", "Application Support", "AutoClicker");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        Path datos = (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : Path.of(inicio, ".local", "share");
        return datos.resolve("autoclicker");
    }

    private static Atajo tecla(int codigo) {
        return new Atajo(Atajo.Tipo.TECLA, codigo, 0, NativeKeyEvent.getKeyText(codigo));
    }

    private static boolean esModificador(int codigo) {
        return codigo == NativeKeyEvent.VC_CONTROL || codigo == NativeKeyEvent.VC_SHIFT
                || codigo == NativeKeyEvent.VC_ALT || codigo == NativeKeyEvent.VC_META;
    }

    /** Convierte los modificadores de JNativeHook (izquierdo/derecho, bloqueos...) a los del modelo. */
    private static int modificadores(NativeInputEvent e) {
        int m = e.getModifiers();
        int resultado = 0;
        if ((m & NativeInputEvent.CTRL_MASK)  != 0) resultado |= Atajo.CTRL;
        if ((m & NativeInputEvent.SHIFT_MASK) != 0) resultado |= Atajo.SHIFT;
        if ((m & NativeInputEvent.ALT_MASK)   != 0) resultado |= Atajo.ALT;
        if ((m & NativeInputEvent.META_MASK)  != 0) resultado |= Atajo.META;
        return resultado;
    }

    private static String nombreTecla(int codigo, int mods) {
        StringBuilder sb = new StringBuilder();
        if ((mods & Atajo.CTRL)  != 0) sb.append("Ctrl + ");
        if ((mods & Atajo.ALT)   != 0) sb.append("Alt + ");
        if ((mods & Atajo.SHIFT) != 0) sb.append("Shift + ");
        if ((mods & Atajo.META)  != 0) sb.append("Win + ");
        sb.append(NativeKeyEvent.getKeyText(codigo));
        return sb.toString();
    }
}