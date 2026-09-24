package autoclicker.servicio;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lógica del autoclicker: controla el mouse con Robot en un hilo aparte.
 * No sabe nada de la interfaz; se comunica mediante ClickerListener.
 */
public class ClickerService {

    private static final int MARGEN_ESQUINA_PX = 5;
    private static final int PAUSA_PRESION_MS = 10;
    private static final int PAUSA_DOBLE_CLIC_MS = 40;

    private final Robot robot;
    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private volatile ClickerListener listener = ClickerListener.NINGUNO;
    private Thread hilo;

    public ClickerService() throws AWTException {
        this(new Robot());
    }

    /** Permite inyectar un Robot (útil para pruebas). */
    public ClickerService(Robot robot) {
        this.robot = robot;
    }

    public void setListener(ClickerListener listener) {
        this.listener = (listener != null) ? listener : ClickerListener.NINGUNO;
    }

    public boolean estaCorriendo() {
        return corriendo.get();
    }

    public synchronized void iniciar(ConfiguracionClics config) {
        if (hilo != null && hilo.isAlive()) return;   // ya hay una sesión activa
        corriendo.set(true);
        hilo = new Thread(() -> ejecutar(config), "hilo-clics");
        hilo.setDaemon(true);
        hilo.start();
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

            int mascara = mascaraDe(config.boton());
            boolean porEsquina = false;

            while (corriendo.get() && (config.esInfinito() || hechos < config.maxClics())) {
                if (mouseEnEsquinaSeguridad()) {
                    porEsquina = true;
                    break;
                }
                clic(mascara);
                if (config.dobleClic()) {
                    robot.delay(PAUSA_DOBLE_CLIC_MS);
                    clic(mascara);
                }
                hechos++;
                listener.alCambiarEstado("Clicando... " + hechos + " clics");
                Thread.sleep(config.intervaloMs());
            }

            if (porEsquina) {
                mensajeFinal = "Detenido (esquina de seguridad) — " + hechos + " clics";
            } else if (!config.esInfinito() && hechos >= config.maxClics()) {
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

    private boolean mouseEnEsquinaSeguridad() {
        PointerInfo info = MouseInfo.getPointerInfo();
        if (info == null) return false;
        Point p = info.getLocation();
        return p.x <= MARGEN_ESQUINA_PX && p.y <= MARGEN_ESQUINA_PX;
    }

    private static int mascaraDe(BotonMouse boton) {
        return switch (boton) {
            case IZQUIERDO -> InputEvent.BUTTON1_DOWN_MASK;
            case DERECHO   -> InputEvent.BUTTON3_DOWN_MASK;
            case CENTRAL   -> InputEvent.BUTTON2_DOWN_MASK;
        };
    }
}
