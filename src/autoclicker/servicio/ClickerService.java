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
 * clic) cuenta dentro de ese intervalo, no se suma a él.
 *
 * El progreso no se notifica clic a clic: quien lo muestre debe consultar
 * {@link #clicsHechos()} cada cierto tiempo. El listener solo recibe avisos
 * puntuales (cuenta regresiva, inicio y final).
 */
public class ClickerService {

    private static final int PAUSA_PRESION_MS = 10;
    private static final int PAUSA_DOBLE_CLIC_MS = 40;

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

    public boolean estaCorriendo() {
        return corriendo.get();
    }

    /** Clics (o dobles clics) hechos en la sesión actual o en la última. Se puede leer desde cualquier hilo. */
    public int clicsHechos() {
        return clics.get();
    }

    /**
     * Inicia una sesión de clics.
     * @return false si no se inició porque el hilo de la sesión anterior todavía está terminando;
     *         en ese caso no habrá avisos al listener, así que quien llama no debe esperar un alTerminar
     */
    public synchronized boolean iniciar(ConfiguracionClics config) {
        if (hilo != null && hilo.isAlive()) return false;   // el hilo anterior aún no termina
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
            long intervaloNs = TimeUnit.MILLISECONDS.toNanos(config.intervaloMs());
            long siguiente = System.nanoTime();   // instante en que debe empezar el próximo clic

            while (corriendo.get() && (config.esInfinito() || hechos < config.maxClics())) {
                clic(mascara);
                if (config.dobleClic()) {
                    robot.delay(PAUSA_DOBLE_CLIC_MS);
                    clic(mascara);
                }
                hechos = clics.incrementAndGet();

                // Al llegar al último clic no hay nada que esperar
                if (!config.esInfinito() && hechos >= config.maxClics()) break;

                siguiente += intervaloNs;
                long falta = siguiente - System.nanoTime();
                if (falta > 0) {
                    TimeUnit.NANOSECONDS.sleep(falta);
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

    private void clic(int mascara) {
        robot.mousePress(mascara);
        robot.delay(PAUSA_PRESION_MS);
        robot.mouseRelease(mascara);
    }

    private static int mascaraDe(BotonMouse boton) {
        return switch (boton) {
            case IZQUIERDO -> InputEvent.BUTTON1_DOWN_MASK;
            case DERECHO   -> InputEvent.BUTTON3_DOWN_MASK;
            case CENTRAL   -> InputEvent.BUTTON2_DOWN_MASK;
        };
    }
}