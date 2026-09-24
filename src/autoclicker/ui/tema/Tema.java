package autoclicker.ui.tema;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.Map;

/**
 * Paleta de colores, tipografía y medidas de la interfaz.
 * Todo el estilo visual se cambia desde aquí.
 */
public final class Tema {

    private Tema() { }

    // ---- Colores base ----
    public static final Color FONDO        = new Color(0xF3F4F8);
    public static final Color TARJETA      = Color.WHITE;
    public static final Color BORDE        = new Color(0xE3E6EE);
    public static final Color CAMPO        = new Color(0xF5F6FA);
    public static final Color TEXTO        = new Color(0x1B1E28);
    public static final Color TEXTO_SUAVE  = new Color(0x6B7183);

    // ---- Colores de acento y estados ----
    public static final Color ACENTO             = new Color(0x4F6BFF);
    public static final Color EXITO              = new Color(0x16A34A);
    public static final Color EXITO_FONDO        = new Color(0xECFDF3);
    public static final Color EXITO_BORDE        = new Color(0xBBF0CF);
    public static final Color PELIGRO            = new Color(0xDC2626);
    public static final Color PELIGRO_FONDO      = new Color(0xFDECEC);
    public static final Color DESHABILITADO      = new Color(0xE9EBF1);
    public static final Color TEXTO_DESHABILITADO = new Color(0xA3A8B6);

    // ---- Medidas ----
    public static final int RADIO = 14;
    public static final int RADIO_CAMPO = 10;

    // ---- Tipografía ----
    private static final String FAMILIA = elegirFamilia();

    private static String elegirFamilia() {
        String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String preferida : new String[]{"Segoe UI", "Inter", "Roboto", "Helvetica Neue"}) {
            if (Arrays.asList(disponibles).contains(preferida)) return preferida;
        }
        return Font.SANS_SERIF;
    }

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA, estilo, 1).deriveFont(tamano);
    }

    /** Fuente pequeña en mayúsculas con letras espaciadas, para títulos de sección. */
    public static Font fuenteEtiqueta() {
        return fuente(Font.BOLD, 11f).deriveFont(Map.of(TextAttribute.TRACKING, 0.08f));
    }

    // ---- Utilidades de dibujo ----

    public static void suavizar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Mezcla dos colores: t = 0 devuelve a, t = 1 devuelve b. */
    public static Color mezclar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }
}
