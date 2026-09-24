package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Botón plano con esquinas redondeadas y efecto al pasar el mouse.
 */
public class BotonModerno extends JButton {

    public enum Variante { PRIMARIO, PELIGRO }

    private final Variante variante;

    public BotonModerno(String texto, Variante variante) {
        super(texto);
        this.variante = variante;
        setFont(Tema.fuente(Font.BOLD, 14f));
        setBorder(new EmptyBorder(13, 22, 13, 22));
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

        Color fondo;
        Color texto;
        if (!isEnabled()) {
            fondo = Tema.DESHABILITADO;
            texto = Tema.TEXTO_DESHABILITADO;
        } else if (variante == Variante.PRIMARIO) {
            fondo = Tema.ACENTO;
            texto = Color.WHITE;
        } else {
            fondo = Tema.PELIGRO_FONDO;
            texto = Tema.PELIGRO;
        }

        if (isEnabled()) {
            ButtonModel m = getModel();
            if (m.isPressed()) {
                fondo = Tema.mezclar(fondo, Color.BLACK, 0.12f);
            } else if (m.isRollover()) {
                fondo = variante == Variante.PRIMARIO
                        ? Tema.mezclar(fondo, Color.WHITE, 0.12f)
                        : Tema.mezclar(fondo, Tema.PELIGRO, 0.10f);
            }
        }

        float w = getWidth();
        float h = getHeight();
        g2.setColor(fondo);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, Tema.RADIO, Tema.RADIO));

        if (isFocusOwner() && isEnabled()) {
            g2.setColor(variante == Variante.PRIMARIO
                    ? Tema.mezclar(fondo, Color.WHITE, 0.55f)
                    : Tema.mezclar(Tema.PELIGRO, Color.WHITE, 0.55f));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, Tema.RADIO - 4, Tema.RADIO - 4));
        }

        g2.setFont(getFont());
        g2.setColor(texto);
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}
