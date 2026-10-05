package autoclicker.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagLayout;

/**
 * Base de las pestañas: reúne lo que todas hacen igual, que es dibujarse como
 * un panel transparente y avisar a la ventana cuando empiezan o terminan de
 * trabajar.
 */
abstract class PanelModo extends JPanel implements ModoPanel {

    private Runnable alCambiarOcupado = () -> { };

    protected PanelModo() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    @Override
    public final JComponent vista() {
        return this;
    }

    @Override
    public final void setAlCambiarOcupado(Runnable accion) {
        this.alCambiarOcupado = (accion != null) ? accion : () -> { };
    }

    /** Las pestañas lo llaman cada vez que puede haber cambiado {@link #estaOcupado()}. */
    protected final void avisarCambioDeOcupado() {
        alCambiarOcupado.run();
    }
}