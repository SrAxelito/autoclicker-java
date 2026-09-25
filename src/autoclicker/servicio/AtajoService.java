package autoclicker.servicio;

import autoclicker.modelo.Atajo;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Escucha el teclado y el mouse de forma global (aunque la ventana no
 * tenga el foco) usando JNativeHook. Sirve para dos cosas:
 * disparar el atajo configurado y capturar un atajo nuevo.
 *
 * Todos los eventos llegan al hilo de Swing gracias a SwingDispatchService.
 */
public class AtajoService implements NativeKeyListener, NativeMouseListener {

    /** Atajo que se usa si el usuario nunca ha elegido uno. */
    public static final Atajo PREDETERMINADO = new Atajo(
            Atajo.Tipo.TECLA, NativeKeyEvent.VC_F6, 0, NativeKeyEvent.getKeyText(NativeKeyEvent.VC_F6));

    private Atajo atajo = PREDETERMINADO;
    private Runnable alActivar = () -> { };
    private Consumer<Atajo> capturaPendiente;
    private final Set<Integer> teclasPresionadas = new HashSet<>();
    private boolean disponible = false;

    /**
     * Activa la escucha global.
     * @return false si el sistema no lo permite (el resto de la app sigue funcionando)
     */
    public boolean iniciar() {
        Logger registro = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        registro.setLevel(Level.WARNING);
        registro.setUseParentHandlers(false);
        try {
            GlobalScreen.setEventDispatcher(new SwingDispatchService());
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
            disponible = true;
        } catch (NativeHookException | LinkageError e) {
            disponible = false;
        }
        return disponible;
    }

    public void cerrar() {
        if (!disponible) return;
        disponible = false;
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.removeNativeMouseListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {
        }
    }

    public boolean estaDisponible() {
        return disponible;
    }

    public void setAtajo(Atajo nuevo) {
        this.atajo = nuevo;
    }

    public void setAlActivar(Runnable accion) {
        this.alActivar = (accion != null) ? accion : () -> { };
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
        if (!teclasPresionadas.add(codigo)) return; // ignora la repetición al mantener la tecla

        if (estaCapturando()) {
            if (codigo == NativeKeyEvent.VC_ESCAPE) {
                terminarCaptura(null);
            } else if (!esModificador(codigo)) {
                int mods = modificadores(e);
                terminarCaptura(new Atajo(Atajo.Tipo.TECLA, codigo, mods, nombreTecla(codigo, mods)));
            }
            return;
        }

        if (atajo.coincideConTecla(codigo, modificadores(e))) alActivar.run();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        teclasPresionadas.remove(e.getKeyCode());
    }

    // ---- Mouse (solo botones laterales, para no chocar con los clics automáticos) ----

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        int boton = e.getButton();
        if (boton != NativeMouseEvent.BUTTON4 && boton != NativeMouseEvent.BUTTON5) return;

        if (estaCapturando()) {
            String nombre = boton == NativeMouseEvent.BUTTON4 ? "Botón lateral 1" : "Botón lateral 2";
            terminarCaptura(new Atajo(Atajo.Tipo.MOUSE, boton, 0, nombre));
            return;
        }

        if (atajo.coincideConBoton(boton)) alActivar.run();
    }

    // ---- Utilidades ----

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
