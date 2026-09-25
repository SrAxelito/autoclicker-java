package autoclicker.ui.componentes;

import autoclicker.ui.tema.Paleta;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * JLabel que toma su color del tema activo, así cambia solo al cambiar de tema.
 */
public class Etiqueta extends JLabel {

    public enum Rol { TEXTO, SUAVE }

    private final Rol rol;
    private boolean atenuada = false;

    public Etiqueta(String texto, Rol rol, Font fuente) {
        this(texto, rol, fuente, SwingConstants.LEADING);
    }

    public Etiqueta(String texto, Rol rol, Font fuente, int alineacion) {
        super(texto, alineacion);
        this.rol = rol;
        setFont(fuente);
    }

    /** Atenuada se ve como deshabilitada, sin usar el estilo gris del look and feel. */
    public void setAtenuada(boolean atenuada) {
        this.atenuada = atenuada;
        repaint();
    }

    @Override
    public Color getForeground() {
        if (rol == null) return super.getForeground(); // durante la construcción de JLabel
        Paleta p = Tema.paleta();
        if (atenuada) return p.textoDeshabilitado();
        return rol == Rol.TEXTO ? p.texto() : p.textoSuave();
    }
}
