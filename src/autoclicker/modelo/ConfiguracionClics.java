package autoclicker.modelo;

import java.util.Objects;

/**
 * Parámetros de una sesión de clics. Es inmutable: si algo cambia,
 * se crea una configuración nueva.
 *
 * @param intervaloMs     milisegundos entre cada clic (mínimo 1)
 * @param maxClics        cantidad de clics a realizar; 0 significa infinito
 * @param esperaSegundos  cuenta regresiva antes de empezar
 * @param boton           botón del mouse a presionar
 * @param dobleClic       si cada clic debe ser doble
 */
public record ConfiguracionClics(
        long intervaloMs,
        int maxClics,
        int esperaSegundos,
        BotonMouse boton,
        boolean dobleClic) {

    public ConfiguracionClics {
        if (intervaloMs < 1) {
            throw new IllegalArgumentException("El intervalo debe ser de al menos 1 ms.");
        }
        if (maxClics < 0) {
            throw new IllegalArgumentException("El número de clics no puede ser negativo.");
        }
        if (esperaSegundos < 0) {
            throw new IllegalArgumentException("La espera no puede ser negativa.");
        }
        Objects.requireNonNull(boton, "Debes elegir un botón del mouse.");
    }

    public boolean esInfinito() {
        return maxClics == 0;
    }
}
