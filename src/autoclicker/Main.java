package autoclicker;

import autoclicker.modelo.TipoGrabacion;
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
                // Primero lo que puede fallar (Robot lanza AWTException): si falla,
                // todavía no se ha registrado la escucha global, que mantiene viva la JVM.
                ClickerService clicker = new ClickerService();
                ReproductorService reproductorMouse = new ReproductorService();
                ReproductorService reproductorTeclado = new ReproductorService();
                ReproductorService reproductorCompleto = new ReproductorService();

                AtajoService atajos = new AtajoService();
                atajos.iniciar();

                new VentanaPrincipal(atajos, preferencias,
                        clicker,
                        new GrabadoraService(TipoGrabacion.MOUSE), reproductorMouse,
                        new GrabadoraService(TipoGrabacion.TECLADO), reproductorTeclado,
                        new GrabadoraService(TipoGrabacion.COMPLETA), reproductorCompleto
                ).setVisible(true);
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo controlar el mouse en este sistema:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}