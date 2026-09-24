package autoclicker.servicio;

/**
 * Recibe avisos del ClickerService mientras trabaja.
 *
 * Importante: estos métodos se llaman desde el hilo de clics, no desde
 * el hilo de la interfaz. Quien los implemente en Swing debe usar
 * SwingUtilities.invokeLater para tocar componentes.
 */
public interface ClickerListener {

    /** Progreso durante la ejecución (cuenta regresiva, clics hechos). */
    void alCambiarEstado(String mensaje);

    /** Se llama una sola vez cuando la sesión termina por cualquier motivo. */
    void alTerminar(String mensajeFinal);

    /** Listener que no hace nada, para no tener que comprobar null. */
    ClickerListener NINGUNO = new ClickerListener() {
        @Override public void alCambiarEstado(String mensaje) { }
        @Override public void alTerminar(String mensajeFinal) { }
    };
}
