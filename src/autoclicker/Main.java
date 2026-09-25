package autoclicker;

import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ClickerService;
import autoclicker.servicio.GrabadoraService;
import autoclicker.servicio.ReproductorService;
import autoclicker.ui.VentanaPrincipal;
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
                AtajoService atajos = new AtajoService();
                atajos.iniciar();

                new VentanaPrincipal(atajos, preferencias,
                        new ClickerService(),
                        new GrabadoraService(false), new ReproductorService(),
                        new GrabadoraService(true), new ReproductorService()
                ).setVisible(true);
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo controlar el mouse en este sistema:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
