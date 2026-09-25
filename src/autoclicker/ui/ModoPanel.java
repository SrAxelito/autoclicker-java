package autoclicker.ui;

import javax.swing.JComponent;
import java.util.List;

/**
 * Un modo de la aplicación que se muestra en su propia pestaña.
 */
interface ModoPanel {

    /** Texto de la pestaña. */
    String titulo();

    /** Contenido de la pestaña. */
    JComponent vista();

    /** Claves de los atajos que deben responder mientras este modo está visible. */
    List<String> clavesAtajo();

    /** true si está grabando, reproduciendo o clicando (no se puede cambiar de pestaña). */
    boolean estaOcupado();

    /** Avisa a la ventana cada vez que cambia estaOcupado(). */
    void setAlCambiarOcupado(Runnable accion);

    /** Detiene todo lo que esté en marcha (al cerrar la ventana). */
    void detenerTodo();
}
