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
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
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
 * Todos los eventos llegan al hilo de Swing gracias a SwingDispatchService.
 */
public class AtajoService implements NativeKeyListener, NativeMouseListener, NativeMouseWheelListener {

    public static final Atajo F6  = tecla(NativeKeyEvent.VC_F6);
    public static final Atajo F7  = tecla(NativeKeyEvent.VC_F7);
    public static final Atajo F8  = tecla(NativeKeyEvent.VC_F8);
    public static final Atajo F9  = tecla(NativeKeyEvent.VC_F9);
    public static final Atajo F10 = tecla(NativeKeyEvent.VC_F10);

    private final Map<String, Atajo> atajos = new HashMap<>();
    private final Map<String, Runnable> acciones = new HashMap<>();
    private final Set<String> activos = new HashSet<>();
    private final List<EntradaListener> oyentes = new CopyOnWriteArrayList<>();

    private final Set<Integer> teclasPresionadas = new HashSet<>();
    private final Set<Integer> teclasDeAtajo = new HashSet<>();
    private final Set<Integer> botonesDeAtajo = new HashSet<>();

    private Consumer<Atajo> capturaPendiente;
    private boolean tolerarModificadores = false;
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
            GlobalScreen.addNativeMouseWheelListener(this);
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
            GlobalScreen.removeNativeMouseWheelListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {
        }
    }

    public boolean estaDisponible() {
        return disponible;
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
        boolean repeticion = !teclasPresionadas.add(codigo);
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
        oyentes.forEach(o -> o.teclaPresionada(e));
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int codigo = e.getKeyCode();
        teclasPresionadas.remove(codigo);
        if (teclasDeAtajo.remove(codigo)) return;
        oyentes.forEach(o -> o.teclaSoltada(e));
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
        oyentes.forEach(o -> o.botonPresionado(e));
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (botonesDeAtajo.remove(e.getButton())) return;
        oyentes.forEach(o -> o.botonSoltado(e));
    }

    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        oyentes.forEach(o -> o.ruedaMovida(e));
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

    // ---- Utilidades ----

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
