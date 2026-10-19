package autoclicker.servicio;

import autoclicker.modelo.ConfiguracionClics;

import java.util.concurrent.TimeUnit;

/**
 * Filtro que decide si una acción de clic o pulsación puede ejecutarse según
 * el monitor en el que se encuentre el cursor.
 *
 * Es una clase pura sin dependencias de AWT, lo que permite probarla en
 * entornos headless. Recibe la hora actual en nanosegundos (System.nanoTime())
 * y aplica la regla de espera de 500 ms consecutivos tras volver de otro monitor
 * antes de reanudar la actividad.
 */
public class FiltroMonitor {

    public static final long ESPERA_REANUDACION_NS = TimeUnit.MILLISECONDS.toNanos(500);

    public enum CambioEstado {
        NINGUNO,
        PAUSADO,
        REANUDADO
    }

    public record Decision(boolean puedeActuar, CambioEstado cambio) {
        public boolean cambioDeEstado() {
            return cambio != CambioEstado.NINGUNO;
        }
    }

    private final int monitor;
    private final UbicacionCursor ubicacion;
    private final long esperaReanudacionNs;

    private boolean pausado;
    private long instanteRetornoNs;

    public FiltroMonitor(int monitor, UbicacionCursor ubicacion) {
        this(monitor, ubicacion, ESPERA_REANUDACION_NS);
    }

    public FiltroMonitor(int monitor, UbicacionCursor ubicacion, long esperaReanudacionNs) {
        this.monitor = monitor;
        this.ubicacion = (ubicacion != null) ? ubicacion : () -> true;
        this.esperaReanudacionNs = esperaReanudacionNs;
        this.pausado = false;
        this.instanteRetornoNs = -1;
    }

    public boolean esTodos() {
        return monitor == ConfiguracionClics.TODOS_LOS_MONITORES;
    }

    public int monitor() {
        return monitor;
    }

    public boolean estaPausado() {
        return pausado;
    }

    public String mensajePausa() {
        return "En pausa: el cursor está fuera del Monitor " + (monitor + 1);
    }

    /**
     * Evalúa si en el instante dado se puede ejecutar la acción programada.
     *
     * @param ahoraNs marca de tiempo en nanosegundos (System.nanoTime())
     * @return la decisión tomada: si puede actuar y si se produjo un cambio de estado
     */
    public Decision evaluar(long ahoraNs) {
        if (esTodos()) {
            return new Decision(true, CambioEstado.NINGUNO);
        }

        boolean dentro = ubicacion.estaEnMonitorObjetivo();

        if (pausado) {
            if (!dentro) {
                // Sigue fuera: se reinicia cualquier contador de retorno
                instanteRetornoNs = -1;
                return new Decision(false, CambioEstado.NINGUNO);
            }

            // El cursor está dentro del monitor objetivo
            if (instanteRetornoNs < 0) {
                instanteRetornoNs = ahoraNs;
            }

            if (ahoraNs - instanteRetornoNs >= esperaReanudacionNs) {
                // Se cumplió el plazo continuo de 500 ms: reanudar
                pausado = false;
                instanteRetornoNs = -1;
                return new Decision(true, CambioEstado.REANUDADO);
            } else {
                // Aún no transcurren los 500 ms continuos
                return new Decision(false, CambioEstado.NINGUNO);
            }
        } else {
            // Estaba activo
            if (dentro) {
                // Sigue dentro: actuar normalmente
                return new Decision(true, CambioEstado.NINGUNO);
            } else {
                // Salió del monitor objetivo: pausar
                pausado = true;
                instanteRetornoNs = -1;
                return new Decision(false, CambioEstado.PAUSADO);
            }
        }
    }
}
