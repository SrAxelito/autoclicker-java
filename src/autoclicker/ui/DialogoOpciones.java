package autoclicker.ui;

import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.ui.componentes.BotonModerno;
import autoclicker.ui.componentes.ControlSegmentado;
import autoclicker.ui.componentes.Etiqueta;
import autoclicker.ui.componentes.PanelFondo;
import autoclicker.ui.componentes.PanelTarjeta;
import autoclicker.ui.tema.Tema;
import autoclicker.ui.tema.TemaVisual;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Ventana de opciones que se abre con el engranaje.
 * Por ahora permite cambiar el tema; está pensada para crecer.
 */
public class DialogoOpciones extends JDialog {

    private static final int ANCHO_MINIMO = 380;

    public DialogoOpciones(JFrame duenio, PreferenciasRepository preferencias) {
        super(duenio, "Opciones", true);

        PanelFondo raiz = new PanelFondo(new GridBagLayout());
        raiz.setBorder(new EmptyBorder(22, 22, 20, 22));

        Etiqueta titulo = new Etiqueta("Opciones", Etiqueta.Rol.TEXTO, Tema.fuente(Font.BOLD, 20f));
        Etiqueta subtitulo = new Etiqueta("Personaliza cómo se ve la aplicación",
                Etiqueta.Rol.SUAVE, Tema.fuente(Font.PLAIN, 13f));

        ControlSegmentado<TemaVisual> selectorTema = new ControlSegmentado<>(TemaVisual.values());
        selectorTema.setSeleccion(Tema.actual());
        selectorTema.alCambiar(tema -> {
            Tema.aplicar(tema);
            preferencias.guardarTema(tema.name());
            repaint();
        });

        PanelTarjeta tarjetaTema = new PanelTarjeta("Tema");
        tarjetaTema.getContenido().add(selectorTema, BorderLayout.NORTH);

        BotonModerno listo = new BotonModerno("Listo", BotonModerno.Variante.PRIMARIO);
        listo.addActionListener(e -> dispose());

        agregar(raiz, titulo, 0, 0);
        agregar(raiz, subtitulo, 1, 2);
        agregar(raiz, tarjetaTema, 2, 18);
        agregar(raiz, listo, 3, 18);

        // Esc cierra la ventana
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(listo);

        setContentPane(raiz);
        setAlwaysOnTop(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        if (getWidth() < ANCHO_MINIMO) setSize(ANCHO_MINIMO, getHeight());
        setLocationRelativeTo(duenio);
    }

    private static void agregar(JPanel panel, JComponent componente, int fila, int espacioArriba) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fila;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(espacioArriba, 0, 0, 0);
        panel.add(componente, c);
    }
}
