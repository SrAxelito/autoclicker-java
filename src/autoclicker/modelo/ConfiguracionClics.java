package autoclicker.modelo;

import java.util.Objects;

/**
 * Parámetros de una sesión de clics. Es inmutable: si algo cambia,
 * se crea una configuración nueva.
 *
 * @param intervaloMs     milisegundos entre cada clic (mínimo 10, o 20 con doble clic)
 * @param maxClics        cantidad de clics a realizar; 0 significa infinito
 * @param esperaSegundos  cuenta regresiva antes de empezar
 * @param boton           botón del mouse a presionar
 * @param dobleClic       si cada clic debe ser doble
 * @param monitor         monitor objetivo (-1 = todos, 0 = Monitor 1, etc.)
 */
public record ConfiguracionClics(
        long intervaloMs,
        int maxClics,
        int esperaSegundos,
        BotonMouse boton,
        boolean dobleClic,
        int monitor) {

    public static final long INTERVALO_MINIMO_MS = 10;
    public static final long INTERVALO_MINIMO_DOBLE_MS = 20;
    public static final int TODOS_LOS_MONITORES = -1;

    public ConfiguracionClics(long intervaloMs, int maxClics, int esperaSegundos, BotonMouse boton, boolean dobleClic) {
        this(intervaloMs, maxClics, esperaSegundos, boton, dobleClic, TODOS_LOS_MONITORES);
    }

    public ConfiguracionClics {
        if (intervaloMs < INTERVALO_MINIMO_MS) {
            throw new IllegalArgumentException("El intervalo debe ser de al menos " + INTERVALO_MINIMO_MS + " ms.");
        }
        if (dobleClic && intervaloMs < INTERVALO_MINIMO_DOBLE_MS) {
            throw new IllegalArgumentException("Con doble clic el intervalo debe ser de al menos " + INTERVALO_MINIMO_DOBLE_MS + " ms.");
        }
        if (maxClics < 0) {
            throw new IllegalArgumentException("El número de clics no puede ser negativo.");
        }
        if (esperaSegundos < 0) {
            throw new IllegalArgumentException("La espera no puede ser negativa.");
        }
        Objects.requireNonNull(boton, "Debes elegir un botón del mouse.");
        if (monitor < TODOS_LOS_MONITORES) {
            throw new IllegalArgumentException("El monitor no puede ser menor que " + TODOS_LOS_MONITORES + ".");
        }
    }

    public boolean esInfinito() {
        return maxClics == 0;
    }
}
