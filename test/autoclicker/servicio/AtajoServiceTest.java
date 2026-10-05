package autoclicker.servicio;

import autoclicker.modelo.Atajo;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba la lógica de los atajos entregándole los avisos directamente, sin
 * activar la escucha global: así corre en cualquier máquina y sin pantalla.
 */
class AtajoServiceTest {

    private static final long MS = 1_000_000L;
    private static final int F6 = NativeKeyEvent.VC_F6;
    private static final int F7 = NativeKeyEvent.VC_F7;
    private static final int F8 = NativeKeyEvent.VC_F8;
    private static final int A = NativeKeyEvent.VC_A;

    private long ahora;                 // reloj falso, en nanosegundos
    private AtajoService servicio;
    private int disparos;
    private final List<String> recibidos = new ArrayList<>();

    @BeforeEach
    void preparar() {
        ahora = 1_000 * MS;
        disparos = 0;
        recibidos.clear();
        servicio = new AtajoService(() -> ahora);
        servicio.registrar("iniciar", AtajoService.F6, () -> disparos++);
        servicio.activarSolo(List.of("iniciar"));
        servicio.agregarOyente(new EntradaListener() {
            @Override public void teclaPresionada(NativeKeyEvent e, long instanteNs) { recibidos.add("presion " + e.getKeyCode() + " @" + instanteNs / MS); }
            @Override public void teclaSoltada(NativeKeyEvent e, long instanteNs) { recibidos.add("suelta " + e.getKeyCode() + " @" + instanteNs / MS); }
            @Override public void botonPresionado(NativeMouseEvent e, long instanteNs) { recibidos.add("boton " + e.getButton()); }
            @Override public void botonSoltado(NativeMouseEvent e, long instanteNs) { recibidos.add("boton suelto " + e.getButton()); }
        });
    }

    private void presionar(int codigo, int modificadores) {
        servicio.nativeKeyPressed(new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED,
                modificadores, 0, codigo, NativeKeyEvent.CHAR_UNDEFINED));
    }

    private void presionar(int codigo) {
        presionar(codigo, 0);
    }

    private void soltar(int codigo) {
        servicio.nativeKeyReleased(new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_RELEASED,
                0, 0, codigo, NativeKeyEvent.CHAR_UNDEFINED));
    }

    private void presionarBoton(int boton) {
        servicio.nativeMousePressed(new NativeMouseEvent(NativeMouseEvent.NATIVE_MOUSE_PRESSED, 0, 10, 10, 1, boton));
    }

    private void soltarBoton(int boton) {
        servicio.nativeMouseReleased(new NativeMouseEvent(NativeMouseEvent.NATIVE_MOUSE_RELEASED, 0, 10, 10, 1, boton));
    }

    private void avanzar(long milisegundos) {
        ahora += milisegundos * MS;
    }

    // ---- Disparo de atajos ----

    @Test
    void elAtajoSeDisparaUnaVezPorPulsacion() {
        presionar(F6);
        soltar(F6);
        avanzar(200);
        presionar(F6);
        soltar(F6);

        assertEquals(2, disparos);
    }

    @Test
    void mantenerElAtajoNoLoVuelveADisparar() {
        presionar(F6);
        avanzar(1000);                              // espera antes de la primera repetición
        for (int i = 0; i < 100; i++) {
            presionar(F6);                          // repeticiones automáticas del teclado
            avanzar(30);
        }
        soltar(F6);

        assertEquals(1, disparos);
    }

    @Test
    void siSePierdeElAvisoDeSoltarLaSiguientePulsacionResponde() {
        presionar(F6);                              // su "soltar" nunca llega (sesión bloqueada)
        avanzar(10_000);
        presionar(F6);

        assertEquals(2, disparos);
    }

    @Test
    void laTeclaDelAtajoNoLlegaALosOyentes() {
        presionar(F6);
        soltar(F6);

        assertTrue(recibidos.isEmpty(), "la grabadora no debe ver la tecla del atajo: " + recibidos);
    }

    @Test
    void lasDemasTeclasLleganALosOyentesConSuInstante() {
        presionar(A);
        avanzar(40);
        soltar(A);

        assertEquals(List.of("presion " + A + " @1000", "suelta " + A + " @1040"), recibidos);
        assertEquals(0, disparos);
    }

    @Test
    void soloRespondenLosAtajosActivos() {
        int[] grabaciones = {0};
        servicio.registrar("grabar", AtajoService.F7, () -> grabaciones[0]++);

        presionar(F7);                              // "grabar" está registrado pero no activo
        soltar(F7);
        assertEquals(0, grabaciones[0]);
        assertEquals(2, recibidos.size(), "una tecla de atajo inactivo es una tecla normal");

        servicio.activarSolo(List.of("grabar"));
        presionar(F7);
        soltar(F7);
        presionar(F6);                              // "iniciar" dejó de estar activo
        soltar(F6);

        assertEquals(1, grabaciones[0]);
        assertEquals(0, disparos);
    }

    @Test
    void unAtajoConModificadoresExigeEsosModificadores() {
        servicio.cambiarAtajo("iniciar", new Atajo(Atajo.Tipo.TECLA, F8, Atajo.CTRL, "Ctrl + F8"));

        presionar(F8);                              // sin Ctrl
        soltar(F8);
        assertEquals(0, disparos);

        presionar(F8, NativeInputEvent.CTRL_L_MASK);
        soltar(F8);
        presionar(F8, NativeInputEvent.CTRL_R_MASK);   // el Ctrl derecho vale igual
        soltar(F8);
        assertEquals(2, disparos);

        presionar(F8, NativeInputEvent.CTRL_L_MASK | NativeInputEvent.SHIFT_L_MASK);   // sobra Shift
        soltar(F8);
        assertEquals(2, disparos);
    }

    @Test
    void losBloqueosDelTecladoNoCuentanComoModificadores() {
        presionar(F6, NativeInputEvent.NUM_LOCK_MASK | NativeInputEvent.CAPS_LOCK_MASK);

        assertEquals(1, disparos);
    }

    @Test
    void conToleranciaElAtajoSeReconoceSoloPorLaTecla() {
        presionar(F6, NativeInputEvent.SHIFT_L_MASK);
        soltar(F6);
        assertEquals(0, disparos, "sin tolerancia, Shift + F6 no es F6");

        servicio.setTolerarModificadores(true);     // mientras se reproduce una grabación
        presionar(F6, NativeInputEvent.SHIFT_L_MASK);
        soltar(F6);

        assertEquals(1, disparos);
    }

    @Test
    void unBotonLateralPuedeSerAtajoPeroElClicNormalNo() {
        servicio.cambiarAtajo("iniciar",
                new Atajo(Atajo.Tipo.MOUSE, NativeMouseEvent.BUTTON4, 0, "Botón lateral 1"));

        presionarBoton(NativeMouseEvent.BUTTON4);
        soltarBoton(NativeMouseEvent.BUTTON4);
        assertEquals(1, disparos);
        assertTrue(recibidos.isEmpty(), "el botón del atajo no debe llegar a la grabadora: " + recibidos);

        presionarBoton(NativeMouseEvent.BUTTON1);
        soltarBoton(NativeMouseEvent.BUTTON1);
        assertEquals(1, disparos);
        assertEquals(List.of("boton " + NativeMouseEvent.BUTTON1, "boton suelto " + NativeMouseEvent.BUTTON1), recibidos);
    }

    // ---- Captura de un atajo nuevo ----

    @Test
    void capturarDevuelveLaTeclaConSusModificadores() {
        List<Atajo> capturados = new ArrayList<>();
        servicio.capturar(capturados::add);

        presionar(NativeKeyEvent.VC_CONTROL, NativeInputEvent.CTRL_L_MASK);   // un modificador solo no cuenta
        assertTrue(servicio.estaCapturando());

        presionar(F8, NativeInputEvent.CTRL_L_MASK);

        assertFalse(servicio.estaCapturando());
        assertEquals(1, capturados.size());
        Atajo atajo = capturados.get(0);
        assertEquals(Atajo.Tipo.TECLA, atajo.tipo());
        assertEquals(F8, atajo.codigo());
        assertEquals(Atajo.CTRL, atajo.modificadores());
        assertEquals("Ctrl + " + NativeKeyEvent.getKeyText(F8), atajo.nombre());
    }

    @Test
    void mientrasSeCapturaNoSeDisparanAtajos() {
        List<Atajo> capturados = new ArrayList<>();
        servicio.capturar(capturados::add);

        presionar(F6);                              // F6 es el atajo actual, pero aquí se está eligiendo
        soltar(F6);

        assertEquals(0, disparos);
        assertEquals(F6, capturados.get(0).codigo());
        assertTrue(recibidos.isEmpty(), "la tecla elegida no debe llegar a la grabadora: " + recibidos);
    }

    @Test
    void escapeCancelaLaCaptura() {
        List<Atajo> capturados = new ArrayList<>();
        servicio.capturar(capturados::add);

        presionar(NativeKeyEvent.VC_ESCAPE);

        assertFalse(servicio.estaCapturando());
        assertEquals(1, capturados.size());
        assertNull(capturados.get(0), "al cancelar se avisa con null");
    }

    @Test
    void laCapturaAceptaUnBotonLateralPeroNoElClicNormal() {
        List<Atajo> capturados = new ArrayList<>();
        servicio.capturar(capturados::add);

        presionarBoton(NativeMouseEvent.BUTTON1);
        assertTrue(servicio.estaCapturando(), "el clic normal no puede ser atajo");

        presionarBoton(NativeMouseEvent.BUTTON5);

        assertFalse(servicio.estaCapturando());
        assertEquals(Atajo.Tipo.MOUSE, capturados.get(0).tipo());
        assertEquals(NativeMouseEvent.BUTTON5, capturados.get(0).codigo());
    }

    // ---- Mensajes de fallo ----

    @Test
    void explicaLosPermisosQueFaltanEnMacOS() {
        AtajoService.FalloEscucha fallo = AtajoService.describir(
                new NativeHookException(NativeHookException.DARWIN_AXAPI_DISABLED, "Accessibility API is disabled"));

        assertTrue(fallo.resumen().contains("macOS"), fallo.resumen());
        assertTrue(fallo.ayuda().contains("Accesibilidad"), fallo.ayuda());
        assertTrue(fallo.ayuda().contains("Accessibility API is disabled"), "debe conservar el detalle técnico");
    }

    @Test
    void explicaQueLinuxNecesitaX11() {
        AtajoService.FalloEscucha fallo = AtajoService.describir(
                new NativeHookException(NativeHookException.X11_OPEN_DISPLAY, "Failed to open X11 display."));

        assertTrue(fallo.resumen().contains("X11"), fallo.resumen());
        assertTrue(fallo.ayuda().contains("Wayland"), fallo.ayuda());
    }

    @Test
    void unFalloDesconocidoTieneUnMensajeGeneral() {
        AtajoService.FalloEscucha fallo = AtajoService.describir(new NativeHookException("algo raro"));

        assertEquals("No se pudo activar la escucha global", fallo.resumen());
        assertTrue(fallo.ayuda().contains("algo raro"));
    }
}