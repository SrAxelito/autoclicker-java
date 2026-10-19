package autoclicker.servicio;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Lógica del autoclicker: controla el mouse con Robot en un hilo aparte.
 * No sabe nada de la interfaz; se comunica mediante EstadoListener.
 *
 * El intervalo configurado es el tiempo entre el INICIO de un clic y el
 * inicio del siguiente. Lo que tarda cada clic (pausas de presión y de doble
 * clic) cuenta dentro de ese intervalo, no se suma a él.
 *
 * La espera entre clics se realiza durmiendo el hilo (sin espera activa ni
 * Thread.onSpinWait) para no saturar la CPU ni trabar los juegos. Se planifica
 * por instantes límite con System.nanoTime(): aunque cada espera individual pueda
 * pasarse ligeramente, el promedio se mantiene exacto y no se acumula atraso.
 * Si un clic se retrasa más allá del siguiente instante límite, se reprograma desde
 * el momento actual en vez de disparar clics en ráfaga para recuperar.
 *
 * El progreso no se notifica clic a clic: quien lo muestre debe consultar
 * {@link #clicsHechos()} cada cierto tiempo. El listener solo recibe avisos
 * puntuales (cuenta regresiva, inicio y final).
 */
public class ClickerService {

    /** Tiempo que el botón queda presionado cuando el intervalo lo permite. */
    private static final long PAUSA_PRESION_MAX_MS = 10;
    /** Separación entre los dos clics de un doble clic cuando el intervalo lo permite. */
    private static final long PAUSA_DOBLE_CLIC_MAX_MS = 40;
    /** Cuánto se espera a que termine el hilo de la sesión anterior antes de rendirse. */
    private static final long ESPERA_HILO_ANTERIOR_MS = 200;
    /** Intervalo de sondeo cuando la sesión está en pausa para comprobar el retorno del cursor. */
    private static final long PASO_REVISION_PAUSA_MS = 50;

    private final Robot robot;
    private final Function<Integer, UbicacionCursor> creadorUbicacion;
    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private final AtomicBoolean pausado = new AtomicBoolean(false);
    private final AtomicInteger clics = new AtomicInteger();
    private volatile EstadoListener listener = EstadoListener.NINGUNO;
    private Thread hilo;

    public ClickerService() throws AWTException {
        this(new Robot());
    }

    /** Permite inyectar un Robot (útil para pruebas). */
    public ClickerService(Robot robot) {
        this(robot, UbicacionCursorAwt::new);
    }

    /** Permite inyectar un Robot y una ubicación de cursor específica (útil para pruebas). */
    public ClickerService(Robot robot, UbicacionCursor ubicacion) {
        this(robot, monitor -> ubicacion);
    }

    /** Permite inyectar un Robot y una fábrica de ubicaciones de cursor. */
    public ClickerService(Robot robot, Function<Integer, UbicacionCursor> creadorUbicacion) {
        this.robot = robot;
        this.creadorUbicacion = (creadorUbicacion != null) ? creadorUbicacion : UbicacionCursorAwt::new;
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

    /** true si la sesión está corriendo pero temporalmente en pausa por estar fuera del monitor. */
    public boolean estaPausado() {
        return pausado.get();
    }

    /** Clics (o dobles clics) hechos en la sesión actual o en la última. Se puede leer desde cualquier hilo. */
    public int clicsHechos() {
        return clics.get();
    }

    /**
     * Inicia una sesión de clics.
     * @return false si no se inició porque ya hay una sesión en marcha; en ese caso
     *         no habrá avisos nuevos al listener
     */
    public synchronized boolean iniciar(ConfiguracionClics config) {
        if (hilo != null && hilo.isAlive()) {
            if (corriendo.get()) return false;   // ya hay una sesión en marcha
            // La sesión anterior ya se detuvo y su hilo está terminando: tarda
            // microsegundos, así que se le espera en lugar de perder la pulsación.
            esperarHiloAnterior();
            if (hilo.isAlive()) return false;
        }
        clics.set(0);
        pausado.set(false);
        corriendo.set(true);
        hilo = new Thread(() -> ejecutar(config), "hilo-clics");
        hilo.setDaemon(true);
        hilo.start();
        return true;
    }

    public synchronized void detener() {
        corriendo.set(false);
        pausado.set(false);
        if (hilo != null) hilo.interrupt();
    }

    // ---- Lógica interna ----

    private void ejecutar(ConfiguracionClics config) {
        int hechos = 0;
        String mensajeFinal = "Detenido";
        pausado.set(false);
        try {
            for (int s = config.esperaSegundos(); s > 0 && corriendo.get(); s--) {
                listener.alCambiarEstado("Empieza en " + s + "...");
                Thread.sleep(1000);
            }

            listener.alCambiarEstado("Clicando...");
            int mascara = mascaraDe(config.boton());
            long pausaPresionMs = pausaPresionMs(config);
            long pausaDobleMs = pausaDobleClicMs(config);
            long intervaloNs = TimeUnit.MILLISECONDS.toNanos(config.intervaloMs());
            long siguiente = System.nanoTime();   // instante en que debe empezar el próximo clic

            UbicacionCursor ubicacion = (config.monitor() >= 0)
                    ? creadorUbicacion.apply(config.monitor())
                    : () -> true;
            FiltroMonitor filtro = new FiltroMonitor(config.monitor(), ubicacion);

            while (corriendo.get() && (config.esInfinito() || hechos < config.maxClics())) {
                FiltroMonitor.Decision decision = filtro.evaluar(System.nanoTime());
                if (decision.cambio() == FiltroMonitor.CambioEstado.PAUSADO) {
                    pausado.set(true);
                    listener.alCambiarEstado(filtro.mensajePausa());
                } else if (decision.cambio() == FiltroMonitor.CambioEstado.REANUDADO) {
                    pausado.set(false);
                    listener.alCambiarEstado("Clicando...");
                }

                if (!decision.puedeActuar()) {
                    Thread.sleep(PASO_REVISION_PAUSA_MS);
                    continue;
                }

                if (decision.cambio() == FiltroMonitor.CambioEstado.REANUDADO) {
                    siguiente = System.nanoTime();
                }

                clic(mascara, pausaPresionMs);
                if (config.dobleClic()) {
                    if (pausaDobleMs > 0) Thread.sleep(pausaDobleMs);
                    clic(mascara, pausaPresionMs);
                }
                hechos = clics.incrementAndGet();

                // Al llegar al último clic no hay nada que esperar
                if (!config.esInfinito() && hechos >= config.maxClics()) break;

                siguiente += intervaloNs;
                if (siguiente - System.nanoTime() > 0) {
                    esperarHasta(siguiente);
                } else {
                    // El clic tardó más que el intervalo: no se acumula "deuda" ni se
                    // disparan clics seguidos para recuperar el tiempo perdido.
                    siguiente = System.nanoTime();
                }
            }

            if (!config.esInfinito() && hechos >= config.maxClics()) {
                mensajeFinal = "Terminado — " + hechos + " clics";
            } else {
                mensajeFinal = "Detenido — " + hechos + " clics";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mensajeFinal = "Detenido — " + hechos + " clics";
        } finally {
            pausado.set(false);
            corriendo.set(false);
            listener.alTerminar(mensajeFinal);
        }
    }

    private void clic(int mascara, long pausaPresionMs) throws InterruptedException {
        robot.mousePress(mascara);
        try {
            if (pausaPresionMs > 0) Thread.sleep(pausaPresionMs);
        } finally {
            robot.mouseRelease(mascara);   // aunque se detenga a mitad, el botón nunca queda presionado
        }
    }

    /**
     * Tiempo que el botón queda presionado:
     * - Clic simple: min(10, intervalo / 2).
     * - Doble clic: min(10, intervalo / 4).
     * Con los mínimos (10 ms simple y 20 ms doble), nunca baja de 5 ms.
     */
    static long pausaPresionMs(ConfiguracionClics config) {
        long limite = config.dobleClic() ? config.intervaloMs() / 4 : config.intervaloMs() / 2;
        return Math.min(PAUSA_PRESION_MAX_MS, limite);
    }

    /**
     * Separación entre los dos clics de un doble clic: min(40, intervalo / 4).
     * Con el mínimo de 20 ms nunca baja de 5 ms y el doble clic ocupa a lo sumo 3/4 del intervalo.
     */
    static long pausaDobleClicMs(ConfiguracionClics config) {
        return Math.min(PAUSA_DOBLE_CLIC_MAX_MS, config.intervaloMs() / 4);
    }

    /** Espera durmiendo hasta el instante límite indicado, sin espera activa. */
    private void esperarHasta(long instanteNs) throws InterruptedException {
        while (corriendo.get()) {
            long falta = instanteNs - System.nanoTime();
            if (falta <= 0) return;
            TimeUnit.NANOSECONDS.sleep(falta);
        }
    }

    private void esperarHiloAnterior() {
        try {
            hilo.join(ESPERA_HILO_ANTERIOR_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int mascaraDe(BotonMouse boton) {
        return switch (boton) {
            case IZQUIERDO -> InputEvent.BUTTON1_DOWN_MASK;
            case DERECHO   -> InputEvent.BUTTON3_DOWN_MASK;
            case CENTRAL   -> InputEvent.BUTTON2_DOWN_MASK;
        };
    }
}