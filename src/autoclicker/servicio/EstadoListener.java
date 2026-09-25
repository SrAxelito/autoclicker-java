package autoclicker.servicio;

/**
 * Recibe avisos de un servicio que trabaja en segundo plano
 * (el autoclicker o el reproductor de grabaciones).
 *
 * Importante: estos métodos se llaman desde el hilo del servicio, no desde
 * el hilo de la interfaz. Quien los implemente en Swing debe usar
 * SwingUtilities.invokeLater para tocar componentes.
 */
public interface EstadoListener {

    /** Progreso durante la ejecución. */
    void alCambiarEstado(String mensaje);

    /** Se llama una sola vez cuando el trabajo termina por cualquier motivo. */
    void alTerminar(String mensajeFinal);

    /** Listener que no hace nada, para no tener que comprobar null. */
    EstadoListener NINGUNO = new EstadoListener() {
        @Override public void alCambiarEstado(String mensaje) { }
        @Override public void alTerminar(String mensajeFinal) { }
    };
}
