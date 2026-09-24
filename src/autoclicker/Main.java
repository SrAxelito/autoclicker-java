package autoclicker;

import autoclicker.servicio.ClickerService;
import autoclicker.ui.VentanaAutoClicker;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTException;

/**
 * Punto de entrada: crea el servicio, se lo entrega a la ventana y la muestra.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }

            try {
                ClickerService servicio = new ClickerService();
                new VentanaAutoClicker(servicio).setVisible(true);
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo controlar el mouse en este sistema:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
