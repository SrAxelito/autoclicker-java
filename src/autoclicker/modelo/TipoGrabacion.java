package autoclicker.modelo;

/**
 * Qué acciones captura una grabación.
 */
public enum TipoGrabacion {

    MOUSE(true, false),
    TECLADO(false, true),
    COMPLETA(true, true);

    private final boolean incluyeMouse;
    private final boolean incluyeTeclado;

    TipoGrabacion(boolean incluyeMouse, boolean incluyeTeclado) {
        this.incluyeMouse = incluyeMouse;
        this.incluyeTeclado = incluyeTeclado;
    }

    public boolean incluyeMouse() {
        return incluyeMouse;
    }

    public boolean incluyeTeclado() {
        return incluyeTeclado;
    }
}
