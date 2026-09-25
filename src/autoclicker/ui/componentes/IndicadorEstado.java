package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Barra de estado con un punto de color: gris cuando está detenido
 * y verde con una pequeña animación cuando está trabajando.
 */
public class IndicadorEstado extends JComponent {

    private String texto = "Listo para iniciar";
    private boolean activo = false;
    private float fase = 0f;
    private final Timer animacion = new Timer(40, e -> {
        fase = (fase + 0.04f) % 1f;
        repaint();
    });

    public IndicadorEstado() {
        setFont(Tema.fuente(Font.BOLD, 13f));
    }

    public void setEstado(String nuevoTexto, boolean estaActivo) {
        texto = nuevoTexto;
        if (estaActivo != activo) {
            activo = estaActivo;
            if (activo) animacion.start(); else { animacion.stop(); fase = 0f; }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 44);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        float w = getWidth();
        float h = getHeight();

        RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Tema.RADIO, Tema.RADIO);
        g2.setColor(activo ? Tema.paleta().exitoFondo() : Tema.paleta().tarjeta());
        g2.fill(forma);
        g2.setColor(activo ? Tema.paleta().exitoBorde() : Tema.paleta().borde());
        g2.draw(forma);

        float cx = 22;
        float cy = h / 2f;
        Color colorPunto = activo ? Tema.paleta().exito() : Tema.paleta().textoDeshabilitado();

        if (activo) {
            float radioHalo = 4f + 7f * fase;
            g2.setColor(new Color(colorPunto.getRed(), colorPunto.getGreen(), colorPunto.getBlue(),
                    Math.round(90 * (1f - fase))));
            g2.fill(new Ellipse2D.Float(cx - radioHalo, cy - radioHalo, radioHalo * 2, radioHalo * 2));
        }
        g2.setColor(colorPunto);
        g2.fill(new Ellipse2D.Float(cx - 4, cy - 4, 8, 8));

        g2.setFont(getFont());
        g2.setColor(activo ? Tema.mezclar(Tema.paleta().exito(), Color.BLACK, 0.25f) : Tema.paleta().texto());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, 40, Math.round((h - fm.getHeight()) / 2f) + fm.getAscent());
        g2.dispose();
    }
}
