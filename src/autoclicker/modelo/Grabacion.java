package autoclicker.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Pulsaciones de teclas. Al mantener una tecla, el teclado repite el aviso
     * de "presionada" muchas veces antes de soltarla; todas esas repeticiones
     * cuentan como una sola pulsación.
     */
    public long cantidadTeclas() {
        Set<Integer> sostenidas = new HashSet<>();
        long pulsaciones = 0;
        for (EventoMacro evento : eventos) {
            if (evento instanceof EventoMacro.PresionTecla presion) {
                if (sostenidas.add(presion.codigoTecla())) pulsaciones++;
            } else if (evento instanceof EventoMacro.SueltaTecla suelta) {
                sostenidas.remove(suelta.codigoTecla());
            }
        }
        return pulsaciones;
    }
}