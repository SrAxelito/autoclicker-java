package autoclicker.modelo;

import java.util.Objects;

/**
 * Parámetros de una sesión de pulsaciones repetidas de teclado. Es inmutable:
 * si algo cambia, se crea una configuración nueva.
 *
 * @param tecla           tecla o combinación a presionar
 * @param intervaloMs     milisegundos entre el inicio de una pulsación y el siguiente (mínimo 10)
 * @param repeticiones    cantidad de pulsaciones a realizar; 0 significa infinito
 * @param esperaSegundos  cuenta regresiva antes de empezar
 */
public record ConfiguracionTecleo(
        Atajo tecla,
        long intervaloMs,
        int repeticiones,
        int esperaSegundos) {

    public static final long INTERVALO_MINIMO_MS = 10;

    public ConfiguracionTecleo {
        Objects.requireNonNull(tecla, "Debes elegir una tecla.");
        if (tecla.tipo() != Atajo.Tipo.TECLA) {
            throw new IllegalArgumentException("La tecla debe ser de tipo TECLA.");
        }
        if (intervaloMs < INTERVALO_MINIMO_MS) {
            throw new IllegalArgumentException("El intervalo debe ser de al menos " + INTERVALO_MINIMO_MS + " ms.");
        }
        if (repeticiones < 0) {
            throw new IllegalArgumentException("Las repeticiones no pueden ser negativas.");
        }
        if (esperaSegundos < 0) {
            throw new IllegalArgumentException("La espera no puede ser negativa.");
        }
    }

    public boolean esInfinito() {
        return repeticiones == 0;
    }
}
