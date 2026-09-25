package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Botón redondo con un icono de engranaje, para abrir las opciones.
 */
public class BotonEngranaje extends JButton {

    private static final int TAMANO = 38;
    private static final int DIENTES = 8;

    public BotonEngranaje() {
        setToolTipText("Opciones");
        setPreferredSize(new Dimension(TAMANO, TAMANO));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        float w = getWidth();
        float h = getHeight();
        boolean sobre = getModel().isRollover() || getModel().isPressed();

        if (sobre) {
            g2.setColor(Tema.paleta().borde());
            g2.fill(new Ellipse2D.Float(0, 0, w, h));
        }
        if (isFocusOwner()) {
            g2.setColor(Tema.paleta().acento());
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new Ellipse2D.Float(1, 1, w - 2, h - 2));
        }

        g2.setColor(sobre ? Tema.paleta().texto() : Tema.paleta().textoSuave());
        g2.fill(engranaje(w / 2f, h / 2f));
        g2.dispose();
    }

    private static Area engranaje(float cx, float cy) {
        Area forma = new Area(new Ellipse2D.Float(cx - 7f, cy - 7f, 14f, 14f));
        RoundRectangle2D diente = new RoundRectangle2D.Float(cx - 2.2f, cy - 10f, 4.4f, 6f, 1.5f, 1.5f);
        for (int i = 0; i < DIENTES; i++) {
            AffineTransform giro = AffineTransform.getRotateInstance(i * Math.PI * 2 / DIENTES, cx, cy);
            forma.add(new Area(giro.createTransformedShape(diente)));
        }
        forma.subtract(new Area(new Ellipse2D.Float(cx - 3f, cy - 3f, 6f, 6f)));
        return forma;
    }
}
