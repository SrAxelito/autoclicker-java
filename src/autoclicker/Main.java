package autoclicker;

import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.ClickerService;
import autoclicker.ui.ModoGrabacion;
import autoclicker.ui.VentanaPrincipal;
import autoclicker.ui.tema.Tema;
import autoclicker.ui.tema.TemaVisual;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTException;
import java.util.List;

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

            // Primero lo que puede fallar (Robot lanza AWTException): si falla,
            // todavía no se ha registrado la escucha global, que mantiene viva la JVM.
            ClickerService clicker;
            List<ModoGrabacion> grabaciones;
            try {
                clicker = new ClickerService();
                grabaciones = ModoGrabacion.predeterminados();
            } catch (AWTException e) {
                mostrarError("No se pudo controlar el mouse en este sistema", e);
                return;
            }

            AtajoService atajos = new AtajoService();
            atajos.iniciar();
            try {
                new VentanaPrincipal(atajos, preferencias, clicker, grabaciones).setVisible(true);
            } catch (RuntimeException e) {
                // Sin ventana no hay forma de cerrar el programa: se libera la escucha
                // global para que el proceso no quede vivo en segundo plano.
                atajos.cerrar();
                mostrarError("No se pudo abrir la ventana", e);
            }
        });
    }

    private static void mostrarError(String titulo, Exception causa) {
        String detalle = (causa.getMessage() != null) ? causa.getMessage() : causa.toString();
        JOptionPane.showMessageDialog(null, titulo + ":\n" + detalle, "Error", JOptionPane.ERROR_MESSAGE);
    }
}