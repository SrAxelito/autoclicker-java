package autoclicker.modelo;

import java.util.Objects;

/**
 * Tecla (con modificadores opcionales) o botón lateral del mouse que
 * inicia y detiene el autoclicker desde cualquier programa.
 *
 * @param tipo          si es una tecla o un botón del mouse
 * @param codigo        código de la tecla o número del botón
 * @param modificadores combinación de CTRL, SHIFT, ALT y META (solo teclas)
 * @param nombre        texto para mostrar, por ejemplo "Ctrl + F6"
 */
public record Atajo(Tipo tipo, int codigo, int modificadores, String nombre) {

    public enum Tipo { TECLA, MOUSE }

    public static final int CTRL  = 1;
    public static final int SHIFT = 2;
    public static final int ALT   = 4;
    public static final int META  = 8;

    public Atajo {
        Objects.requireNonNull(tipo, "El atajo necesita un tipo.");
        Objects.requireNonNull(nombre, "El atajo necesita un nombre.");
        if (codigo <= 0) {
            throw new IllegalArgumentException("Código de atajo inválido: " + codigo);
        }
        if (tipo == Tipo.MOUSE) modificadores = 0;
    }

    public boolean coincideConTecla(int codigoTecla, int mods) {
        return tipo == Tipo.TECLA && codigo == codigoTecla && modificadores == mods;
    }

    public boolean coincideConBoton(int boton) {
        return tipo == Tipo.MOUSE && codigo == boton;
    }
}
