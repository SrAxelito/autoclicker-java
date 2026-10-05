package autoclicker.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionClicsTest {

    @Test
    void ceroClicsSignificaInfinito() {
        assertTrue(new ConfiguracionClics(100, 0, 3, BotonMouse.IZQUIERDO, false).esInfinito());
        assertFalse(new ConfiguracionClics(100, 25, 3, BotonMouse.IZQUIERDO, false).esInfinito());
    }

    @Test
    void aceptaLosValoresMinimos() {
        assertDoesNotThrow(() -> new ConfiguracionClics(1, 0, 0, BotonMouse.CENTRAL, true));
    }

    @Test
    void rechazaIntervalosMenoresAUnMilisegundo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionClics(0, 0, 0, BotonMouse.IZQUIERDO, false));
    }

    @Test
    void rechazaCantidadesNegativas() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionClics(100, -1, 0, BotonMouse.IZQUIERDO, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionClics(100, 0, -1, BotonMouse.IZQUIERDO, false));
    }

    @Test
    void exigeUnBoton() {
        assertThrows(NullPointerException.class,
                () -> new ConfiguracionClics(100, 0, 0, null, false));
    }
}