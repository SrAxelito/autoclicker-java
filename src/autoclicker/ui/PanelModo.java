package autoclicker.ui;

import autoclicker.ui.componentes.PanelDesplazable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;

/**
 * Base de las pestañas: reúne lo que todas hacen igual, que es dibujarse como
 * un panel transparente, repartir su espacio en dos zonas y avisar a la
 * ventana cuando empiezan o terminan de trabajar.
 *
 * Las dos zonas existen para que la ventana se pueda achicar: las opciones
 * se desplazan cuando no caben, y el pie (estado y botones) queda siempre a
 * la vista.
 */
abstract class PanelModo extends JPanel implements ModoPanel {

    private final JPanel zonaOpciones = zona();
    private final JPanel zonaPie = zona();
    private Runnable alCambiarOcupado = () -> { };

    protected PanelModo() {
        super(new BorderLayout());
        setOpaque(false);
        add(new PanelDesplazable(zonaOpciones), BorderLayout.CENTER);
        add(zonaPie, BorderLayout.SOUTH);
    }

    private static JPanel zona() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    /** Pila de arriba: las tarjetas de opciones. Se desplaza si la ventana es baja. */
    protected final Diseno.Pila pilaDeOpciones() {
        return new Diseno.Pila(zonaOpciones);
    }

    /** Pila de abajo: el estado y los botones. Siempre está a la vista. */
    protected final Diseno.Pila pilaDelPie() {
        return new Diseno.Pila(zonaPie);
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