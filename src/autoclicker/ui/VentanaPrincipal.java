package autoclicker.ui;

import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ClickerService;
import autoclicker.servicio.GrabadoraService;
import autoclicker.servicio.ReproductorService;
import autoclicker.ui.componentes.BotonEngranaje;
import autoclicker.ui.componentes.ControlSegmentado;
import autoclicker.ui.componentes.Etiqueta;
import autoclicker.ui.componentes.Logo;
import autoclicker.ui.componentes.PanelFondo;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Ventana principal: encabezado, pestañas para cambiar de modo y el
 * contenido del modo elegido.
 */
public class VentanaPrincipal extends JFrame {

    private static final int ANCHO_MINIMO = 500;

    private final List<ModoPanel> modos;
    private final AtajoService atajos;
    private final PreferenciasRepository preferencias;

    private final ControlSegmentado<ModoPanel> pestanas;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final BotonEngranaje opcionesBtn = new BotonEngranaje();
    private final JLabel logo = new JLabel();

    public VentanaPrincipal(AtajoService atajos, PreferenciasRepository preferencias,
                            ClickerService clicker,
                            GrabadoraService grabadoraMouse, ReproductorService reproductorMouse,
                            GrabadoraService grabadoraCompleta, ReproductorService reproductorCompleto) {
        super("AutoClicker");
        this.atajos = atajos;
        this.preferencias = preferencias;
        this.modos = List.of(
                new PanelAutoclicker(clicker, atajos, preferencias),
                new PanelMacro("Mouse",
                        "Graba movimientos, clics y rueda del mouse",
                        "atajo.mouse", AtajoService.F7, AtajoService.F8,
                        grabadoraMouse, reproductorMouse, atajos, preferencias),
                new PanelMacro("Mouse + Teclado",
                        "Graba el mouse y el teclado al mismo tiempo",
                        "atajo.completo", AtajoService.F9, AtajoService.F10,
                        grabadoraCompleta, reproductorCompleto, atajos, preferencias));

        String[] titulos = modos.stream().map(ModoPanel::titulo).toArray(String[]::new);
        pestanas = new ControlSegmentado<>(modos.toArray(new ModoPanel[0]), titulos);
        pestanas.alCambiar(this::mostrarModo);

        contenido.setOpaque(false);
        for (ModoPanel modo : modos) {
            contenido.add(modo.vista(), modo.titulo());
            modo.setAlCambiarOcupado(this::actualizarBloqueo);
        }

        construirInterfaz();
        mostrarModo(modos.get(0));
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
                modos.forEach(ModoPanel::detenerTodo);
                atajos.cerrar();
            }
        });
    }

    // ---- Modos ----

    private void mostrarModo(ModoPanel modo) {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        tarjetas.show(contenido, modo.titulo());
        atajos.activarSolo(modo.clavesAtajo());
    }

    /** Mientras un modo está trabajando no se puede cambiar de pestaña ni abrir opciones. */
    private void actualizarBloqueo() {
        boolean ocupado = modos.stream().anyMatch(ModoPanel::estaOcupado);
        pestanas.setEnabled(!ocupado);
        opcionesBtn.setEnabled(!ocupado);
    }

    private void abrirOpciones() {
        if (atajos.estaCapturando()) atajos.cancelarCaptura();
        new DialogoOpciones(this, preferencias).setVisible(true);
    }

    // ---- Construcción de la interfaz ----

    private void construirInterfaz() {
        PanelFondo raiz = new PanelFondo(new GridBagLayout());
        raiz.setBorder(new EmptyBorder(22, 22, 22, 22));
        raiz.setFocusable(true); // recibe el foco al abrir, así ningún campo aparece seleccionado

        Diseno.Pila pila = new Diseno.Pila(raiz);
        pila.agregar(encabezado(), 0);
        pila.agregar(pestanas, 18);
        pila.agregar(contenido, 18, 1);

        setContentPane(raiz);
    }

    private JComponent encabezado() {
        Etiqueta titulo = new Etiqueta("AutoClicker", Etiqueta.Rol.TEXTO, Tema.fuente(Font.BOLD, 22f));
        Etiqueta subtitulo = new Etiqueta("Automatiza clics, movimientos y teclas",
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

    /** El logo es una imagen, así que se vuelve a dibujar con el acento del tema nuevo. */
    private void actualizarColoresDelTema() {
        logo.setIcon(new ImageIcon(Logo.crear(44)));
        setIconImages(Logo.iconosVentana());
        repaint();
    }
}
