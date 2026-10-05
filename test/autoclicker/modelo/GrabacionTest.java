package autoclicker.modelo;

import autoclicker.modelo.EventoMacro.MovimientoMouse;
import autoclicker.modelo.EventoMacro.PresionBoton;
import autoclicker.modelo.EventoMacro.PresionTecla;
import autoclicker.modelo.EventoMacro.SueltaBoton;
import autoclicker.modelo.EventoMacro.SueltaTecla;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrabacionTest {

    private static final int A = 65;
    private static final int B = 66;

    @Test
    void laGrabacionVaciaNoTieneEventos() {
        assertTrue(Grabacion.VACIA.estaVacia());
        assertEquals(0, Grabacion.VACIA.cantidadClics());
        assertEquals(0, Grabacion.VACIA.cantidadTeclas());
    }

    @Test
    void unaTeclaMantenidaCuentaComoUnaSolaPulsacion() {
        Grabacion grabacion = new Grabacion(List.of(
                new PresionTecla(0, A),
                new PresionTecla(500, A),   // repeticiones automáticas del teclado
                new PresionTecla(530, A),
                new SueltaTecla(600, A)), 700);

        assertEquals(1, grabacion.cantidadTeclas());
    }

    @Test
    void laMismaTeclaPulsadaDosVecesCuentaDos() {
        Grabacion grabacion = new Grabacion(List.of(
                new PresionTecla(0, A), new SueltaTecla(50, A),
                new PresionTecla(100, A), new SueltaTecla(150, A)), 200);

        assertEquals(2, grabacion.cantidadTeclas());
    }

    @Test
    void dosTeclasSolapadasCuentanDosAunqueUnaSeRepita() {
        Grabacion grabacion = new Grabacion(List.of(
                new PresionTecla(0, A),
                new PresionTecla(20, B),
                new PresionTecla(500, B),
                new SueltaTecla(600, A),
                new SueltaTecla(650, B)), 700);

        assertEquals(2, grabacion.cantidadTeclas());
    }

    @Test
    void losClicsSeCuentanPorPresionDeBoton() {
        Grabacion grabacion = new Grabacion(List.of(
                new MovimientoMouse(0, 10, 10),
                new PresionBoton(100, 1, 10, 10), new SueltaBoton(130, 1, 10, 10),
                new PresionBoton(300, 3, 40, 40), new SueltaBoton(330, 3, 40, 40)), 400);

        assertEquals(2, grabacion.cantidadClics());
        assertEquals(0, grabacion.cantidadTeclas());
        assertFalse(grabacion.estaVacia());
    }

    @Test
    void noCambiaAunqueSeModifiqueLaListaOriginal() {
        List<EventoMacro> original = new ArrayList<>(List.of(new PresionTecla(0, A), new SueltaTecla(50, A)));
        Grabacion grabacion = new Grabacion(original, 100);

        original.clear();

        assertEquals(2, grabacion.eventos().size());
        assertThrows(UnsupportedOperationException.class, () -> grabacion.eventos().clear());
    }

    @Test
    void rechazaDuracionesNegativas() {
        assertThrows(IllegalArgumentException.class, () -> new Grabacion(List.of(), -1));
    }
}