package autoclicker.servicio;

/**
 * Consulta si el cursor del mouse se encuentra en el monitor objetivo.
 * Se define como interfaz para permitir pruebas unitarias sin dependencias
 * del entorno gráfico nativo (entornos headless).
 */
@FunctionalInterface
public interface UbicacionCursor {

    /**
     * @return true si el cursor está en el monitor objetivo o si no aplica filtro;
     *         false si está en otro monitor o si no se puede determinar
     */
    boolean estaEnMonitorObjetivo();
}
