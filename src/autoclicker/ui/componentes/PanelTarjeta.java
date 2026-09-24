package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Tarjeta blanca con esquinas redondeadas y un título pequeño arriba.
 * El contenido se agrega con {@link #getContenido()}.
 */
public class PanelTarjeta extends JPanel {

    private final JPanel contenido = new JPanel(new BorderLayout(0, 8));

    public PanelTarjeta(String titulo) {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 16, 16));

        JLabel etiqueta = new JLabel(titulo.toUpperCase());
        etiqueta.setFont(Tema.fuenteEtiqueta());
        etiqueta.setForeground(Tema.TEXTO_SUAVE);

        contenido.setOpaque(false);

        add(etiqueta, BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
    }

    public JPanel getContenido() {
        return contenido;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        RoundRectangle2D forma = new RoundRectangle2D.Float(
                0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, Tema.RADIO, Tema.RADIO);
        g2.setColor(Tema.TARJETA);
        g2.fill(forma);
        g2.setColor(Tema.BORDE);
        g2.draw(forma);
        g2.dispose();
    }
}
