package autoclicker.servicio;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.SwingKeyAdapter;

import java.awt.event.KeyEvent;

/**
 * Pasa de código nativo de JNativeHook a código de tecla de Java (AWT/Robot)
 * reutilizando la tabla interna de {@link SwingKeyAdapter}.
 */
public class ConversorTeclas extends SwingKeyAdapter {

    private static final ConversorTeclas INSTANCIA = new ConversorTeclas();

    /** Convierte un evento nativo al código de tecla correspondiente de Java (KeyEvent.VK_*). */
    public static int aCodigoJava(NativeKeyEvent e) {
        return INSTANCIA.codigoJava(e);
    }

    /**
     * Convierte un código nativo (NativeKeyEvent.VC_*) al código de tecla correspondiente
     * de Java (KeyEvent.VK_*). Devuelve KeyEvent.VK_UNDEFINED si no tiene equivalente.
     */
    public static int aCodigoJava(int codigoNativo) {
        NativeKeyEvent e = new NativeKeyEvent(
                NativeKeyEvent.NATIVE_KEY_PRESSED,
                0,
                0,
                codigoNativo,
                NativeKeyEvent.CHAR_UNDEFINED);
        return INSTANCIA.codigoJava(e);
    }

    /**
     * Comprueba si una tecla nativa se puede convertir a un código válido de Java que Robot sepa pulsar.
     */
    public static boolean esConvertible(int codigoNativo) {
        int codigoJava = aCodigoJava(codigoNativo);
        return codigoJava != KeyEvent.VK_UNDEFINED && codigoJava > 0;
    }

    public int codigoJava(NativeKeyEvent e) {
        KeyEvent javaEvent = getJavaKeyEvent(e);
        return javaEvent != null ? javaEvent.getKeyCode() : KeyEvent.VK_UNDEFINED;
    }

    public int codigoJava(int codigoNativo) {
        return aCodigoJava(codigoNativo);
    }
}
