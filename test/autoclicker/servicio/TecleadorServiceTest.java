package autoclicker.servicio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba el cálculo de la duración de presión de teclas sin necesitar Robot. */
class TecleadorServiceTest {

    @Test
    void duracionDePresionEnIntervalosEspecificos() {
        assertEquals(5, TecleadorService.pausaPresionMs(10));
        assertEquals(10, TecleadorService.pausaPresionMs(20));
        assertEquals(25, TecleadorService.pausaPresionMs(50));
        assertEquals(30, TecleadorService.pausaPresionMs(60));
        assertEquals(30, TecleadorService.pausaPresionMs(100));
        assertEquals(30, TecleadorService.pausaPresionMs(1000));
    }

    @Test
    void paraIntervalosDeDiezADosMilLaPresionCumpleLosLimites() {
        for (long intervalo = 10; intervalo <= 2000; intervalo++) {
            long presion = TecleadorService.pausaPresionMs(intervalo);
            assertTrue(presion >= 5, "intervalo de " + intervalo + " ms: la presión " + presion + " es menor a 5 ms");
            assertTrue(presion <= 30, "intervalo de " + intervalo + " ms: la presión " + presion + " supera 30 ms");
            assertTrue(presion * 2 <= intervalo, "intervalo de " + intervalo + " ms: la presión " + presion + " supera la mitad");
        }
    }
}
