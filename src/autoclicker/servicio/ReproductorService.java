package autoclicker.servicio;

import autoclicker.modelo.EventoMacro;
import autoclicker.modelo.EventoMacro.*;
import autoclicker.modelo.Grabacion;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reproduce una grabación con Robot respetando sus tiempos, las veces
 * indicadas (0 = hasta detener), en un hilo aparte.
 */
public class ReproductorService {

    private static final long DURACION_MINIMA_MS = 50;

    private final Robot robot;
    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private volatile EstadoListener listener = EstadoListener.NINGUNO;
    private Thread hilo;

    // Solo los usa el hilo de reproducción
    private final Set<Integer> teclasSostenidas = new HashSet<>();
    private final Set<Integer> botonesSostenidos = new HashSet<>();

    public ReproductorService() throws AWTException {
        this(new Robot());
    }

    public ReproductorService(Robot robot) {
        this.robot = robot;
        this.robot.setAutoWaitForIdle(false);
    }

    public void setListener(EstadoListener listener) {
        this.listener = (listener != null) ? listener : EstadoListener.NINGUNO;
    }

    public boolean estaCorriendo() {
        return corriendo.get();
    }

    public synchronized void iniciar(Grabacion grabacion, int repeticiones) {
        if (grabacion.estaVacia()) return;
        if (repeticiones < 0) throw new IllegalArgumentException("Las repeticiones no pueden ser negativas.");
        if (hilo != null && hilo.isAlive()) return;
        corriendo.set(true);
        hilo = new Thread(() -> ejecutar(grabacion, repeticiones), "hilo-reproduccion");
        hilo.setDaemon(true);
        hilo.start();
    }

    public synchronized void detener() {
        corriendo.set(false);
        if (hilo != null) hilo.interrupt();
    }

    // ---- Lógica interna ----

    private void ejecutar(Grabacion grabacion, int repeticiones) {
        boolean infinito = repeticiones == 0;
        long duracion = Math.max(grabacion.duracionMs(), DURACION_MINIMA_MS);
        int hechas = 0;
        String mensajeFinal = "Detenido";
        try {
            while (corriendo.get() && (infinito || hechas < repeticiones)) {
                listener.alCambiarEstado(infinito
                        ? "Reproduciendo… repetición " + (hechas + 1)
                        : "Reproduciendo… " + (hechas + 1) + " de " + repeticiones);

                long inicio = System.nanoTime();
                for (EventoMacro evento : grabacion.eventos()) {
                    if (!corriendo.get()) break;
                    esperarHasta(inicio, evento.tiempoMs());
                    if (!corriendo.get()) break;
                    ejecutar(evento);
                }
                if (!corriendo.get()) break;
                esperarHasta(inicio, duracion);
                hechas++;
            }
            mensajeFinal = (!infinito && hechas >= repeticiones)
                    ? "Terminado — " + repeticiones(hechas)
                    : "Detenido — " + repeticiones(hechas) + " completa" + (hechas == 1 ? "" : "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mensajeFinal = "Detenido — " + repeticiones(hechas) + " completa" + (hechas == 1 ? "" : "s");
        } finally {
            soltarTodo();
            corriendo.set(false);
            listener.alTerminar(mensajeFinal);
        }
    }

    private void ejecutar(EventoMacro evento) {
        switch (evento) {
            case MovimientoMouse m -> robot.mouseMove(m.x(), m.y());
            case PresionBoton p -> {
                robot.mouseMove(p.x(), p.y());
                int mascara = mascara(p.boton());
                if (mascara != 0) {
                    robot.mousePress(mascara);
                    botonesSostenidos.add(p.boton());
                }
            }
            case SueltaBoton s -> {
                robot.mouseMove(s.x(), s.y());
                int mascara = mascara(s.boton());
                if (mascara != 0) {
                    robot.mouseRelease(mascara);
                    botonesSostenidos.remove(s.boton());
                }
            }
            case RuedaMouse r -> robot.mouseWheel(r.giro());
            case PresionTecla t -> {
                try {
                    robot.keyPress(t.codigoTecla());
                    teclasSostenidas.add(t.codigoTecla());
                } catch (IllegalArgumentException ignorada) {
                    // tecla que Robot no sabe presionar en este sistema
                }
            }
            case SueltaTecla t -> {
                try {
                    robot.keyRelease(t.codigoTecla());
                } catch (IllegalArgumentException ignorada) {
                }
                teclasSostenidas.remove(t.codigoTecla());
            }
        }
    }

    /** Espera con precisión de milisegundos: duerme lo grueso y afina al final. */
    private void esperarHasta(long inicioNanos, long milisegundos) throws InterruptedException {
        long objetivo = inicioNanos + milisegundos * 1_000_000L;
        while (corriendo.get()) {
            long falta = objetivo - System.nanoTime();
            if (falta <= 0) return;
            if (falta > 2_000_000L) {
                Thread.sleep((falta - 1_000_000L) / 1_000_000L);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /** Suelta todo lo que haya quedado presionado, por si se detuvo a mitad. */
    private void soltarTodo() {
        for (int codigo : teclasSostenidas) {
            try { robot.keyRelease(codigo); } catch (IllegalArgumentException ignorada) { }
        }
        for (int boton : botonesSostenidos) {
            int mascara = mascara(boton);
            if (mascara != 0) robot.mouseRelease(mascara);
        }
        teclasSostenidas.clear();
        botonesSostenidos.clear();
    }

    private static int mascara(int boton) {
        try {
            return InputEvent.getMaskForButton(boton);
        } catch (IllegalArgumentException e) {
            return 0; // botón que este mouse o sistema no soporta
        }
    }

    private static String repeticiones(int n) {
        return n + (n == 1 ? " repetición" : " repeticiones");
    }
}
