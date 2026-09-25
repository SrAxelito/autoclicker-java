package autoclicker.ui.tema;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tema activo, tipografía y medidas de la interfaz.
 * Los componentes leen los colores con {@code Tema.paleta()} al dibujarse,
 * así que al cambiar de tema basta con repintar.
 */
public final class Tema {

    private Tema() { }

    // ---- Tema activo ----

    private static volatile TemaVisual actual = TemaVisual.CLARO;
    private static final List<Runnable> oyentes = new CopyOnWriteArrayList<>();

    public static Paleta paleta() {
        return actual.paleta();
    }

    public static TemaVisual actual() {
        return actual;
    }

    public static void aplicar(TemaVisual nuevo) {
        if (nuevo == null || nuevo == actual) return;
        actual = nuevo;
        oyentes.forEach(Runnable::run);
    }

    /** Se ejecuta cada vez que cambia el tema. */
    public static void alCambiar(Runnable accion) {
        oyentes.add(accion);
    }

    // ---- Medidas ----

    public static final int RADIO = 14;
    public static final int RADIO_CAMPO = 10;

    // ---- Tipografía ----

    private static final String FAMILIA = elegirFamilia();

    private static String elegirFamilia() {
        List<String> disponibles = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
        for (String preferida : new String[]{"Segoe UI", "Inter", "Roboto", "Helvetica Neue"}) {
            if (disponibles.contains(preferida)) return preferida;
        }
        return Font.SANS_SERIF;
    }

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA, estilo, 1).deriveFont(tamano);
    }

    /** Fuente pequeña con letras espaciadas, para títulos de sección. */
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
