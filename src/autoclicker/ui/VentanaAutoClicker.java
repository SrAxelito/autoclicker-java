package autoclicker.ui;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ClickerListener;
import autoclicker.servicio.ClickerService;
import autoclicker.ui.componentes.BotonEngranaje;
import autoclicker.ui.componentes.BotonModerno;
import autoclicker.ui.componentes.CampoNumerico;
import autoclicker.ui.componentes.ControlSegmentado;
import autoclicker.ui.componentes.Etiqueta;
import autoclicker.ui.componentes.IndicadorEstado;
import autoclicker.ui.componentes.Logo;
import autoclicker.ui.componentes.PanelFondo;
import autoclicker.ui.componentes.PanelTarjeta;
import autoclicker.ui.componentes.SelectorAtajo;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal. Lee lo que el usuario configura, se lo pasa a los
 * servicios y muestra el estado que el servicio de clics le reporta.
 */
public class VentanaAutoClicker extends JFrame implements ClickerListener {

    private static final int ANCHO_MINIMO = 480;

    private final ClickerService servicio;
    private final AtajoService atajos;
    private final PreferenciasRepository preferencias;
    private Atajo atajoActual;

    private final CampoNumerico intervaloCampo = new CampoNumerico(100, 1, 600_000, 10, "ms");
    private final CampoNumerico esperaCampo    = new CampoNumerico(3, 0, 60, 1, "s");
    private final CampoNumerico clicsCampo     = new CampoNumerico(0, 0, 10_000_000, 1, "clics");
    private final ControlSegmentado<BotonMouse> botonControl =
            new ControlSegmentado<>(BotonMouse.values());
    private final ControlSegmentado<Boolean> tipoControl =
            new ControlSegmentado<>(new Boolean[]{false, true}, new String[]{"Simple", "Doble"});
    private final SelectorAtajo selectorAtajo = new SelectorAtajo();

    private final BotonModerno iniciarBtn = new BotonModerno("Iniciar", BotonModerno.Variante.PRIMARIO);
    private final BotonModerno detenerBtn = new BotonModerno("Detener", BotonModerno.Variante.PELIGRO);
    private final BotonEngranaje opcionesBtn = new BotonEngranaje();
    private final IndicadorEstado estado = new IndicadorEstado();
    private final JLabel logo = new JLabel();

    public VentanaAutoClicker(ClickerService servicio, AtajoService atajos, PreferenciasRepository preferencias) {
        super("AutoClicker");
        this.servicio = servicio;
        this.atajos = atajos;
        this.preferencias = preferencias;
        this.atajoActual = preferencias.cargarAtajo().orElse(AtajoService.PREDETERMINADO);

        construirInterfaz();
        configurarAtajo();

        iniciarBtn.addActionListener(e -> iniciar());
        detenerBtn.addActionListener(e -> servicio.detener());
        detenerBtn.setEnabled(false);
        opcionesBtn.addActionListener(e -> abrirOpciones());

        actualizarColoresDelTema();
        Tema.alCambiar(this::actualizarColoresDelTema);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        pack();
        if (getWidth() < ANCHO_MINIMO) setSize(ANCHO_MINIMO, getHeight());
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                getContentPane().requestFocusInWindow();
            }
            @Override public void windowClosing(WindowEvent e) {
                servicio.detener();
                atajos.cerrar();
            }
        });

        servicio.setListener(this);
    }

    // ---- Acciones ----

    private void iniciar() {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        try {
            ConfiguracionClics config = leerConfiguracion();
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

    private ConfiguracionClics leerConfiguracion() {
        return new ConfiguracionClics(
                intervaloCampo.getValor(),
                clicsCampo.getValor(),
                esperaCampo.getValor(),
                botonControl.getSeleccion(),
                tipoControl.getSeleccion());
    }

    private void abrirOpciones() {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        new DialogoOpciones(this, preferencias).setVisible(true);
    }

    // ---- Atajo global ----

    private void configurarAtajo() {
        atajos.setAtajo(atajoActual);
        atajos.setAlActivar(this::alternar);
        selectorAtajo.setDisponible(atajos.estaDisponible());
        selectorAtajo.setAlActivar(this::alternarCaptura);
        mostrarAtajo();
    }

    private void alternarCaptura() {
        if (atajos.estaCapturando()) {
            atajos.cancelarCaptura();
            return;
        }
        selectorAtajo.setCapturando(true);
        atajos.capturar(nuevo -> {
            selectorAtajo.setCapturando(false);
            if (nuevo == null) return; // cancelado con Esc
            atajoActual = nuevo;
            atajos.setAtajo(nuevo);
            preferencias.guardarAtajo(nuevo);
            mostrarAtajo();
        });
    }

    private void mostrarAtajo() {
        selectorAtajo.setNombre(atajoActual.nombre());
        boolean conAtajo = atajos.estaDisponible();
        iniciarBtn.setText(conAtajo ? "Iniciar  ·  " + atajoActual.nombre() : "Iniciar");
        detenerBtn.setText(conAtajo ? "Detener  ·  " + atajoActual.nombre() : "Detener");
    }

    // ---- ClickerListener (llegan desde el hilo de clics) ----

    @Override
    public void alCambiarEstado(String mensaje) {
        SwingUtilities.invokeLater(() -> estado.setEstado(mensaje, true));
    }

    @Override
    public void alTerminar(String mensajeFinal) {
        SwingUtilities.invokeLater(() -> {
            estado.setEstado(mensajeFinal, false);
            habilitarControles(true);
        });
    }

    // ---- Construcción de la interfaz ----

    private void construirInterfaz() {
        PanelFondo raiz = new PanelFondo(new GridBagLayout());
        raiz.setBorder(new EmptyBorder(22, 22, 22, 22));
        raiz.setFocusable(true); // recibe el foco al abrir, así ningún campo aparece seleccionado

        Agregador fila = new Agregador(raiz);
        fila.agregar(encabezado(), 0);
        fila.agregar(dosColumnas(
                tarjeta("Intervalo entre clics", intervaloCampo, null),
                tarjeta("Espera inicial", esperaCampo, null)), 18);
        fila.agregar(tarjeta("Botón del mouse", botonControl, null), 12);
        fila.agregar(dosColumnas(
                tarjeta("Tipo de clic", tipoControl, null),
                tarjeta("Repeticiones", clicsCampo, "0 = repetir hasta detener")), 12);
        fila.agregar(tarjeta("Atajo para iniciar / detener", selectorAtajo, notaAtajo()), 12);
        fila.agregar(estado, 18);
        fila.agregar(dosColumnas(iniciarBtn, detenerBtn), 12);

        setContentPane(raiz);
    }

    private String notaAtajo() {
        return atajos.estaDisponible()
                ? "Funciona aunque la ventana no esté activa"
                : "No se pudo activar la escucha global del teclado";
    }

    private JComponent encabezado() {
        Etiqueta titulo = new Etiqueta("AutoClicker", Etiqueta.Rol.TEXTO, Tema.fuente(Font.BOLD, 22f));
        Etiqueta subtitulo = new Etiqueta("Configura el clic y deja que trabaje por ti",
                Etiqueta.Rol.SUAVE, Tema.fuente(Font.PLAIN, 13f));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        JPanel esquina = new JPanel(new BorderLayout());
        esquina.setOpaque(false);
        esquina.add(opcionesBtn, BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);
        panel.add(logo, BorderLayout.WEST);
        panel.add(textos, BorderLayout.CENTER);
        panel.add(esquina, BorderLayout.EAST);
        return panel;
    }

    private JComponent tarjeta(String titulo, JComponent control, String nota) {
        PanelTarjeta tarjeta = new PanelTarjeta(titulo);
        tarjeta.getContenido().add(control, BorderLayout.NORTH);
        if (nota != null) {
            tarjeta.getContenido().add(
                    new Etiqueta(nota, Etiqueta.Rol.SUAVE, Tema.fuente(Font.PLAIN, 12f)),
                    BorderLayout.CENTER);
        }
        return tarjeta;
    }

    private JComponent dosColumnas(JComponent izquierda, JComponent derecha) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setOpaque(false);
        panel.add(izquierda);
        panel.add(derecha);
        return panel;
    }

    /** El logo es una imagen, así que se vuelve a dibujar con el acento del tema nuevo. */
    private void actualizarColoresDelTema() {
        logo.setIcon(new ImageIcon(Logo.crear(44)));
        setIconImages(Logo.iconosVentana());
        repaint();
    }

    private void habilitarControles(boolean habilitar) {
        iniciarBtn.setEnabled(habilitar);
        detenerBtn.setEnabled(!habilitar);
        intervaloCampo.setEnabled(habilitar);
        esperaCampo.setEnabled(habilitar);
        clicsCampo.setEnabled(habilitar);
        botonControl.setEnabled(habilitar);
        tipoControl.setEnabled(habilitar);
        selectorAtajo.setEnabled(habilitar);
        opcionesBtn.setEnabled(habilitar);
    }

    /** Apila componentes verticalmente ocupando todo el ancho. */
    private static final class Agregador {
        private final JPanel panel;
        private int filaActual = 0;

        Agregador(JPanel panel) {
            this.panel = panel;
        }

        void agregar(JComponent componente, int espacioArriba) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = filaActual++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(espacioArriba, 0, 0, 0);
            panel.add(componente, c);
        }
    }
}
