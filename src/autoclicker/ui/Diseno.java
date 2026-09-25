package autoclicker.ui;

import autoclicker.ui.componentes.Etiqueta;
import autoclicker.ui.componentes.PanelTarjeta;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Piezas de maquetación que comparten las ventanas y los paneles.
 */
final class Diseno {

    private Diseno() { }

    static PanelTarjeta tarjeta(String titulo, JComponent control, String nota) {
        PanelTarjeta tarjeta = new PanelTarjeta(titulo);
        tarjeta.getContenido().add(control, BorderLayout.NORTH);
        if (nota != null) {
            tarjeta.getContenido().add(
                    new Etiqueta(nota, Etiqueta.Rol.SUAVE, Tema.fuente(Font.PLAIN, 12f)),
                    BorderLayout.CENTER);
        }
        return tarjeta;
    }

    static JComponent dosColumnas(JComponent izquierda, JComponent derecha) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setOpaque(false);
        panel.add(izquierda);
        panel.add(derecha);
        return panel;
    }

    /** Apila componentes verticalmente ocupando todo el ancho. */
    static final class Pila {
        private final JPanel panel;
        private int fila = 0;

        Pila(JPanel panel) {
            this.panel = panel;
        }

        void agregar(JComponent componente, int espacioArriba) {
            GridBagConstraints c = base();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(espacioArriba, 0, 0, 0);
            panel.add(componente, c);
        }

        void agregar(JComponent componente, int espacioArriba, double pesoVertical) {
            GridBagConstraints c = base();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = pesoVertical;
            c.insets = new Insets(espacioArriba, 0, 0, 0);
            panel.add(componente, c);
        }

        /** Espacio flexible: empuja lo que venga después hacia abajo. */
        void agregarRelleno() {
            GridBagConstraints c = base();
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            JPanel relleno = new JPanel();
            relleno.setOpaque(false);
            panel.add(relleno, c);
        }

        private GridBagConstraints base() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = fila++;
            c.weightx = 1;
            return c;
        }
    }
}
