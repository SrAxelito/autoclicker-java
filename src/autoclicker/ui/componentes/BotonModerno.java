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
    private String textoCorto;

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

    /** Texto completo y una versión corta que se usa si el completo no cabe. */
    public void setTextos(String completo, String corto) {
        this.textoCorto = corto;
        setText(completo);
        repaint();
    }

    private String textoVisible(FontMetrics fm) {
        String texto = getText();
        if (textoCorto != null && fm.stringWidth(texto) > getWidth() - 24) return textoCorto;
        return texto;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (textoCorto != null) {
            FontMetrics fm = getFontMetrics(getFont());
            Insets in = getInsets();
            d.width = fm.stringWidth(textoCorto) + in.left + in.right;
        }
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);

        Color fondo;
        Color texto;
        if (!isEnabled()) {
            fondo = Tema.paleta().deshabilitado();
            texto = Tema.paleta().textoDeshabilitado();
        } else if (variante == Variante.PRIMARIO) {
            fondo = Tema.paleta().acento();
            texto = Color.WHITE;
        } else {
            fondo = Tema.paleta().peligroFondo();
            texto = Tema.paleta().peligro();
        }

        if (isEnabled()) {
            ButtonModel m = getModel();
            if (m.isPressed()) {
                fondo = Tema.mezclar(fondo, Color.BLACK, 0.12f);
            } else if (m.isRollover()) {
                fondo = variante == Variante.PRIMARIO
                        ? Tema.mezclar(fondo, Color.WHITE, 0.12f)
                        : Tema.mezclar(fondo, Tema.paleta().peligro(), 0.10f);
            }
        }

        float w = getWidth();
        float h = getHeight();
        g2.setColor(fondo);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, Tema.RADIO, Tema.RADIO));

        if (isFocusOwner() && isEnabled()) {
            g2.setColor(variante == Variante.PRIMARIO
                    ? Tema.mezclar(fondo, Color.WHITE, 0.55f)
                    : Tema.mezclar(Tema.paleta().peligro(), Tema.paleta().peligroFondo(), 0.5f));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, Tema.RADIO - 4, Tema.RADIO - 4));
        }

        g2.setFont(getFont());
        g2.setColor(texto);
        FontMetrics fm = g2.getFontMetrics();
        String visible = textoVisible(fm);
        int x = (getWidth() - fm.stringWidth(visible)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(visible, x, y);
        g2.dispose();
    }
}
