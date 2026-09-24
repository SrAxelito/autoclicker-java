package autoclicker.modelo;

/**
 * Botones del mouse que el autoclicker puede presionar.
 */
public enum BotonMouse {
    IZQUIERDO("Izquierdo"),
    DERECHO("Derecho"),
    CENTRAL("Central");

    private final String etiqueta;

    BotonMouse(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto que se muestra en la interfaz (lo usa el JComboBox). */
    @Override
    public String toString() {
        return etiqueta;
    }
}
