package autoclicker.servicio;

import autoclicker.modelo.EventoMacro;
import autoclicker.modelo.EventoMacro.*;
import autoclicker.modelo.Grabacion;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.SwingKeyAdapter;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * Graba movimientos, clics y rueda del mouse, y opcionalmente el teclado,
 * todo en la misma línea de tiempo.
 *
 * La posición del mouse se lee con MouseInfo (y no con las coordenadas de
 * JNativeHook) porque así usa el mismo sistema de coordenadas que Robot,
 * incluso con la escala de pantalla de Windows al 125 % o 150 %.
 */
public class GrabadoraService implements EntradaListener {

    private static final long SONDEO_MS = 8;

    private final boolean incluirTeclado;
    private final ConversorTeclas conversor = new ConversorTeclas();
    private final List<EventoMacro> eventos = new ArrayList<>();

    private volatile boolean grabando = false;
    private long inicioNanos;
    private Thread sondeo;

    public GrabadoraService(boolean incluirTeclado) {
        this.incluirTeclado = incluirTeclado;
    }

    public boolean incluyeTeclado() {
        return incluirTeclado;
    }

    public boolean estaGrabando() {
        return grabando;
    }

    public synchronized int cantidadEventos() {
        return eventos.size();
    }

    public long milisegundosGrabados() {
        return grabando ? ahora() : 0;
    }

    public synchronized void iniciar() {
        if (grabando) return;
        eventos.clear();
        inicioNanos = System.nanoTime();
        Point p = posicionMouse();
        if (p != null) eventos.add(new MovimientoMouse(0, p.x, p.y));
        grabando = true;

        sondeo = new Thread(this::sondearMouse, "hilo-grabacion");
        sondeo.setDaemon(true);
        sondeo.start();
    }

    /**
     * Termina la grabación.
     * @param desdeLaVentana true si se detuvo con el botón de la ventana: en ese
     *                       caso se quita el último clic, que fue sobre ese botón
     */
    public Grabacion detener(boolean desdeLaVentana) {
        List<EventoMacro> copia;
        long duracion;
        synchronized (this) {
            if (!grabando) return Grabacion.VACIA;
            grabando = false;
            duracion = ahora();
            copia = new ArrayList<>(eventos);
        }
        if (sondeo != null) sondeo.interrupt();

        copia.sort(Comparator.comparingLong(EventoMacro::tiempoMs));
        if (desdeLaVentana) duracion = quitarUltimoClic(copia, duracion);
        equilibrar(copia, duracion);
        return new Grabacion(copia, duracion);
    }

    // ---- Captura de eventos ----

    private void sondearMouse() {
        Point ultimo = null;
        while (grabando) {
            Point p = posicionMouse();
            if (p != null && !p.equals(ultimo)) {
                agregar(new MovimientoMouse(ahora(), p.x, p.y));
                ultimo = p;
            }
            try {
                Thread.sleep(SONDEO_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void teclaPresionada(NativeKeyEvent e) {
        if (!grabando || !incluirTeclado) return;
        int codigo = conversor.codigoJava(e);
        if (codigo != KeyEvent.VK_UNDEFINED) agregar(new PresionTecla(ahora(), codigo));
    }

    @Override
    public void teclaSoltada(NativeKeyEvent e) {
        if (!grabando || !incluirTeclado) return;
        int codigo = conversor.codigoJava(e);
        if (codigo != KeyEvent.VK_UNDEFINED) agregar(new SueltaTecla(ahora(), codigo));
    }

    @Override
    public void botonPresionado(NativeMouseEvent e) {
        if (!grabando) return;
        int boton = botonJava(e.getButton());
        if (boton == 0) return;
        Point p = posicionOEvento(e);
        agregar(new PresionBoton(ahora(), boton, p.x, p.y));
    }

    @Override
    public void botonSoltado(NativeMouseEvent e) {
        if (!grabando) return;
        int boton = botonJava(e.getButton());
        if (boton == 0) return;
        Point p = posicionOEvento(e);
        agregar(new SueltaBoton(ahora(), boton, p.x, p.y));
    }

    @Override
    public void ruedaMovida(NativeMouseWheelEvent e) {
        if (!grabando) return;
        if (e.getWheelDirection() == NativeMouseWheelEvent.WHEEL_HORIZONTAL_DIRECTION) return;
        if (e.getWheelRotation() != 0) agregar(new RuedaMouse(ahora(), e.getWheelRotation()));
    }

    private synchronized void agregar(EventoMacro evento) {
        if (grabando) eventos.add(evento);
    }

    // ---- Limpieza de la grabación ----

    /** Quita el último clic y lo que venga después. Devuelve la nueva duración. */
    static long quitarUltimoClic(List<EventoMacro> lista, long duracion) {
        for (int i = lista.size() - 1; i >= 0; i--) {
            if (lista.get(i) instanceof PresionBoton p) {
                lista.subList(i, lista.size()).clear();
                return p.tiempoMs();
            }
        }
        return duracion;
    }

    /**
     * Deja la grabación "equilibrada": quita las sueltas que no tienen su
     * presión (por ejemplo, soltar la tecla con la que se empezó a grabar) y
     * agrega al final las sueltas que faltan (por ejemplo, el Ctrl del atajo
     * con el que se detuvo). Así la reproducción nunca deja teclas pegadas.
     */
    static void equilibrar(List<EventoMacro> lista, long fin) {
        Map<Integer, Integer> teclas = new HashMap<>();
        Map<Integer, Integer> botones = new HashMap<>();
        int x = 0;
        int y = 0;

        Iterator<EventoMacro> it = lista.iterator();
        while (it.hasNext()) {
            EventoMacro e = it.next();
            switch (e) {
                case MovimientoMouse m -> { x = m.x(); y = m.y(); }
                case PresionTecla t -> teclas.merge(t.codigoTecla(), 1, Integer::sum);
                case SueltaTecla t -> { if (!quitarUno(teclas, t.codigoTecla())) it.remove(); }
                case PresionBoton b -> { botones.merge(b.boton(), 1, Integer::sum); x = b.x(); y = b.y(); }
                case SueltaBoton b -> { if (!quitarUno(botones, b.boton())) it.remove(); }
                case RuedaMouse r -> { }
            }
        }
        for (int codigo : teclas.keySet()) lista.add(new SueltaTecla(fin, codigo));
        for (int boton : botones.keySet()) lista.add(new SueltaBoton(fin, boton, x, y));
    }

    private static boolean quitarUno(Map<Integer, Integer> presionados, int codigo) {
        Integer n = presionados.get(codigo);
        if (n == null) return false;
        if (n == 1) presionados.remove(codigo); else presionados.put(codigo, n - 1);
        return true;
    }

    // ---- Utilidades ----

    private long ahora() {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }

    private static Point posicionMouse() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info == null ? null : info.getLocation();
    }

    private static Point posicionOEvento(NativeMouseEvent e) {
        Point p = posicionMouse();
        return p != null ? p : new Point(e.getX(), e.getY());
    }

    /** JNativeHook numera 1 = izquierdo, 2 = derecho, 3 = central; Java usa 1, 3, 2. */
    static int botonJava(int botonNativo) {
        return switch (botonNativo) {
            case NativeMouseEvent.BUTTON1 -> 1;
            case NativeMouseEvent.BUTTON2 -> 3;
            case NativeMouseEvent.BUTTON3 -> 2;
            case NativeMouseEvent.BUTTON4 -> 4;
            case NativeMouseEvent.BUTTON5 -> 5;
            default -> 0;
        };
    }

    /** Usa la tabla de JNativeHook para pasar de código nativo a código de Java. */
    private static final class ConversorTeclas extends SwingKeyAdapter {
        int codigoJava(NativeKeyEvent e) {
            return getJavaKeyEvent(e).getKeyCode();
        }
    }
}
