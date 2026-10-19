package autoclicker.servicio;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.ConfiguracionTecleo;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lógica del autoteclado: presiona una tecla o combinación con Robot en un hilo aparte.
 * No sabe nada de la interfaz; se comunica mediante {@link EstadoListener}.
 *
 * El intervalo configurado es el tiempo entre el INICIO de una pulsación y el
 * inicio de la siguiente. La duración de la presión cuenta dentro de ese intervalo,
 * no se suma a él.
 *
 * La espera entre pulsaciones duerme el hilo sin espera activa para no saturar la CPU.
 * Se planifica por instantes límite con System.nanoTime() para mantener el promedio
 * exacto sin acumular atraso.
 *
 * El progreso no se notifica pulsación a pulsación: quien lo muestre debe consultar
 * {@link #pulsacionesHechas()} periódicamente. El listener solo recibe avisos
 * puntuales (cuenta regresiva, inicio y final).
 */
public class TecleadorService {

    /** Duración máxima de la presión en milisegundos. */
    private static final long PAUSA_PRESION_MAX_MS = 30;
    /** Cuánto se espera a que termine el hilo de la sesión anterior antes de rendirse. */
    private static final long ESPERA_HILO_ANTERIOR_MS = 200;

    private final Robot robot;
    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private final AtomicInteger pulsaciones = new AtomicInteger();
    private volatile EstadoListener listener = EstadoListener.NINGUNO;
    private Thread hilo;

    /** Teclas que están presionadas en este instante, para soltarlas al detener. */
    private final Set<Integer> teclasSostenidas = ConcurrentHashMap.newKeySet();

    public TecleadorService() throws AWTException {
        this(new Robot());
    }

    /** Permite inyectar un Robot (útil para pruebas). */
    public TecleadorService(Robot robot) {
        this.robot = robot;
    }

    public void setListener(EstadoListener listener) {
        this.listener = (listener != null) ? listener : EstadoListener.NINGUNO;
    }

    /**
     * true desde que se inicia una sesión hasta que termina o se pide detenerla.
     * Es la fuente de verdad para la interfaz: se puede leer desde cualquier hilo.
     */
    public boolean estaCorriendo() {
        return corriendo.get();
    }

    /** Pulsaciones hechas en la sesión actual o en la última. Se puede leer desde cualquier hilo. */
    public int pulsacionesHechas() {
        return pulsaciones.get();
    }

    /**
     * Inicia una sesión de tecleo repetido.
     * @return false si no se inició porque ya hay una sesión en marcha; en ese caso
     *         no habrá avisos nuevos al listener
     */
    public synchronized boolean iniciar(ConfiguracionTecleo config) {
        if (hilo != null && hilo.isAlive()) {
            if (corriendo.get()) return false;
            esperarHiloAnterior();
            if (hilo.isAlive()) return false;
        }
        pulsaciones.set(0);
        corriendo.set(true);
        hilo = new Thread(() -> ejecutar(config), "hilo-tecleo");
        hilo.setDaemon(true);
        hilo.start();
        return true;
    }

    public synchronized void detener() {
        corriendo.set(false);
        if (hilo != null) hilo.interrupt();
    }

    /**
     * Duración de la presión de la tecla: min(30, intervalo / 2).
     * Con el intervalo mínimo de 10 ms nunca baja de 5 ms ni supera la mitad del intervalo.
     */
    public static long pausaPresionMs(long intervaloMs) {
        return Math.min(PAUSA_PRESION_MAX_MS, intervaloMs / 2);
    }

    /** Duración de la presión para la configuración dada. */
    public static long pausaPresionMs(ConfiguracionTecleo config) {
        return pausaPresionMs(config.intervaloMs());
    }

    // ---- Lógica interna ----

    private void ejecutar(ConfiguracionTecleo config) {
        int hechos = 0;
        String mensajeFinal = "Detenido";
        try {
            for (int s = config.esperaSegundos(); s > 0 && corriendo.get(); s--) {
                listener.alCambiarEstado("Empieza en " + s + "...");
                Thread.sleep(1000);
            }

            int codigoJava = ConversorTeclas.aCodigoJava(config.tecla().codigo());
            if (codigoJava == KeyEvent.VK_UNDEFINED || codigoJava <= 0) {
                mensajeFinal = "Tecla no soportada";
                return;
            }

            listener.alCambiarEstado("Pulsando...");
            long pausaPresionMs = pausaPresionMs(config);
            long intervaloNs = TimeUnit.MILLISECONDS.toNanos(config.intervaloMs());
            long siguiente = System.nanoTime();

            while (corriendo.get() && (config.esInfinito() || hechos < config.repeticiones())) {
                pulsar(config, codigoJava, pausaPresionMs);
                hechos = pulsaciones.incrementAndGet();

                if (!config.esInfinito() && hechos >= config.repeticiones()) break;

                siguiente += intervaloNs;
                if (siguiente - System.nanoTime() > 0) {
                    esperarHasta(siguiente);
                } else {
                    siguiente = System.nanoTime();
                }
            }

            if (!config.esInfinito() && hechos >= config.repeticiones()) {
                mensajeFinal = "Terminado — " + hechos + (hechos == 1 ? " pulsación" : " pulsaciones");
            } else {
                mensajeFinal = "Detenido — " + hechos + (hechos == 1 ? " pulsación" : " pulsaciones");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mensajeFinal = "Detenido — " + hechos + (hechos == 1 ? " pulsación" : " pulsaciones");
        } finally {
            soltarTodo();
            corriendo.set(false);
            listener.alTerminar(mensajeFinal);
        }
    }

    private void pulsar(ConfiguracionTecleo config, int codigoJava, long pausaPresionMs) throws InterruptedException {
        Deque<Integer> pulsadas = new ArrayDeque<>();
        try {
            int mods = config.tecla().modificadores();
            if ((mods & Atajo.CTRL) != 0) {
                robot.keyPress(KeyEvent.VK_CONTROL);
                pulsadas.push(KeyEvent.VK_CONTROL);
                teclasSostenidas.add(KeyEvent.VK_CONTROL);
            }
            if ((mods & Atajo.ALT) != 0) {
                robot.keyPress(KeyEvent.VK_ALT);
                pulsadas.push(KeyEvent.VK_ALT);
                teclasSostenidas.add(KeyEvent.VK_ALT);
            }
            if ((mods & Atajo.SHIFT) != 0) {
                robot.keyPress(KeyEvent.VK_SHIFT);
                pulsadas.push(KeyEvent.VK_SHIFT);
                teclasSostenidas.add(KeyEvent.VK_SHIFT);
            }
            if ((mods & Atajo.META) != 0) {
                robot.keyPress(KeyEvent.VK_META);
                pulsadas.push(KeyEvent.VK_META);
                teclasSostenidas.add(KeyEvent.VK_META);
            }

            robot.keyPress(codigoJava);
            pulsadas.push(codigoJava);
            teclasSostenidas.add(codigoJava);

            if (pausaPresionMs > 0) {
                Thread.sleep(pausaPresionMs);
            }
        } finally {
            while (!pulsadas.isEmpty()) {
                int tecla = pulsadas.pop();
                try {
                    robot.keyRelease(tecla);
                } catch (IllegalArgumentException ignorada) {
                }
                teclasSostenidas.remove(tecla);
            }
        }
    }

    /** Espera durmiendo hasta el instante límite indicado, sin espera activa. */
    private void esperarHasta(long instanteNs) throws InterruptedException {
        while (corriendo.get()) {
            long falta = instanteNs - System.nanoTime();
            if (falta <= 0) return;
            TimeUnit.NANOSECONDS.sleep(falta);
        }
    }

    private void soltarTodo() {
        for (int tecla : teclasSostenidas) {
            try {
                robot.keyRelease(tecla);
            } catch (IllegalArgumentException ignorada) {
            }
        }
        teclasSostenidas.clear();
    }

    private void esperarHiloAnterior() {
        try {
            hilo.join(ESPERA_HILO_ANTERIOR_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
