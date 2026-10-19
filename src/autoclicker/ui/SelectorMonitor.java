package autoclicker.ui;

import autoclicker.modelo.ConfiguracionClics;
import autoclicker.ui.componentes.ControlSegmentado;
import autoclicker.ui.componentes.Etiqueta;
import autoclicker.ui.tema.Tema;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * Selector interactivo de monitor activo. Muestra un ControlSegmentado
 * con las opciones ("Todos", "Monitor 1", "Monitor 2", ...) y debajo
 * una nota con las resoluciones reales de cada monitor y cuál es el principal.
 *
 * Si el sistema tiene un solo monitor conectado (o está en entorno headless),
 * {@link #hayMultiplesMonitores()} devuelve false y la tarjeta no se muestra.
 */
public class SelectorMonitor extends JPanel {

    private final boolean hayMultiplesMonitores;
    private final int cantidadMonitores;
    private final ControlSegmentado<Integer> controlSegmentado;

    public SelectorMonitor() {
        super(new BorderLayout(0, 6));
        setOpaque(false);

        GraphicsDevice[] dispositivos;
        GraphicsDevice principal = null;

        if (GraphicsEnvironment.isHeadless()) {
            dispositivos = new GraphicsDevice[0];
        } else {
            try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                dispositivos = ge.getScreenDevices();
                principal = ge.getDefaultScreenDevice();
            } catch (Exception e) {
                dispositivos = new GraphicsDevice[0];
            }
        }

        this.cantidadMonitores = (dispositivos != null) ? dispositivos.length : 0;
        this.hayMultiplesMonitores = cantidadMonitores > 1;

        if (hayMultiplesMonitores) {
            Integer[] valores = new Integer[cantidadMonitores + 1];
            String[] etiquetas = new String[cantidadMonitores + 1];
            valores[0] = ConfiguracionClics.TODOS_LOS_MONITORES;
            etiquetas[0] = "Todos";

            StringBuilder nota = new StringBuilder();
            for (int i = 0; i < cantidadMonitores; i++) {
                valores[i + 1] = i;
                etiquetas[i + 1] = "Monitor " + (i + 1);

                if (i > 0) nota.append(" · ");
                nota.append("Monitor ").append(i + 1).append(": ");
                DisplayMode dm = dispositivos[i].getDisplayMode();
                if (dm != null) {
                    nota.append(dm.getWidth()).append("x").append(dm.getHeight());
                }
                if (dispositivos[i].equals(principal)) {
                    nota.append(" (principal)");
                }
            }

            this.controlSegmentado = new ControlSegmentado<>(valores, etiquetas);
            add(controlSegmentado, BorderLayout.NORTH);

            Etiqueta detalle = new Etiqueta(nota.toString(), Etiqueta.Rol.SUAVE, Tema.fuente(Font.PLAIN, 12f));
            add(detalle, BorderLayout.CENTER);
        } else {
            this.controlSegmentado = null;
        }
    }

    /**
     * @return true si hay al menos dos monitores conectados para poder seleccionar entre ellos
     */
    public boolean hayMultiplesMonitores() {
        return hayMultiplesMonitores;
    }

    public int cantidadMonitores() {
        return cantidadMonitores;
    }

    public int getMonitor() {
        if (!hayMultiplesMonitores || controlSegmentado == null) {
            return ConfiguracionClics.TODOS_LOS_MONITORES;
        }
        Integer sel = controlSegmentado.getSeleccion();
        return (sel != null) ? sel : ConfiguracionClics.TODOS_LOS_MONITORES;
    }

    public void setMonitor(int monitor) {
        if (!hayMultiplesMonitores || controlSegmentado == null) return;
        if (monitor < 0 || monitor >= cantidadMonitores) {
            controlSegmentado.setSeleccion(ConfiguracionClics.TODOS_LOS_MONITORES);
        } else {
            controlSegmentado.setSeleccion(monitor);
        }
    }

    @Override
    public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        if (controlSegmentado != null) {
            controlSegmentado.setEnabled(habilitado);
        }
    }
}
