package autoclicker.servicio;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

/**
 * Recibe las pulsaciones globales de teclado y mouse que NO fueron
 * usadas como atajo. Lo usa la grabadora.
 */
public interface EntradaListener {
    default void teclaPresionada(NativeKeyEvent e) { }
    default void teclaSoltada(NativeKeyEvent e) { }
    default void botonPresionado(NativeMouseEvent e) { }
    default void botonSoltado(NativeMouseEvent e) { }
    default void ruedaMovida(NativeMouseWheelEvent e) { }
}
