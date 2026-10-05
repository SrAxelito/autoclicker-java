package autoclicker.servicio;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lógica del autoclicker: controla el mouse con Robot en un hilo aparte.
 * No sabe nada de la interfaz; se comunica mediante EstadoListener.
 *
 * El intervalo configurado es el tiempo entre el INICIO de un clic y el
 * inicio del siguiente. Lo que tarda cada clic (pausas de presión y de doble
 * clic) cuenta dentro de ese intervalo, no se suma a él. Para que eso se
 * cumpla también con intervalos cortos, las pausas se acortan hasta ocupar
 * como mucho la mitad del intervalo.
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
    /**
     * Thread.sleep puede pasarse uno o dos milisegundos, así que el último
     * tramo de cada espera se afina con espera activa. Con intervalos de 1 o
     * 2 ms toda la espera es activa y un núcleo del procesador queda ocupado
     * mientras se hacen los clics.
     */
    private static final long MARGEN_ESPERA_ACTIVA_NS = TimeUnit.MILLISECONDS.toNanos(2);
    /** Cuánto se espera a que termine el hilo de la sesión anterior antes de rendirse. */
    private static final long ESPERA_HILO_ANTERIOR_MS = 200;

    private final Robot robot;
    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private final AtomicInteger clics = new AtomicInteger();
    private volatile EstadoListener listener = EstadoListener.NINGUNO;
    private Thread hilo;

    public ClickerService() throws AWTException {
        this(new Robot());
    }

    /** Permite inyectar un Robot (útil para pruebas). */
    public ClickerService(Robot robot) {
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
        corriendo.set(true);
        hilo = new Thread(() -> ejecutar(config), "hilo-clics");
        hilo.setDaemon(true);
        hilo.start();
        return true;
    }

    public synchronized void detener() {
        corriendo.set(false);
        if (hilo != null) hilo.interrupt();
    }

    // ---- Lógica interna ----

    private void ejecutar(ConfiguracionClics config) {
        int hechos = 0;
        String mensajeFinal = "Detenido";
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

            while (corriendo.get() && (config.esInfinito() || hechos < config.maxClics())) {
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
     * Tiempo que el botón queda presionado. Un clic simple ocupa como mucho la
     * mitad del intervalo y un doble clic (dos presiones más la separación)
     * también, para que siempre quede tiempo libre antes del siguiente.
     */
    static long pausaPresionMs(ConfiguracionClics config) {
        long limite = config.dobleClic() ? config.intervaloMs() / 8 : config.intervaloMs() / 2;
        return Math.min(PAUSA_PRESION_MAX_MS, limite);
    }

    /** Separación entre los dos clics de un doble clic: como mucho un cuarto del intervalo. */
    static long pausaDobleClicMs(ConfiguracionClics config) {
        return Math.min(PAUSA_DOBLE_CLIC_MAX_MS, config.intervaloMs() / 4);
    }

    /** Espera hasta el instante indicado: duerme lo grueso y afina el final con espera activa. */
    private void esperarHasta(long instanteNs) throws InterruptedException {
        while (corriendo.get()) {
            long falta = instanteNs - System.nanoTime();
            if (falta <= 0) return;
            if (falta > MARGEN_ESPERA_ACTIVA_NS) {
                TimeUnit.NANOSECONDS.sleep(falta - MARGEN_ESPERA_ACTIVA_NS);
            } else {
                Thread.onSpinWait();
            }
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