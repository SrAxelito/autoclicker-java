package autoclicker.ui;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import autoclicker.servicio.ClickerListener;
import autoclicker.servicio.ClickerService;
import autoclicker.ui.componentes.BotonModerno;
import autoclicker.ui.componentes.CampoNumerico;
import autoclicker.ui.componentes.ControlSegmentado;
import autoclicker.ui.componentes.IndicadorEstado;
import autoclicker.ui.componentes.Logo;
import autoclicker.ui.componentes.PanelTarjeta;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Interfaz gráfica. Solo lee lo que el usuario configura, se lo pasa
 * al servicio y muestra el estado que el servicio le reporta.
 */
public class VentanaAutoClicker extends JFrame implements ClickerListener {

    private static final int ANCHO_MINIMO = 480;

    private final ClickerService servicio;

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

    public VentanaAutoClicker(ClickerService servicio) {
        super("AutoClicker");
        this.servicio = servicio;

        construirInterfaz();

        iniciarBtn.addActionListener(e -> iniciar());
        detenerBtn.addActionListener(e -> servicio.detener());
        detenerBtn.setEnabled(false);

        setIconImages(Logo.iconosVentana());
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
        });

        servicio.setListener(this);
    }

    // ---- Acciones ----

    private void iniciar() {
        try {
            ConfiguracionClics config = leerConfiguracion();
            habilitarControles(false);
            servicio.iniciar(config);
        } catch (IllegalArgumentException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
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
        JPanel raiz = new JPanel(new GridBagLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(new EmptyBorder(22, 22, 18, 22));
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
        fila.agregar(estado, 18);
        fila.agregar(dosColumnas(iniciarBtn, detenerBtn), 12);
        fila.agregar(pie(), 14);

        setContentPane(raiz);
    }

    private JComponent encabezado() {
        JLabel titulo = new JLabel("AutoClicker");
        titulo.setFont(Tema.fuente(Font.BOLD, 22f));
        titulo.setForeground(Tema.TEXTO);

        JLabel subtitulo = new JLabel("Configura el clic y deja que trabaje por ti");
        subtitulo.setFont(Tema.fuente(Font.PLAIN, 13f));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        JLabel logo = new JLabel(new ImageIcon(Logo.crear(44)));

        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);
        panel.add(logo, BorderLayout.WEST);
        panel.add(textos, BorderLayout.CENTER);
        return panel;
    }

    private JComponent tarjeta(String titulo, JComponent control, String nota) {
        PanelTarjeta tarjeta = new PanelTarjeta(titulo);
        tarjeta.getContenido().add(control, BorderLayout.NORTH);
        if (nota != null) {
            JLabel etiqueta = new JLabel(nota);
            etiqueta.setFont(Tema.fuente(Font.PLAIN, 12f));
            etiqueta.setForeground(Tema.TEXTO_SUAVE);
            tarjeta.getContenido().add(etiqueta, BorderLayout.CENTER);
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

    private JComponent pie() {
        JLabel ayuda = new JLabel(
                "Freno de seguridad: lleva el mouse a la esquina superior izquierda.",
                SwingConstants.CENTER);
        ayuda.setFont(Tema.fuente(Font.PLAIN, 12f));
        ayuda.setForeground(Tema.TEXTO_SUAVE);
        return ayuda;
    }

    private void habilitarControles(boolean habilitar) {
        iniciarBtn.setEnabled(habilitar);
        detenerBtn.setEnabled(!habilitar);
        intervaloCampo.setEnabled(habilitar);
        esperaCampo.setEnabled(habilitar);
        clicsCampo.setEnabled(habilitar);
        botonControl.setEnabled(habilitar);
        tipoControl.setEnabled(habilitar);
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
