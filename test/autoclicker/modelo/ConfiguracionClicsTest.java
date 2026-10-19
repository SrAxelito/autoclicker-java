package autoclicker.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertDoesNotThrow(() -> new ConfiguracionClics(ConfiguracionClics.INTERVALO_MINIMO_MS, 0, 0, BotonMouse.IZQUIERDO, false));
        assertDoesNotThrow(() -> new ConfiguracionClics(ConfiguracionClics.INTERVALO_MINIMO_DOBLE_MS, 0, 0, BotonMouse.CENTRAL, true));
    }

    @Test
    void rechazaIntervalosMenoresAlMinimo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionClics(9, 0, 0, BotonMouse.IZQUIERDO, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionClics(19, 0, 0, BotonMouse.IZQUIERDO, true));
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

    @Test
    void constructorSinMonitorUsaTodos() {
        ConfiguracionClics config = new ConfiguracionClics(100, 0, 3, BotonMouse.IZQUIERDO, false);
        assertEquals(ConfiguracionClics.TODOS_LOS_MONITORES, config.monitor());
    }

    @Test
    void aceptaMonitoresValidos() {
        assertDoesNotThrow(() -> new ConfiguracionClics(100, 0, 0, BotonMouse.IZQUIERDO, false, -1));
        assertDoesNotThrow(() -> new ConfiguracionClics(100, 0, 0, BotonMouse.IZQUIERDO, false, 0));
        assertDoesNotThrow(() -> new ConfiguracionClics(100, 0, 0, BotonMouse.IZQUIERDO, false, 1));
    }

    @Test
    void rechazaMonitorMenorQueMenosUno() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionClics(100, 0, 0, BotonMouse.IZQUIERDO, false, -2));
    }
}