package autoclicker.ui.tema;

import java.awt.Color;

/**
 * Conjunto de colores de un tema.
 */
public record Paleta(
        Color fondo,
        Color tarjeta,
        Color borde,
        Color campo,
        Color texto,
        Color textoSuave,
        Color acento,
        Color exito,
        Color exitoFondo,
        Color exitoBorde,
        Color peligro,
        Color peligroFondo,
        Color deshabilitado,
        Color textoDeshabilitado) {
}
