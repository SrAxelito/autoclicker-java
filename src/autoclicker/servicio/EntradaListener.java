package autoclicker.servicio;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

/**
 * Recibe las pulsaciones globales de teclado y mouse que NO fueron
 * usadas como atajo. Lo usa la grabadora.
 *
 * Los avisos llegan en el hilo de Swing, que puede atenderlos con algo de
 * retraso. Por eso cada uno trae {@code instanteNs}: el valor de
 * System.nanoTime() del momento en que el evento llegó del sistema.
 */
public interface EntradaListener {
    default void teclaPresionada(NativeKeyEvent e, long instanteNs) { }
    default void teclaSoltada(NativeKeyEvent e, long instanteNs) { }
    default void botonPresionado(NativeMouseEvent e, long instanteNs) { }
    default void botonSoltado(NativeMouseEvent e, long instanteNs) { }
    default void ruedaMovida(NativeMouseWheelEvent e, long instanteNs) { }
}