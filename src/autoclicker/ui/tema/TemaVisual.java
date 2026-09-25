package autoclicker.ui.tema;

import java.awt.Color;

/**
 * Temas disponibles para la interfaz.
 */
public enum TemaVisual {

    CLARO("Claro", new Paleta(
            new Color(0xF3F4F8),   // fondo
            Color.WHITE,           // tarjeta
            new Color(0xE3E6EE),   // borde
            new Color(0xF5F6FA),   // campo
            new Color(0x1B1E28),   // texto
            new Color(0x6B7183),   // textoSuave
            new Color(0x4F6BFF),   // acento
            new Color(0x16A34A),   // exito
            new Color(0xECFDF3),   // exitoFondo
            new Color(0xBBF0CF),   // exitoBorde
            new Color(0xDC2626),   // peligro
            new Color(0xFDECEC),   // peligroFondo
            new Color(0xE9EBF1),   // deshabilitado
            new Color(0xA3A8B6))), // textoDeshabilitado

    OSCURO("Oscuro", new Paleta(
            new Color(0x0F1117),
            new Color(0x1A1D27),
            new Color(0x2A2E3B),
            new Color(0x13151D),
            new Color(0xE7E9F0),
            new Color(0x8E94A6),
            new Color(0x6C84FF),
            new Color(0x22C55E),
            new Color(0x10261A),
            new Color(0x1E5134),
            new Color(0xF87171),
            new Color(0x35181B),
            new Color(0x232633),
            new Color(0x5C6275)));

    private final String nombre;
    private final Paleta paleta;

    TemaVisual(String nombre, Paleta paleta) {
        this.nombre = nombre;
        this.paleta = paleta;
    }

    public Paleta paleta() {
        return paleta;
    }

    /** Busca un tema por su nombre interno; si no existe devuelve CLARO. */
    public static TemaVisual desde(String nombreInterno) {
        if (nombreInterno != null) {
            for (TemaVisual t : values()) {
                if (t.name().equals(nombreInterno)) return t;
            }
        }
        return CLARO;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
