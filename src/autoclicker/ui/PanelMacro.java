package autoclicker.ui;

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

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Font;
import java.util.List;
import java.util.Locale;

/**
 * Pestaña de grabación: graba lo que hace el usuario y lo reproduce
 * las veces indicadas. Es la misma clase para "Mouse", "Teclado" y
 * "Mouse + Teclado"; lo que cambia entre ellas viene en el {@link ModoGrabacion}.
 * Las repeticiones se guardan al reproducir y al cerrar la ventana; la
 * grabación en sí no se guarda y se pierde al cerrar el programa.
 */
class PanelMacro extends PanelModo implements EstadoListener {

    private static final int REFRESCO_GRABACION_MS = 200;

    private final String titulo;
    private final GrabadoraService grabadora;
    private final ReproductorService reproductor;
    private final AtajoService atajos;
    private final PreferenciasRepository preferencias;
    private final String claveRepeticiones;
    private final AtajoConfigurable atajoGrabar;
    private final AtajoConfigurable atajoReproducir;

    private Grabacion grabacion = Grabacion.VACIA;

    private final Etiqueta resumen = new Etiqueta("Todavía no hay nada grabado",
            Etiqueta.Rol.TEXTO, Tema.fuente(Font.BOLD, 15f));
    // El valor con el que nace el campo es el predeterminado de la primera vez
    private final CampoNumerico repeticionesCampo = new CampoNumerico(1, 0, 1_000_000, 1, "veces");
    private final IndicadorEstado estado = new IndicadorEstado();
    private final BotonModerno grabarBtn = new BotonModerno("Grabar", BotonModerno.Variante.PELIGRO);
    private final BotonModerno reproducirBtn = new BotonModerno("Reproducir", BotonModerno.Variante.PRIMARIO);
    private final Timer relojGrabacion = new Timer(REFRESCO_GRABACION_MS, e -> mostrarProgresoGrabacion());

    PanelMacro(ModoGrabacion modo, AtajoService atajos, PreferenciasRepository preferencias) {
        this.titulo = modo.titulo();
        this.grabadora = modo.grabadora();
        this.reproductor = modo.reproductor();
        this.atajos = atajos;
        this.preferencias = preferencias;
        this.claveRepeticiones = modo.claveRepeticiones();

        atajoGrabar = new AtajoConfigurable(modo.prefijoAtajos() + ".grabar", modo.atajoGrabar(),
                () -> alternarGrabacion(false), atajos, preferencias);
        atajoReproducir = new AtajoConfigurable(modo.prefijoAtajos() + ".reproducir", modo.atajoReproducir(),
                this::alternarReproduccion, atajos, preferencias);
        AtajoConfigurable.sonHermanos(atajoGrabar, atajoReproducir);
        atajoGrabar.setAlCambiar(this::actualizarControles);
        atajoReproducir.setAlCambiar(this::actualizarControles);

        repeticionesCampo.setValor(
                preferencias.cargarRepeticiones(claveRepeticiones, repeticionesCampo.getValor()));

        atajos.agregarOyente(grabadora);
        reproductor.setListener(this);

        Diseno.Pila pila = new Diseno.Pila(this);
        pila.agregar(Diseno.tarjeta("Grabación", resumen, modo.descripcion()), 0);
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
    @Override public List<String> clavesAtajo() { return List.of(atajoGrabar.clave(), atajoReproducir.clave()); }
    /** Los servicios son la única fuente de verdad: aquí no se guarda una copia del estado. */
    @Override public boolean estaOcupado() { return grabadora.estaGrabando() || reproductor.estaCorriendo(); }

    @Override
    public void detenerTodo() {
        if (grabadora.estaGrabando()) grabadora.detener(false);
        reproductor.detener();
    }

    @Override
    public void guardarParametros() {
        preferencias.guardarRepeticiones(claveRepeticiones, repeticionesCampo.getValor());
    }

    // ---- Grabar ----

    /**
     * @param desdeLaVentana true si se usó el botón: el clic sobre el botón
     *                       no debe quedar dentro de la grabación
     */
    private void alternarGrabacion(boolean desdeLaVentana) {
        if (reproductor.estaCorriendo()) return;
        if (atajos.estaCapturando()) atajos.cancelarCaptura();

        if (grabadora.estaGrabando()) {
            relojGrabacion.stop();
            grabacion = grabadora.detener(desdeLaVentana);
            if (grabacion.estaVacia()) {
                resumen.setText("Todavía no hay nada grabado");
                estado.setEstado("No se grabó ninguna acción", IndicadorEstado.Tono.INACTIVO);
            } else {
                resumen.setText(describir(grabacion));
                estado.setEstado("Grabación lista", IndicadorEstado.Tono.INACTIVO);
            }
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
        if (reproductor.estaCorriendo()) {
            reproductor.detener();
            return;
        }
        if (grabacion.estaVacia()) return;
        if (atajos.estaCapturando()) atajos.cancelarCaptura();

        // Si no se inició no habrá aviso de final: la interfaz se deja como está.
        int repeticiones = repeticionesCampo.getValor();
        if (!reproductor.iniciar(grabacion, repeticiones)) return;
        atajos.setTolerarModificadores(true);
        actualizarControles();
        // Se guarda aquí además de al cerrar, por si el programa no se cierra con la X.
        preferencias.guardarRepeticiones(claveRepeticiones, repeticiones);
    }

    // ---- EstadoListener (llegan desde el hilo de reproducción) ----

    @Override
    public void alCambiarEstado(String mensaje) {
        SwingUtilities.invokeLater(() -> estado.setEstado(mensaje, IndicadorEstado.Tono.ACTIVO));
    }

    @Override
    public void alTerminar(String mensajeFinal) {
        SwingUtilities.invokeLater(() -> {
            // Aviso atrasado de la reproducción anterior: ya se inició otra y la
            // interfaz la está mostrando, así que este final no debe desbloquear nada.
            if (reproductor.estaCorriendo()) return;
            atajos.setTolerarModificadores(false);
            // Si en ese instante ya se empezó a grabar, el estado de grabación manda.
            if (!grabadora.estaGrabando()) {
                estado.setEstado(mensajeFinal, IndicadorEstado.Tono.INACTIVO);
            }
            actualizarControles();
        });
    }

    // ---- Estado de los controles ----

    private void actualizarControles() {
        boolean grabando = grabadora.estaGrabando();
        boolean reproduciendo = reproductor.estaCorriendo();
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
        avisarCambioDeOcupado();
    }

    private String describir(Grabacion g) {
        StringBuilder sb = new StringBuilder(segundos(g.duracionMs()));
        if (grabadora.tipo().incluyeMouse()) {
            sb.append("  ·  ").append(plural(g.cantidadClics(), "clic", "clics"));
        }
        if (grabadora.tipo().incluyeTeclado()) {
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