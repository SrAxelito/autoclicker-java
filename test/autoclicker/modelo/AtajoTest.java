package autoclicker.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtajoTest {

    private static final int F6 = 64;

    @Test
    void unaTeclaCoincideSoloConSuCodigoYSusModificadores() {
        Atajo atajo = new Atajo(Atajo.Tipo.TECLA, F6, Atajo.CTRL, "Ctrl + F6");

        assertTrue(atajo.coincideConTecla(F6, Atajo.CTRL));
        assertFalse(atajo.coincideConTecla(F6, 0), "sin Ctrl no debe coincidir");
        assertFalse(atajo.coincideConTecla(F6, Atajo.CTRL | Atajo.SHIFT), "con un modificador de más no debe coincidir");
        assertFalse(atajo.coincideConTecla(F6 + 1, Atajo.CTRL), "otra tecla no debe coincidir");
    }

    @Test
    void unaTeclaNuncaCoincideConUnBotonDelMouse() {
        Atajo tecla = new Atajo(Atajo.Tipo.TECLA, 4, 0, "3");

        assertFalse(tecla.coincideConBoton(4));
    }

    @Test
    void unBotonCoincideSoloConSuNumero() {
        Atajo boton = new Atajo(Atajo.Tipo.MOUSE, 4, 0, "Botón lateral 1");

        assertTrue(boton.coincideConBoton(4));
        assertFalse(boton.coincideConBoton(5));
        assertFalse(boton.coincideConTecla(4, 0), "un botón no es una tecla");
    }

    @Test
    void unBotonDelMouseDescartaLosModificadores() {
        Atajo boton = new Atajo(Atajo.Tipo.MOUSE, 5, Atajo.CTRL | Atajo.ALT, "Botón lateral 2");

        assertEquals(0, boton.modificadores());
    }

    @Test
    void rechazaCodigosQueNoSonPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new Atajo(Atajo.Tipo.TECLA, 0, 0, "nada"));
        assertThrows(IllegalArgumentException.class, () -> new Atajo(Atajo.Tipo.TECLA, -1, 0, "nada"));
    }

    @Test
    void exigeTipoYNombre() {
        assertThrows(NullPointerException.class, () -> new Atajo(null, F6, 0, "F6"));
        assertThrows(NullPointerException.class, () -> new Atajo(Atajo.Tipo.TECLA, F6, 0, null));
    }
}