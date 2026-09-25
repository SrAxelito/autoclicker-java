package autoclicker.ui.componentes;

import autoclicker.ui.tema.Paleta;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Barra de estado con un punto de color: gris cuando está quieto, verde
 * cuando está trabajando y rojo cuando está grabando, con una pequeña animación.
 */
public class IndicadorEstado extends JComponent {

    public enum Tono { INACTIVO, ACTIVO, GRABANDO }

    private String texto = "Listo para iniciar";
    private Tono tono = Tono.INACTIVO;
    private float fase = 0f;
    private final Timer animacion = new Timer(40, e -> {
        fase = (fase + 0.04f) % 1f;
        repaint();
    });

    public IndicadorEstado() {
        setFont(Tema.fuente(Font.BOLD, 13f));
    }

    public void setEstado(String nuevoTexto, Tono nuevoTono) {
        texto = nuevoTexto;
        if (nuevoTono != tono) {
            tono = nuevoTono;
            if (tono == Tono.INACTIVO) {
                animacion.stop();
                fase = 0f;
            } else {
                animacion.start();
            }
        }
        repaint();
    }

    public void setEstado(String nuevoTexto, boolean activo) {
        setEstado(nuevoTexto, activo ? Tono.ACTIVO : Tono.INACTIVO);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 44);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        Paleta p = Tema.paleta();
        float w = getWidth();
        float h = getHeight();

        Color fondo;
        Color borde;
        Color punto;
        Color colorTexto;
        switch (tono) {
            case ACTIVO -> {
                fondo = p.exitoFondo(); borde = p.exitoBorde(); punto = p.exito();
                colorTexto = Tema.mezclar(p.exito(), p.texto(), 0.35f);
            }
            case GRABANDO -> {
                fondo = p.peligroFondo(); borde = Tema.mezclar(p.peligroFondo(), p.peligro(), 0.3f);
                punto = p.peligro(); colorTexto = Tema.mezclar(p.peligro(), p.texto(), 0.35f);
            }
            default -> {
                fondo = p.tarjeta(); borde = p.borde(); punto = p.textoDeshabilitado();
                colorTexto = p.texto();
            }
        }

        RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Tema.RADIO, Tema.RADIO);
        g2.setColor(fondo);
        g2.fill(forma);
        g2.setColor(borde);
        g2.draw(forma);

        float cx = 22;
        float cy = h / 2f;
        if (tono != Tono.INACTIVO) {
            float radioHalo = 4f + 7f * fase;
            g2.setColor(new Color(punto.getRed(), punto.getGreen(), punto.getBlue(),
                    Math.round(90 * (1f - fase))));
            g2.fill(new Ellipse2D.Float(cx - radioHalo, cy - radioHalo, radioHalo * 2, radioHalo * 2));
        }
        g2.setColor(punto);
        g2.fill(new Ellipse2D.Float(cx - 4, cy - 4, 8, 8));

        g2.setFont(getFont());
        g2.setColor(colorTexto);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, 40, Math.round((h - fm.getHeight()) / 2f) + fm.getAscent());
        g2.dispose();
    }
}
