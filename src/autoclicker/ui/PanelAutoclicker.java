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
 * Los parámetros se guardan al iniciar y al cerrar la ventana, y se
 * recuperan al abrir el programa.
 */
class PanelAutoclicker extends PanelModo implements EstadoListener {

    private static final String CLAVE_ATAJO = "atajo"; // se mantiene la clave de versiones anteriores
    private static final int REFRESCO_PROGRESO_MS = 100;

    private final ClickerService servicio;
    private final AtajoService atajos;
    private final PreferenciasRepository preferencias;
    private final AtajoConfigurable atajo;

    // Los valores con los que nacen los campos son los predeterminados de la primera vez
    private final CampoNumerico intervaloCampo =
            new CampoNumerico(100, (int) ConfiguracionClics.INTERVALO_MINIMO_MS, 600_000, 10, "ms");
    private final CampoNumerico esperaCampo    = new CampoNumerico(3, 0, 60, 1, "s");
    private final CampoNumerico clicsCampo     = new CampoNumerico(0, 0, 10_000_000, 1, "clics");
    private final ControlSegmentado<BotonMouse> botonControl =
            new ControlSegmentado<>(BotonMouse.values());
    private final ControlSegmentado<Boolean> tipoControl =
            new ControlSegmentado<>(new Boolean[]{false, true}, new String[]{"Simple", "Doble"});
    private final SelectorMonitor selectorMonitor = new SelectorMonitor();

    private final BotonModerno iniciarBtn = new BotonModerno("Iniciar", BotonModerno.Variante.PRIMARIO);
    private final BotonModerno detenerBtn = new BotonModerno("Detener", BotonModerno.Variante.PELIGRO);
    private final IndicadorEstado estado = new IndicadorEstado();
    private final Timer relojProgreso = new Timer(REFRESCO_PROGRESO_MS, e -> mostrarProgreso());

    PanelAutoclicker(ClickerService servicio, AtajoService atajos, PreferenciasRepository preferencias) {
        this.servicio = servicio;
        this.atajos = atajos;
        this.preferencias = preferencias;
        this.atajo = new AtajoConfigurable(CLAVE_ATAJO, AtajoService.F6, this::alternar, atajos, preferencias);
        atajo.setAlCambiar(this::mostrarAtajo);
        tipoControl.alCambiar(doble -> {
            if (Boolean.TRUE.equals(doble) && intervaloCampo.getValor() < ConfiguracionClics.INTERVALO_MINIMO_DOBLE_MS) {
                intervaloCampo.setValor((int) ConfiguracionClics.INTERVALO_MINIMO_DOBLE_MS);
            }
        });
        mostrarConfiguracion(preferencias.cargarConfiguracionClics(leerConfiguracionParaGuardar()));

        Diseno.Pila opciones = pilaDeOpciones();
        opciones.agregar(Diseno.dosColumnas(
                Diseno.tarjeta("Intervalo entre clics", intervaloCampo, null),
                Diseno.tarjeta("Espera inicial", esperaCampo, null)), 0);
        opciones.agregar(Diseno.tarjeta("Botón del mouse", botonControl, null), 12);
        opciones.agregar(Diseno.dosColumnas(
                Diseno.tarjeta("Tipo de clic", tipoControl, null),
                Diseno.tarjeta("Repeticiones", clicsCampo, "0 = repetir hasta detener")), 12);
        if (selectorMonitor.hayMultiplesMonitores()) {
            opciones.agregar(Diseno.tarjeta("Monitor activo", selectorMonitor,
                    "En los demás monitores los clics se pausan"), 12);
        }
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

    @Override public String titulo() { return "Autoclicker"; }
    @Override public List<String> clavesAtajo() { return List.of(atajo.clave()); }
    /** El servicio es la única fuente de verdad: aquí no se guarda una copia del estado. */
    @Override public boolean estaOcupado() { return servicio.estaCorriendo(); }

    @Override
    public void detenerTodo() {
        servicio.detener();
    }

    @Override
    public void guardarParametros() {
        preferencias.guardarConfiguracionClics(leerConfiguracionParaGuardar());
    }

    // ---- Parámetros ----

    /** Configuración para ejecutar la sesión: usa el monitor activo si está disponible, o "Todos". */
    private ConfiguracionClics leerConfiguracionParaTrabajar() {
        return new ConfiguracionClics(
                intervaloCampo.getValor(),
                clicsCampo.getValor(),
                esperaCampo.getValor(),
                botonControl.getSeleccion(),
                tipoControl.getSeleccion(),
                selectorMonitor.monitorParaTrabajar());
    }

    /** Configuración para persistir: conserva el último monitor elegido o cargado. */
    private ConfiguracionClics leerConfiguracionParaGuardar() {
        return new ConfiguracionClics(
                intervaloCampo.getValor(),
                clicsCampo.getValor(),
                esperaCampo.getValor(),
                botonControl.getSeleccion(),
                tipoControl.getSeleccion(),
                selectorMonitor.monitorParaGuardar());
    }

    /** Pone una configuración en los campos; cada campo ajusta el valor a sus propios límites. */
    private void mostrarConfiguracion(ConfiguracionClics config) {
        intervaloCampo.setValor((int) Math.min(config.intervaloMs(), Integer.MAX_VALUE));
        clicsCampo.setValor(config.maxClics());
        esperaCampo.setValor(config.esperaSegundos());
        botonControl.setSeleccion(config.boton());
        tipoControl.setSeleccion(config.dobleClic());
        selectorMonitor.setMonitor(config.monitor());
    }

    // ---- Acciones ----

    private void iniciar() {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        try {
            ConfiguracionClics configTrabajo = leerConfiguracionParaTrabajar();
            // Si ya había una sesión en marcha no se inició nada y no habrá aviso de
            // final: la interfaz se deja como está.
            if (!servicio.iniciar(configTrabajo)) return;
            relojProgreso.start();
            actualizarControles();
            // Se guarda aquí además de al cerrar, por si el programa no se cierra con la X.
            preferencias.guardarConfiguracionClics(leerConfiguracionParaGuardar());
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
        if (servicio.estaPausado()) return;
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
        selectorMonitor.setEnabled(libre);
        atajo.setEnabled(libre);
        avisarCambioDeOcupado();
    }
}