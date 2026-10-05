package autoclicker.servicio;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba el cálculo de las pausas del clic, que no necesita mover el mouse de verdad. */
class ClickerServiceTest {

    private static ConfiguracionClics simple(long intervaloMs) {
        return new ConfiguracionClics(intervaloMs, 0, 0, BotonMouse.IZQUIERDO, false);
    }

    private static ConfiguracionClics doble(long intervaloMs) {
        return new ConfiguracionClics(intervaloMs, 0, 0, BotonMouse.IZQUIERDO, true);
    }

    @Test
    void conIntervalosLargosUsaLasPausasCompletas() {
        assertEquals(10, ClickerService.pausaPresionMs(simple(100)));
        assertEquals(10, ClickerService.pausaPresionMs(simple(600_000)));
        assertEquals(10, ClickerService.pausaPresionMs(doble(1000)));
        assertEquals(40, ClickerService.pausaDobleClicMs(doble(1000)));
    }

    @Test
    void conIntervalosCortosAcortaLaPausaDePresion() {
        assertEquals(0, ClickerService.pausaPresionMs(simple(1)));
        assertEquals(2, ClickerService.pausaPresionMs(simple(5)));
        assertEquals(5, ClickerService.pausaPresionMs(simple(10)));
        assertEquals(10, ClickerService.pausaPresionMs(simple(20)));
    }

    @Test
    void conIntervalosCortosAcortaElDobleClic() {
        assertEquals(0, ClickerService.pausaPresionMs(doble(1)));
        assertEquals(0, ClickerService.pausaDobleClicMs(doble(1)));
        assertEquals(6, ClickerService.pausaPresionMs(doble(50)));
        assertEquals(12, ClickerService.pausaDobleClicMs(doble(50)));
    }

    @Test
    void unClicSimpleNuncaOcupaMasDeLaMitadDelIntervalo() {
        for (long intervalo = 1; intervalo <= 2000; intervalo++) {
            long ocupado = ClickerService.pausaPresionMs(simple(intervalo));
            assertTrue(ocupado * 2 <= intervalo, "intervalo de " + intervalo + " ms: el clic ocupa " + ocupado + " ms");
        }
    }

    @Test
    void unDobleClicNuncaOcupaMasDeLaMitadDelIntervalo() {
        for (long intervalo = 1; intervalo <= 2000; intervalo++) {
            ConfiguracionClics config = doble(intervalo);
            long ocupado = 2 * ClickerService.pausaPresionMs(config) + ClickerService.pausaDobleClicMs(config);
            assertTrue(ocupado * 2 <= intervalo, "intervalo de " + intervalo + " ms: el doble clic ocupa " + ocupado + " ms");
        }
    }
}