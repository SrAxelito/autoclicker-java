package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Panel que usa el color de fondo del tema activo.
 */
public class PanelFondo extends JPanel {

    public PanelFondo(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    @Override
    public Color getBackground() {
        return Tema.paleta().fondo();
    }
}
