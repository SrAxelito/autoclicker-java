package autoclicker;

import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ClickerService;
import autoclicker.ui.VentanaAutoClicker;
import autoclicker.ui.tema.Tema;
import autoclicker.ui.tema.TemaVisual;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTException;

/**
 * Punto de entrada: arma las piezas (preferencias, servicios y ventana)
 * y muestra la ventana.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }

            PreferenciasRepository preferencias = new PreferenciasRepository();
            Tema.aplicar(TemaVisual.desde(preferencias.cargarTema().orElse(null)));

            try {
                ClickerService servicio = new ClickerService();
                AtajoService atajos = new AtajoService();
                atajos.iniciar();
                new VentanaAutoClicker(servicio, atajos, preferencias).setVisible(true);
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo controlar el mouse en este sistema:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
