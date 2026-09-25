package autoclicker.modelo;

/**
 * Una acción grabada, con el momento en que ocurrió (milisegundos desde
 * que empezó la grabación). Los botones del mouse usan la numeración de
 * Java: 1 = izquierdo, 2 = central, 3 = derecho, 4 y 5 = laterales.
 * Las teclas usan los códigos de java.awt.event.KeyEvent.
 */
public sealed interface EventoMacro {

    long tiempoMs();

    record MovimientoMouse(long tiempoMs, int x, int y) implements EventoMacro { }

    record PresionBoton(long tiempoMs, int boton, int x, int y) implements EventoMacro { }

    record SueltaBoton(long tiempoMs, int boton, int x, int y) implements EventoMacro { }

    record RuedaMouse(long tiempoMs, int giro) implements EventoMacro { }

    record PresionTecla(long tiempoMs, int codigoTecla) implements EventoMacro { }

    record SueltaTecla(long tiempoMs, int codigoTecla) implements EventoMacro { }
}
