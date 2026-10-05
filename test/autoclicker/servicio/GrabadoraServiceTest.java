package autoclicker.servicio;

import autoclicker.modelo.EventoMacro;
import autoclicker.modelo.EventoMacro.MovimientoMouse;
import autoclicker.modelo.EventoMacro.PresionBoton;
import autoclicker.modelo.EventoMacro.PresionTecla;
import autoclicker.modelo.EventoMacro.SueltaBoton;
import autoclicker.modelo.EventoMacro.SueltaTecla;
import autoclicker.modelo.Grabacion;
import autoclicker.modelo.TipoGrabacion;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrabadoraServiceTest {

    private static final long MS = 1_000_000L;
    private static final int A = KeyEvent.VK_A;
    private static final int CTRL = KeyEvent.VK_CONTROL;

    // ---- equilibrar ----

    @Test
    void equilibrarQuitaLaSueltaQueNoTienePresion() {
        List<EventoMacro> lista = new ArrayList<>(List.of(
                new SueltaTecla(5, KeyEvent.VK_F7),            // la tecla con la que se empezó a grabar
                new PresionTecla(100, A), new SueltaTecla(150, A)));

        GrabadoraService.equilibrar(lista, 1000);

        assertEquals(List.of(new PresionTecla(100, A), new SueltaTecla(150, A)), lista);
    }

    @Test
    void equilibrarSueltaAlFinalLaTeclaQueQuedoPresionada() {
        List<EventoMacro> lista = new ArrayList<>(List.of(
                new PresionTecla(100, A), new SueltaTecla(150, A),
                new PresionTecla(900, CTRL)));                  // el Ctrl del atajo con el que se detuvo

        GrabadoraService.equilibrar(lista, 1000);

        assertEquals(new SueltaTecla(1000, CTRL), lista.get(lista.size() - 1));
        assertTrue(quedanPresionadas(lista).isEmpty());
    }

    @Test
    void equilibrarSueltaElBotonDondeEstabaElMouse() {
        List<EventoMacro> lista = new ArrayList<>(List.of(
                new PresionBoton(100, 1, 20, 30),
                new MovimientoMouse(200, 300, 400)));           // se detuvo a mitad de un arrastre

        GrabadoraService.equilibrar(lista, 500);

        assertEquals(new SueltaBoton(500, 1, 300, 400), lista.get(lista.size() - 1));
    }

    @Test
    void equilibrarDejaSueltaUnaTeclaConRepeticiones() {
        List<EventoMacro> lista = new ArrayList<>(List.of(
                new PresionTecla(0, A), new PresionTecla(500, A), new PresionTecla(530, A),
                new SueltaTecla(600, A)));

        GrabadoraService.equilibrar(lista, 700);

        assertTrue(quedanPresionadas(lista).isEmpty());
        assertEquals(1, new Grabacion(lista, 700).cantidadTeclas());
    }

    @Test
    void equilibrarNoTocaUnaGrabacionCompleta() {
        List<EventoMacro> original = List.of(
                new MovimientoMouse(0, 1, 1),
                new PresionBoton(10, 1, 1, 1), new SueltaBoton(40, 1, 1, 1),
                new PresionTecla(100, A), new SueltaTecla(150, A));
        List<EventoMacro> lista = new ArrayList<>(original);

        GrabadoraService.equilibrar(lista, 200);

        assertEquals(original, lista);
    }

    /** Recorre la lista como lo haría el reproductor y devuelve las teclas que quedarían presionadas. */
    private static Set<Integer> quedanPresionadas(List<EventoMacro> lista) {
        Set<Integer> presionadas = new HashSet<>();
        for (EventoMacro evento : lista) {
            if (evento instanceof PresionTecla presion) presionadas.add(presion.codigoTecla());
            if (evento instanceof SueltaTecla suelta) presionadas.remove(suelta.codigoTecla());
        }
        return presionadas;
    }

    // ---- quitarUltimoClic ----

    @Test
    void quitarUltimoClicBorraEseClicYLoQueVieneDespues() {
        List<EventoMacro> lista = new ArrayList<>(List.of(
                new MovimientoMouse(0, 1, 1),
                new PresionBoton(300, 1, 5, 5), new SueltaBoton(350, 1, 5, 5),
                new MovimientoMouse(500, 9, 9),
                new PresionBoton(800, 1, 400, 400),             // el clic sobre el botón "Detener"
                new SueltaBoton(850, 1, 400, 400)));

        long duracion = GrabadoraService.quitarUltimoClic(lista, 900);

        assertEquals(800, duracion);
        assertEquals(4, lista.size());
        assertEquals(new MovimientoMouse(500, 9, 9), lista.get(3));
    }

    @Test
    void quitarUltimoClicSinClicsNoCambiaNada() {
        List<EventoMacro> lista = new ArrayList<>(List.of(new MovimientoMouse(0, 1, 1), new MovimientoMouse(50, 2, 2)));

        long duracion = GrabadoraService.quitarUltimoClic(lista, 900);

        assertEquals(900, duracion);
        assertEquals(2, lista.size());
    }

    // ---- botonJava ----

    @Test
    void traduceLosBotonesDeJNativeHookALosDeJava() {
        assertEquals(1, GrabadoraService.botonJava(NativeMouseEvent.BUTTON1), "izquierdo");
        assertEquals(3, GrabadoraService.botonJava(NativeMouseEvent.BUTTON2), "derecho");
        assertEquals(2, GrabadoraService.botonJava(NativeMouseEvent.BUTTON3), "central");
        assertEquals(4, GrabadoraService.botonJava(NativeMouseEvent.BUTTON4));
        assertEquals(5, GrabadoraService.botonJava(NativeMouseEvent.BUTTON5));
        assertEquals(0, GrabadoraService.botonJava(NativeMouseEvent.NOBUTTON));
    }

    // ---- grabar el teclado (no necesita pantalla) ----

    private static NativeKeyEvent presion(int codigoNativo) {
        return new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, 0, 0, codigoNativo, NativeKeyEvent.CHAR_UNDEFINED);
    }

    private static NativeKeyEvent suelta(int codigoNativo) {
        return new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_RELEASED, 0, 0, codigoNativo, NativeKeyEvent.CHAR_UNDEFINED);
    }

    @Test
    void sinIniciarNoGrabaNada() {
        GrabadoraService grabadora = new GrabadoraService(TipoGrabacion.TECLADO);

        grabadora.teclaPresionada(presion(NativeKeyEvent.VC_A), System.nanoTime());

        assertFalse(grabadora.estaGrabando());
        assertTrue(grabadora.detener(false).estaVacia());
    }

    @Test
    void grabaCadaTeclaConLaHoraEnQueLlego() {
        GrabadoraService grabadora = new GrabadoraService(TipoGrabacion.TECLADO);
        grabadora.iniciar();
        long inicio = System.nanoTime();

        grabadora.teclaPresionada(presion(NativeKeyEvent.VC_A), inicio + 50 * MS);
        grabadora.teclaSoltada(suelta(NativeKeyEvent.VC_A), inicio + 80 * MS);
        List<EventoMacro> eventos = grabadora.detener(false).eventos();

        assertEquals(2, eventos.size());
        PresionTecla presionada = assertInstanceOf(PresionTecla.class, eventos.get(0));
        SueltaTecla soltada = assertInstanceOf(SueltaTecla.class, eventos.get(1));
        assertEquals(A, presionada.codigoTecla());
        assertEquals(A, soltada.codigoTecla());
        // La hora sale del instante recibido, no del momento en que se atendió el aviso
        assertEquals(30, soltada.tiempoMs() - presionada.tiempoMs());
        assertTrue(presionada.tiempoMs() >= 50, "la presión debe quedar a 50 ms del inicio o un poco después");
    }

    @Test
    void ignoraLoQueOcurrioAntesDeEmpezarAGrabar() {
        GrabadoraService grabadora = new GrabadoraService(TipoGrabacion.TECLADO);
        long antes = System.nanoTime() - 20 * MS;
        grabadora.iniciar();

        grabadora.teclaPresionada(presion(NativeKeyEvent.VC_A), antes);   // llegó antes, se atiende después

        assertTrue(grabadora.detener(false).estaVacia());
    }

    @Test
    void alDetenerSueltaLaTeclaQueSeguiaPresionada() {
        GrabadoraService grabadora = new GrabadoraService(TipoGrabacion.TECLADO);
        grabadora.iniciar();

        grabadora.teclaPresionada(presion(NativeKeyEvent.VC_SHIFT), System.nanoTime());
        Grabacion grabacion = grabadora.detener(false);

        assertEquals(2, grabacion.eventos().size());
        SueltaTecla ultima = assertInstanceOf(SueltaTecla.class, grabacion.eventos().get(1));
        assertEquals(KeyEvent.VK_SHIFT, ultima.codigoTecla());
        assertEquals(grabacion.duracionMs(), ultima.tiempoMs());
    }

    @Test
    void laGrabadoraDeMouseNoGuardaTeclas() {
        GrabadoraService grabadora = new GrabadoraService(TipoGrabacion.MOUSE);

        assertFalse(grabadora.tipo().incluyeTeclado());
        assertTrue(new GrabadoraService(TipoGrabacion.COMPLETA).tipo().incluyeTeclado());
    }
}