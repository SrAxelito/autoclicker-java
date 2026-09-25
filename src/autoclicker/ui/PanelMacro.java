package autoclicker.ui;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.Grabacion;
import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.EstadoListener;
import autoclicker.servicio.GrabadoraService;
import autoclicker.servicio.ReproductorService;
import autoclicker.ui.componentes.BotonModerno;
import autoclicker.ui.componentes.CampoNumerico;
import autoclicker.ui.componentes.Etiqueta;
import autoclicker.ui.componentes.IndicadorEstado;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

/**
 * Pestaña de grabación: graba lo que hace el usuario y lo reproduce
 * las veces indicadas. Sirve tanto para "solo mouse" como para
 * "mouse y teclado", según la grabadora que reciba.
 */
class PanelMacro extends JPanel implements ModoPanel, EstadoListener {

    private static final int REFRESCO_GRABACION_MS = 200;

    private final String titulo;
    private final GrabadoraService grabadora;
    private final ReproductorService reproductor;
    private final AtajoService atajos;
    private final AtajoConfigurable atajoGrabar;
    private final AtajoConfigurable atajoReproducir;
    private Runnable alCambiarOcupado = () -> { };

    private Grabacion grabacion = Grabacion.VACIA;
    private boolean reproduciendo = false;

    private final Etiqueta resumen = new Etiqueta("Todavía no hay nada grabado",
            Etiqueta.Rol.TEXTO, Tema.fuente(Font.BOLD, 15f));
    private final CampoNumerico repeticionesCampo = new CampoNumerico(1, 0, 1_000_000, 1, "veces");
    private final IndicadorEstado estado = new IndicadorEstado();
    private final BotonModerno grabarBtn = new BotonModerno("Grabar", BotonModerno.Variante.PELIGRO);
    private final BotonModerno reproducirBtn = new BotonModerno("Reproducir", BotonModerno.Variante.PRIMARIO);
    private final Timer relojGrabacion = new Timer(REFRESCO_GRABACION_MS, e -> mostrarProgresoGrabacion());

    /**
     * @param prefijoAtajos prefijo para guardar sus atajos, por ejemplo "atajo.mouse"
     */
    PanelMacro(String titulo, String descripcion, String prefijoAtajos,
               Atajo atajoGrabarPredeterminado, Atajo atajoReproducirPredeterminado,
               GrabadoraService grabadora, ReproductorService reproductor,
               AtajoService atajos, PreferenciasRepository preferencias) {
        super(new GridBagLayout());
        this.titulo = titulo;
        this.grabadora = grabadora;
        this.reproductor = reproductor;
        this.atajos = atajos;
        setOpaque(false);

        atajoGrabar = new AtajoConfigurable(prefijoAtajos + ".grabar", atajoGrabarPredeterminado,
                () -> alternarGrabacion(false), atajos, preferencias);
        atajoReproducir = new AtajoConfigurable(prefijoAtajos + ".reproducir", atajoReproducirPredeterminado,
                this::alternarReproduccion, atajos, preferencias);
        AtajoConfigurable.sonHermanos(atajoGrabar, atajoReproducir);
        atajoGrabar.setAlCambiar(this::actualizarControles);
        atajoReproducir.setAlCambiar(this::actualizarControles);

        atajos.agregarOyente(grabadora);
        reproductor.setListener(this);

        Diseno.Pila pila = new Diseno.Pila(this);
        pila.agregar(Diseno.tarjeta("Grabación", resumen, descripcion), 0);
        pila.agregar(Diseno.tarjeta("Repeticiones", repeticionesCampo, "0 = repetir hasta detener"), 12);
        pila.agregar(Diseno.dosColumnas(
                Diseno.tarjeta("Grabar / detener", atajoGrabar.selector(), null),
                Diseno.tarjeta("Reproducir / detener", atajoReproducir.selector(), null)), 12);
        pila.agregarRelleno();
        pila.agregar(estado, 18);
        pila.agregar(Diseno.dosColumnas(grabarBtn, reproducirBtn), 12);

        grabarBtn.addActionListener(e -> alternarGrabacion(true));
        reproducirBtn.addActionListener(e -> alternarReproduccion());
        estado.setEstado(atajos.estaDisponible()
                ? "Graba con " + atajoGrabar.nombre() + " y reproduce con " + atajoReproducir.nombre()
                : "Listo para grabar", IndicadorEstado.Tono.INACTIVO);
        actualizarControles();
    }

    // ---- ModoPanel ----

    @Override public String titulo() { return titulo; }
    @Override public JComponent vista() { return this; }
    @Override public List<String> clavesAtajo() { return List.of(atajoGrabar.clave(), atajoReproducir.clave()); }
    @Override public boolean estaOcupado() { return grabadora.estaGrabando() || reproduciendo; }

    @Override
    public void setAlCambiarOcupado(Runnable accion) {
        this.alCambiarOcupado = (accion != null) ? accion : () -> { };
    }

    @Override
    public void detenerTodo() {
        if (grabadora.estaGrabando()) grabadora.detener(false);
        reproductor.detener();
    }

    // ---- Grabar ----

    /**
     * @param desdeLaVentana true si se usó el botón: el clic sobre el botón
     *                       no debe quedar dentro de la grabación
     */
    private void alternarGrabacion(boolean desdeLaVentana) {
        if (reproduciendo) return;
        if (atajos.estaCapturando()) atajos.cancelarCaptura();

        if (grabadora.estaGrabando()) {
            relojGrabacion.stop();
            grabacion = grabadora.detener(desdeLaVentana);
            resumen.setText(describir(grabacion));
            estado.setEstado("Grabación lista", IndicadorEstado.Tono.INACTIVO);
        } else {
            grabadora.iniciar();
            relojGrabacion.start();
            mostrarProgresoGrabacion();
        }
        actualizarControles();
    }

    private void mostrarProgresoGrabacion() {
        estado.setEstado("Grabando…  " + segundos(grabadora.milisegundosGrabados())
                + "  ·  " + grabadora.cantidadEventos() + " acciones", IndicadorEstado.Tono.GRABANDO);
    }

    // ---- Reproducir ----

    private void alternarReproduccion() {
        if (grabadora.estaGrabando()) return;
        if (reproduciendo) {
            reproductor.detener();
            return;
        }
        if (grabacion.estaVacia()) return;
        if (atajos.estaCapturando()) atajos.cancelarCaptura();

        reproduciendo = true;
        atajos.setTolerarModificadores(true);
        reproductor.iniciar(grabacion, repeticionesCampo.getValor());
        actualizarControles();
    }

    // ---- EstadoListener (llegan desde el hilo de reproducción) ----

    @Override
    public void alCambiarEstado(String mensaje) {
        SwingUtilities.invokeLater(() -> estado.setEstado(mensaje, IndicadorEstado.Tono.ACTIVO));
    }

    @Override
    public void alTerminar(String mensajeFinal) {
        SwingUtilities.invokeLater(() -> {
            reproduciendo = false;
            atajos.setTolerarModificadores(false);
            estado.setEstado(mensajeFinal, IndicadorEstado.Tono.INACTIVO);
            actualizarControles();
        });
    }

    // ---- Estado de los controles ----

    private void actualizarControles() {
        boolean grabando = grabadora.estaGrabando();
        boolean conAtajos = atajos.estaDisponible();
        String g = conAtajos ? "  ·  " + atajoGrabar.nombre() : "";
        String r = conAtajos ? "  ·  " + atajoReproducir.nombre() : "";

        grabarBtn.setTextos(grabando ? "Detener" + g : "Grabar" + g, grabando ? "Detener" : "Grabar");
        reproducirBtn.setTextos(reproduciendo ? "Detener" + r : "Reproducir" + r,
                reproduciendo ? "Detener" : "Reproducir");

        grabarBtn.setEnabled(!reproduciendo);
        reproducirBtn.setEnabled(!grabando && (reproduciendo || !grabacion.estaVacia()));

        boolean libre = !grabando && !reproduciendo;
        repeticionesCampo.setEnabled(libre);
        atajoGrabar.setEnabled(libre);
        atajoReproducir.setEnabled(libre);
        alCambiarOcupado.run();
    }

    private String describir(Grabacion g) {
        StringBuilder sb = new StringBuilder(segundos(g.duracionMs()));
        sb.append("  ·  ").append(plural(g.cantidadClics(), "clic", "clics"));
        if (grabadora.incluyeTeclado()) {
            sb.append("  ·  ").append(plural(g.cantidadTeclas(), "tecla", "teclas"));
        }
        return sb.toString();
    }

    private static String segundos(long ms) {
        return String.format(Locale.forLanguageTag("es"), "%.1f s", ms / 1000.0);
    }

    private static String plural(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
