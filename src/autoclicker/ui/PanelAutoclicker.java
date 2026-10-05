package autoclicker.ui;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ClickerService;
import autoclicker.servicio.EstadoListener;
import autoclicker.ui.componentes.BotonModerno;
import autoclicker.ui.componentes.CampoNumerico;
import autoclicker.ui.componentes.ControlSegmentado;
import autoclicker.ui.componentes.IndicadorEstado;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.List;

/**
 * Pestaña del autoclicker: clics repetidos en la posición actual del mouse.
 */
class PanelAutoclicker extends PanelModo implements EstadoListener {

    private static final String CLAVE_ATAJO = "atajo"; // se mantiene la clave de versiones anteriores
    private static final int REFRESCO_PROGRESO_MS = 100;

    private final ClickerService servicio;
    private final AtajoService atajos;
    private final AtajoConfigurable atajo;

    private final CampoNumerico intervaloCampo = new CampoNumerico(100, 1, 600_000, 10, "ms");
    private final CampoNumerico esperaCampo    = new CampoNumerico(3, 0, 60, 1, "s");
    private final CampoNumerico clicsCampo     = new CampoNumerico(0, 0, 10_000_000, 1, "clics");
    private final ControlSegmentado<BotonMouse> botonControl =
            new ControlSegmentado<>(BotonMouse.values());
    private final ControlSegmentado<Boolean> tipoControl =
            new ControlSegmentado<>(new Boolean[]{false, true}, new String[]{"Simple", "Doble"});

    private final BotonModerno iniciarBtn = new BotonModerno("Iniciar", BotonModerno.Variante.PRIMARIO);
    private final BotonModerno detenerBtn = new BotonModerno("Detener", BotonModerno.Variante.PELIGRO);
    private final IndicadorEstado estado = new IndicadorEstado();
    private final Timer relojProgreso = new Timer(REFRESCO_PROGRESO_MS, e -> mostrarProgreso());

    PanelAutoclicker(ClickerService servicio, AtajoService atajos, PreferenciasRepository preferencias) {
        this.servicio = servicio;
        this.atajos = atajos;
        this.atajo = new AtajoConfigurable(CLAVE_ATAJO, AtajoService.F6, this::alternar, atajos, preferencias);
        atajo.setAlCambiar(this::mostrarAtajo);

        Diseno.Pila pila = new Diseno.Pila(this);
        pila.agregar(Diseno.dosColumnas(
                Diseno.tarjeta("Intervalo entre clics", intervaloCampo, null),
                Diseno.tarjeta("Espera inicial", esperaCampo, null)), 0);
        pila.agregar(Diseno.tarjeta("Botón del mouse", botonControl, null), 12);
        pila.agregar(Diseno.dosColumnas(
                Diseno.tarjeta("Tipo de clic", tipoControl, null),
                Diseno.tarjeta("Repeticiones", clicsCampo, "0 = repetir hasta detener")), 12);
        pila.agregar(Diseno.tarjeta("Atajo para iniciar / detener", atajo.selector(), notaAtajo()), 12);
        pila.agregarRelleno();
        pila.agregar(estado, 18);
        pila.agregar(Diseno.dosColumnas(iniciarBtn, detenerBtn), 12);

        iniciarBtn.addActionListener(e -> iniciar());
        detenerBtn.addActionListener(e -> servicio.detener());
        detenerBtn.setEnabled(false);
        servicio.setListener(this);
        mostrarAtajo();
    }

    // ---- ModoPanel ----

    @Override public String titulo() { return "Autoclicker"; }
    @Override public List<String> clavesAtajo() { return List.of(atajo.clave()); }
    /** El servicio es la única fuente de verdad: aquí no se guarda una copia del estado. */
    @Override public boolean estaOcupado() { return servicio.estaCorriendo(); }

    @Override
    public void detenerTodo() {
        servicio.detener();
    }

    // ---- Acciones ----

    private void iniciar() {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        try {
            ConfiguracionClics config = new ConfiguracionClics(
                    intervaloCampo.getValor(),
                    clicsCampo.getValor(),
                    esperaCampo.getValor(),
                    botonControl.getSeleccion(),
                    tipoControl.getSeleccion());
            // Si ya había una sesión en marcha no se inició nada y no habrá aviso de
            // final: la interfaz se deja como está.
            if (!servicio.iniciar(config)) return;
            relojProgreso.start();
            actualizarControles();
        } catch (IllegalArgumentException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Lo que hace el atajo: detener si está trabajando, iniciar si está quieto.
     * Se pregunta al servicio y no a la interfaz, porque la interfaz se entera
     * del final un momento después (el aviso llega con invokeLater).
     */
    private void alternar() {
        if (servicio.estaCorriendo()) {
            servicio.detener();
        } else {
            iniciar();
        }
    }

    private String notaAtajo() {
        if (atajos.estaDisponible()) return "Funciona aunque la ventana no esté activa";
        return atajos.fallo()
                .map(AtajoService.FalloEscucha::resumen)
                .orElse("No se pudo activar la escucha global del teclado");
    }

    private void mostrarAtajo() {
        if (atajos.estaDisponible()) {
            iniciarBtn.setTextos("Iniciar  ·  " + atajo.nombre(), "Iniciar");
            detenerBtn.setTextos("Detener  ·  " + atajo.nombre(), "Detener");
        }
    }

    /** Se ejecuta en el hilo de Swing: lee el contador del servicio en lugar de recibir un aviso por clic. */
    private void mostrarProgreso() {
        int hechos = servicio.clicsHechos();
        if (hechos > 0 && servicio.estaCorriendo()) {
            estado.setEstado("Clicando... " + hechos + " clics", IndicadorEstado.Tono.ACTIVO);
        }
    }

    // ---- EstadoListener (llegan desde el hilo de clics) ----

    @Override
    public void alCambiarEstado(String mensaje) {
        SwingUtilities.invokeLater(() -> estado.setEstado(mensaje, IndicadorEstado.Tono.ACTIVO));
    }

    @Override
    public void alTerminar(String mensajeFinal) {
        SwingUtilities.invokeLater(() -> {
            // Aviso atrasado de la sesión anterior: ya se inició otra y la interfaz
            // la está mostrando, así que este final no debe desbloquear nada.
            if (servicio.estaCorriendo()) return;
            relojProgreso.stop();
            estado.setEstado(mensajeFinal, IndicadorEstado.Tono.INACTIVO);
            actualizarControles();
        });
    }

    /** Pone los controles según el estado del servicio. Se llama desde el hilo de Swing. */
    private void actualizarControles() {
        boolean libre = !servicio.estaCorriendo();
        iniciarBtn.setEnabled(libre);
        detenerBtn.setEnabled(!libre);
        intervaloCampo.setEnabled(libre);
        esperaCampo.setEnabled(libre);
        clicsCampo.setEnabled(libre);
        botonControl.setEnabled(libre);
        tipoControl.setEnabled(libre);
        atajo.setEnabled(libre);
        avisarCambioDeOcupado();
    }
}