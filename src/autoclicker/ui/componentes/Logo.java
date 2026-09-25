package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Dibuja el logo de la aplicación (un cursor sobre un cuadro redondeado)
 * para usarlo en el encabezado y como icono de la ventana.
 */
public final class Logo {

    private Logo() { }

    public static BufferedImage crear(int tamano) {
        BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Tema.suavizar(g);
        float s = tamano;

        g.setColor(Tema.paleta().acento());
        g.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.32f, s * 0.32f));

        // Ondas del clic
        g.setColor(new Color(255, 255, 255, 90));
        g.setStroke(new BasicStroke(Math.max(1f, s * 0.06f)));
        g.draw(new Ellipse2D.Float(s * 0.18f, s * 0.18f, s * 0.36f, s * 0.36f));

        // Cursor
        Path2D cursor = new Path2D.Float();
        cursor.moveTo(s * 0.36f, s * 0.30f);
        cursor.lineTo(s * 0.36f, s * 0.80f);
        cursor.lineTo(s * 0.48f, s * 0.67f);
        cursor.lineTo(s * 0.57f, s * 0.84f);
        cursor.lineTo(s * 0.65f, s * 0.80f);
        cursor.lineTo(s * 0.56f, s * 0.63f);
        cursor.lineTo(s * 0.73f, s * 0.63f);
        cursor.closePath();
        g.setColor(Color.WHITE);
        g.fill(cursor);

        g.dispose();
        return img;
    }

    public static List<Image> iconosVentana() {
        return List.of(crear(16), crear(32), crear(48), crear(64));
    }
}
