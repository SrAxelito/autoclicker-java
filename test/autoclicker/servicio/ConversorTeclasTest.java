package autoclicker.servicio;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversorTeclasTest {

    @Test
    void convierteLetrasComunes() {
        assertEquals(KeyEvent.VK_A, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_A));
        assertEquals(KeyEvent.VK_B, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_B));
        assertEquals(KeyEvent.VK_C, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_C));
        assertEquals(KeyEvent.VK_V, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_V));
        assertEquals(KeyEvent.VK_Z, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_Z));
    }

    @Test
    void convierteNumeros() {
        assertEquals(KeyEvent.VK_0, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_0));
        assertEquals(KeyEvent.VK_1, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_1));
        assertEquals(KeyEvent.VK_5, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_5));
        assertEquals(KeyEvent.VK_9, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_9));
    }

    @Test
    void convierteEspacioYEnter() {
        assertEquals(KeyEvent.VK_SPACE, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_SPACE));
        assertEquals(KeyEvent.VK_ENTER, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_ENTER));
    }

    @Test
    void convierteTeclasDeFuncion() {
        assertEquals(KeyEvent.VK_F1, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F1));
        assertEquals(KeyEvent.VK_F2, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F2));
        assertEquals(KeyEvent.VK_F3, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F3));
        assertEquals(KeyEvent.VK_F4, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F4));
        assertEquals(KeyEvent.VK_F5, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F5));
        assertEquals(KeyEvent.VK_F6, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F6));
        assertEquals(KeyEvent.VK_F7, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F7));
        assertEquals(KeyEvent.VK_F8, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F8));
        assertEquals(KeyEvent.VK_F9, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F9));
        assertEquals(KeyEvent.VK_F10, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F10));
        assertEquals(KeyEvent.VK_F11, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F11));
        assertEquals(KeyEvent.VK_F12, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_F12));
    }

    @Test
    void convierteFlechas() {
        assertEquals(KeyEvent.VK_UP, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_UP));
        assertEquals(KeyEvent.VK_DOWN, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_DOWN));
        assertEquals(KeyEvent.VK_LEFT, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_LEFT));
        assertEquals(KeyEvent.VK_RIGHT, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_RIGHT));
    }

    @Test
    void convierteModificadores() {
        assertEquals(KeyEvent.VK_CONTROL, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_CONTROL));
        assertEquals(KeyEvent.VK_SHIFT, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_SHIFT));
        assertEquals(KeyEvent.VK_ALT, ConversorTeclas.aCodigoJava(NativeKeyEvent.VC_ALT));
    }

    @Test
    void esConvertibleValidaTeclasSoportadas() {
        assertTrue(ConversorTeclas.esConvertible(NativeKeyEvent.VC_A));
        assertTrue(ConversorTeclas.esConvertible(NativeKeyEvent.VC_SPACE));
        assertTrue(ConversorTeclas.esConvertible(NativeKeyEvent.VC_F6));
    }
}
