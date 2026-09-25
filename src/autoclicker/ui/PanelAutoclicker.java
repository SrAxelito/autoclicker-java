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

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Pestaña del autoclicker: clics repetidos en la posición actual del mouse.
 */
class PanelAutoclicker extends JPanel implements ModoPanel, EstadoListener {

    private static final String CLAVE_ATAJO = "atajo"; // se mantiene la clave de versiones anteriores

    private final ClickerService servicio;
    private final AtajoService atajos;
    private final AtajoConfigurable atajo;
    private Runnable alCambiarOcupado = () -> { };

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

    PanelAutoclicker(ClickerService servicio, AtajoService atajos, PreferenciasRepository preferencias) {
        super(new GridBagLayout());
        this.servicio = servicio;
        this.atajos = atajos;
        this.atajo = new AtajoConfigurable(CLAVE_ATAJO, AtajoService.F6, this::alternar, atajos, preferencias);
        atajo.setAlCambiar(this::mostrarAtajo);
        setOpaque(false);

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
    @Override public JComponent vista() { return this; }
    @Override public List<String> clavesAtajo() { return List.of(atajo.clave()); }
    @Override public boolean estaOcupado() { return detenerBtn.isEnabled(); }

    @Override
    public void setAlCambiarOcupado(Runnable accion) {
        this.alCambiarOcupado = (accion != null) ? accion : () -> { };
    }

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
            habilitarControles(false);
            servicio.iniciar(config);
        } catch (IllegalArgumentException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Lo que hace el atajo: detener si está trabajando, iniciar si está quieto. */
    private void alternar() {
        if (detenerBtn.isEnabled()) {
            servicio.detener();
        } else if (iniciarBtn.isEnabled()) {
            iniciar();
        }
    }

    private String notaAtajo() {
        return atajos.estaDisponible()
                ? "Funciona aunque la ventana no esté activa"
                : "No se pudo activar la escucha global del teclado";
    }

    private void mostrarAtajo() {
        if (atajos.estaDisponible()) {
            iniciarBtn.setTextos("Iniciar  ·  " + atajo.nombre(), "Iniciar");
            detenerBtn.setTextos("Detener  ·  " + atajo.nombre(), "Detener");
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
            estado.setEstado(mensajeFinal, IndicadorEstado.Tono.INACTIVO);
            habilitarControles(true);
        });
    }

    private void habilitarControles(boolean habilitar) {
        iniciarBtn.setEnabled(habilitar);
        detenerBtn.setEnabled(!habilitar);
        intervaloCampo.setEnabled(habilitar);
        esperaCampo.setEnabled(habilitar);
        clicsCampo.setEnabled(habilitar);
        botonControl.setEnabled(habilitar);
        tipoControl.setEnabled(habilitar);
        atajo.setEnabled(habilitar);
        alCambiarOcupado.run();
    }
}
