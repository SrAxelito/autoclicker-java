package autoclicker.modelo;

import java.util.List;

/**
 * Secuencia de acciones grabadas y cuánto duró la grabación.
 * Es inmutable: grabar de nuevo crea otra Grabacion.
 */
public record Grabacion(List<EventoMacro> eventos, long duracionMs) {

    public static final Grabacion VACIA = new Grabacion(List.of(), 0);

    public Grabacion {
        eventos = List.copyOf(eventos);
        if (duracionMs < 0) throw new IllegalArgumentException("La duración no puede ser negativa.");
    }

    public boolean estaVacia() {
        return eventos.isEmpty();
    }

    public long cantidadClics() {
        return eventos.stream().filter(e -> e instanceof EventoMacro.PresionBoton).count();
    }

    public long cantidadTeclas() {
        return eventos.stream().filter(e -> e instanceof EventoMacro.PresionTecla).count();
    }
}
