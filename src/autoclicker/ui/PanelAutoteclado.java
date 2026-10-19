package autoclicker.ui;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.ConfiguracionTecleo;
import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ConversorTeclas;
import autoclicker.servicio.EstadoListener;
import autoclicker.servicio.TecleadorService;
import autoclicker.ui.componentes.BotonModerno;
import autoclicker.ui.componentes.CampoNumerico;
import autoclicker.ui.componentes.IndicadorEstado;
import autoclicker.ui.componentes.SelectorAtajo;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.List;

/**
 * Pestaña del autoteclado: pulsaciones repetidas de una tecla o combinación.
 * Los parámetros se guardan al iniciar y al cerrar la ventana, y se
 * recuperan al abrir el programa.
 */
class PanelAutoteclado extends PanelModo implements EstadoListener {

    private static final String CLAVE_ATAJO = "tecleo.atajo";
    private static final int REFRESCO_PROGRESO_MS = 100;
    private static final Atajo TECLA_PREDETERMINADA =
            new Atajo(Atajo.Tipo.TECLA, NativeKeyEvent.VC_SPACE, 0, "Espacio");

    private final TecleadorService servicio;
    private final AtajoService atajos;
    private final PreferenciasRepository preferencias;
    private final AtajoConfigurable atajo;

    private Atajo teclaActual = TECLA_PREDETERMINADA;
    private final SelectorAtajo selectorTecla = new SelectorAtajo();

    // Los valores con los que nacen los campos son los predeterminados de la primera vez
    private final CampoNumerico intervaloCampo =
            new CampoNumerico(100, (int) ConfiguracionTecleo.INTERVALO_MINIMO_MS, 600_000, 10, "ms");
    private final CampoNumerico esperaCampo       = new CampoNumerico(3, 0, 60, 1, "s");
    private final CampoNumerico repeticionesCampo = new CampoNumerico(0, 0, 10_000_000, 1, "veces");

    private final BotonModerno iniciarBtn = new BotonModerno("Iniciar", BotonModerno.Variante.PRIMARIO);
    private final BotonModerno detenerBtn = new BotonModerno("Detener", BotonModerno.Variante.PELIGRO);
    private final IndicadorEstado estado = new IndicadorEstado();
    private final Timer relojProgreso = new Timer(REFRESCO_PROGRESO_MS, e -> mostrarProgreso());

    PanelAutoteclado(TecleadorService servicio, AtajoService atajos, PreferenciasRepository preferencias) {
        this.servicio = servicio;
        this.atajos = atajos;
        this.preferencias = preferencias;
        this.atajo = new AtajoConfigurable(CLAVE_ATAJO, AtajoService.F6, this::alternar, atajos, preferencias);
        atajo.setAlCambiar(this::mostrarAtajo);

        selectorTecla.setDisponible(atajos.estaDisponible());
        selectorTecla.setNombre(teclaActual.nombre());
        selectorTecla.setAlActivar(this::alternarCapturaTecla);

        mostrarConfiguracion(preferencias.cargarConfiguracionTecleo(leerConfiguracion()));

        Diseno.Pila opciones = pilaDeOpciones();
        opciones.agregar(Diseno.tarjeta("Tecla a pulsar", selectorTecla, "Acepta modificadores (Ctrl, Alt, Shift)"), 0);
        opciones.agregar(Diseno.dosColumnas(
                Diseno.tarjeta("Intervalo entre pulsaciones", intervaloCampo, null),
                Diseno.tarjeta("Espera inicial", esperaCampo, null)), 12);
        opciones.agregar(Diseno.tarjeta("Repeticiones", repeticionesCampo, "0 = repetir hasta detener"), 12);
        opciones.agregar(Diseno.tarjeta("Atajo para iniciar / detener", atajo.selector(), notaAtajo()), 12);

        Diseno.Pila pie = pilaDelPie();
        pie.agregar(estado, 28);
        pie.agregar(Diseno.dosColumnas(iniciarBtn, detenerBtn), 12);

        iniciarBtn.addActionListener(e -> iniciar());
        detenerBtn.addActionListener(e -> servicio.detener());
        detenerBtn.setEnabled(false);
        servicio.setListener(this);
        mostrarAtajo();
    }

    // ---- ModoPanel ----

    @Override public String titulo() { return "Autoteclado"; }
    @Override public List<String> clavesAtajo() { return List.of(atajo.clave()); }
    /** El servicio es la única fuente de verdad: aquí no se guarda una copia del estado. */
    @Override public boolean estaOcupado() { return servicio.estaCorriendo(); }

    @Override
    public void detenerTodo() {
        servicio.detener();
    }

    @Override
    public void guardarParametros() {
        preferencias.guardarConfiguracionTecleo(leerConfiguracion());
    }

    // ---- Parámetros ----

    /** Arma la configuración con lo que hay ahora mismo en los campos. */
    private ConfiguracionTecleo leerConfiguracion() {
        return new ConfiguracionTecleo(
                teclaActual,
                intervaloCampo.getValor(),
                repeticionesCampo.getValor(),
                esperaCampo.getValor());
    }

    /** Pone una configuración en los campos; cada campo ajusta el valor a sus propios límites. */
    private void mostrarConfiguracion(ConfiguracionTecleo config) {
        teclaActual = config.tecla();
        selectorTecla.setNombre(teclaActual.nombre());
        intervaloCampo.setValor((int) Math.min(config.intervaloMs(), Integer.MAX_VALUE));
        repeticionesCampo.setValor(config.repeticiones());
        esperaCampo.setValor(config.esperaSegundos());
    }

    // ---- Acciones ----

    private void alternarCapturaTecla() {
        if (atajos.estaCapturando()) {
            boolean eraEste = selectorTecla.estaCapturando();
            atajos.cancelarCaptura();
            if (eraEste) return;
        }
        selectorTecla.setCapturando(true);
        atajos.capturarSoloTeclas(nueva -> {
            selectorTecla.setCapturando(false);
            if (nueva == null) return; // cancelado con Esc
            if (!ConversorTeclas.esConvertible(nueva.codigo())) {
                selectorTecla.mostrarAviso("Robot no soporta esta tecla");
                return;
            }
            teclaActual = nueva;
            selectorTecla.setNombre(nueva.nombre());
        });
    }

    private void iniciar() {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        try {
            ConfiguracionTecleo config = leerConfiguracion();
            Atajo tecla = config.tecla();
            Atajo atajoIniciar = atajos.atajo(atajo.clave());
            if (atajoIniciar != null
                    && tecla.tipo() == atajoIniciar.tipo()
                    && tecla.codigo() == atajoIniciar.codigo()
                    && tecla.modificadores() == atajoIniciar.modificadores()) {
                JOptionPane.showMessageDialog(this,
                        "La tecla a pulsar no puede ser el mismo atajo de iniciar y detener.",
                        "Configuración inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!servicio.iniciar(config)) return;
            atajos.setTolerarModificadores(true);
            relojProgreso.start();
            actualizarControles();
            preferencias.guardarConfiguracionTecleo(config);
        } catch (IllegalArgumentException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
        }
    }

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

    private void mostrarProgreso() {
        int hechos = servicio.pulsacionesHechas();
        if (hechos > 0 && servicio.estaCorriendo()) {
            estado.setEstado("Pulsando... " + hechos + (hechos == 1 ? " pulsación" : " pulsaciones"),
                    IndicadorEstado.Tono.ACTIVO);
        }
    }

    // ---- EstadoListener (llegan desde el hilo de tecleo) ----

    @Override
    public void alCambiarEstado(String mensaje) {
        SwingUtilities.invokeLater(() -> estado.setEstado(mensaje, IndicadorEstado.Tono.ACTIVO));
    }

    @Override
    public void alTerminar(String mensajeFinal) {
        SwingUtilities.invokeLater(() -> {
            if (servicio.estaCorriendo()) return;
            atajos.setTolerarModificadores(false);
            relojProgreso.stop();
            estado.setEstado(mensajeFinal, IndicadorEstado.Tono.INACTIVO);
            actualizarControles();
        });
    }

    private void actualizarControles() {
        boolean libre = !servicio.estaCorriendo();
        iniciarBtn.setEnabled(libre);
        detenerBtn.setEnabled(!libre);
        selectorTecla.setEnabled(libre);
        intervaloCampo.setEnabled(libre);
        esperaCampo.setEnabled(libre);
        repeticionesCampo.setEnabled(libre);
        atajo.setEnabled(libre);
        avisarCambioDeOcupado();
    }
}
