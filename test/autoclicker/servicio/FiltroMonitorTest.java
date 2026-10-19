package autoclicker.servicio;

import autoclicker.modelo.ConfiguracionClics;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroMonitorTest {

    private static final long MS_A_NS = 1_000_000L;

    @Test
    void conTodosSiemprePermiteActuar() {
        AtomicBoolean dentro = new AtomicBoolean(false);
        FiltroMonitor filtro = new FiltroMonitor(ConfiguracionClics.TODOS_LOS_MONITORES, dentro::get);

        long ahora = 1_000_000_000L;
        // Aunque el cursor esté fuera, con todos permite actuar
        FiltroMonitor.Decision d1 = filtro.evaluar(ahora);
        assertTrue(d1.puedeActuar());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, d1.cambio());
        assertFalse(filtro.estaPausado());

        dentro.set(true);
        FiltroMonitor.Decision d2 = filtro.evaluar(ahora + 100 * MS_A_NS);
        assertTrue(d2.puedeActuar());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, d2.cambio());
        assertFalse(filtro.estaPausado());
    }

    @Test
    void conCursorFueraNoPermite() {
        FiltroMonitor filtro = new FiltroMonitor(0, () -> false);
        long ahora = 1_000_000_000L;

        FiltroMonitor.Decision d = filtro.evaluar(ahora);
        assertFalse(d.puedeActuar());
        assertEquals(FiltroMonitor.CambioEstado.PAUSADO, d.cambio());
        assertTrue(filtro.estaPausado());
        assertEquals("En pausa: el cursor está fuera del Monitor 1", filtro.mensajePausa());
    }

    @Test
    void alEmpezarConCursorDentroPermiteDeInmediato() {
        FiltroMonitor filtro = new FiltroMonitor(1, () -> true);
        long ahora = 1_000_000_000L;

        FiltroMonitor.Decision d = filtro.evaluar(ahora);
        assertTrue(d.puedeActuar());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, d.cambio());
        assertFalse(filtro.estaPausado());
    }

    @Test
    void alVolverNoPermiteAntesDe500MsYSiDespues() {
        AtomicBoolean dentro = new AtomicBoolean(true);
        FiltroMonitor filtro = new FiltroMonitor(0, dentro::get);
        long t = 1_000_000_000L;

        // Comienza dentro: actúa
        assertTrue(filtro.evaluar(t).puedeActuar());

        // Sale: pausa
        dentro.set(false);
        t += 100 * MS_A_NS;
        FiltroMonitor.Decision dPausa = filtro.evaluar(t);
        assertFalse(dPausa.puedeActuar());
        assertEquals(FiltroMonitor.CambioEstado.PAUSADO, dPausa.cambio());
        assertTrue(filtro.estaPausado());

        // Vuelve a entrar en tRetorno
        dentro.set(true);
        long tRetorno = t + 200 * MS_A_NS;
        FiltroMonitor.Decision dEntrada = filtro.evaluar(tRetorno);
        assertFalse(dEntrada.puedeActuar(), "No debe permitir en el mismo instante de retorno");
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, dEntrada.cambio());
        assertTrue(filtro.estaPausado());

        // A los 499 ms de haber vuelto: aún no permite
        FiltroMonitor.Decision dAntes = filtro.evaluar(tRetorno + 499 * MS_A_NS);
        assertFalse(dAntes.puedeActuar(), "No debe permitir a los 499 ms");
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, dAntes.cambio());
        assertTrue(filtro.estaPausado());

        // A los 500 ms exactos: reanuda y permite actuar
        FiltroMonitor.Decision dDespues = filtro.evaluar(tRetorno + 500 * MS_A_NS);
        assertTrue(dDespues.puedeActuar(), "Debe permitir al cumplirse los 500 ms");
        assertEquals(FiltroMonitor.CambioEstado.REANUDADO, dDespues.cambio());
        assertFalse(filtro.estaPausado());
    }

    @Test
    void siSaleOtraVezAntesDeLos500MsElPlazoEmpiezaDeNuevo() {
        AtomicBoolean dentro = new AtomicBoolean(false);
        FiltroMonitor filtro = new FiltroMonitor(0, dentro::get);
        long t = 1_000_000_000L;

        // Empieza fuera: pausado
        filtro.evaluar(t);
        assertTrue(filtro.estaPausado());

        // Entra en t = 1000 ms
        dentro.set(true);
        long tPrimerRetorno = t + 500 * MS_A_NS;
        filtro.evaluar(tPrimerRetorno);

        // A los 300 ms del primer retorno, vuelve a salir
        dentro.set(false);
        long tSalida = tPrimerRetorno + 300 * MS_A_NS;
        filtro.evaluar(tSalida);
        assertTrue(filtro.estaPausado());

        // Vuelve a entrar en tSegundoRetorno
        dentro.set(true);
        long tSegundoRetorno = tSalida + 200 * MS_A_NS;
        filtro.evaluar(tSegundoRetorno);

        // A los 300 ms del segundo retorno (800 ms después del primero): no debe reanudar porque salió
        FiltroMonitor.Decision d = filtro.evaluar(tSegundoRetorno + 300 * MS_A_NS);
        assertFalse(d.puedeActuar());
        assertTrue(filtro.estaPausado());

        // A los 500 ms del segundo retorno: sí debe reanudar
        FiltroMonitor.Decision dFinal = filtro.evaluar(tSegundoRetorno + 500 * MS_A_NS);
        assertTrue(dFinal.puedeActuar());
        assertEquals(FiltroMonitor.CambioEstado.REANUDADO, dFinal.cambio());
        assertFalse(filtro.estaPausado());
    }

    @Test
    void informaElCambioDeEstadoSoloUnaVezPorCadaPausaYCadaReanudacion() {
        AtomicBoolean dentro = new AtomicBoolean(true);
        FiltroMonitor filtro = new FiltroMonitor(0, dentro::get);
        long t = 1_000_000_000L;

        // Inicial: dentro, sin cambio
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(t).cambio());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(t + 10 * MS_A_NS).cambio());

        // Pasa a fuera: avisa PAUSADO la primera vez
        dentro.set(false);
        assertEquals(FiltroMonitor.CambioEstado.PAUSADO, filtro.evaluar(t + 20 * MS_A_NS).cambio());
        // Sigue fuera: no vuelve a avisar
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(t + 30 * MS_A_NS).cambio());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(t + 40 * MS_A_NS).cambio());

        // Vuelve a entrar
        dentro.set(true);
        long tRetorno = t + 100 * MS_A_NS;
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(tRetorno).cambio());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(tRetorno + 200 * MS_A_NS).cambio());

        // Se cumplen los 500 ms: avisa REANUDADO la primera vez
        assertEquals(FiltroMonitor.CambioEstado.REANUDADO, filtro.evaluar(tRetorno + 500 * MS_A_NS).cambio());
        // Sigue dentro: no vuelve a avisar
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(tRetorno + 600 * MS_A_NS).cambio());
        assertEquals(FiltroMonitor.CambioEstado.NINGUNO, filtro.evaluar(tRetorno + 700 * MS_A_NS).cambio());
    }
}
