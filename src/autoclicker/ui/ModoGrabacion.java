package autoclicker.ui;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.TipoGrabacion;
import autoclicker.servicio.AtajoService;
import autoclicker.servicio.GrabadoraService;
import autoclicker.servicio.ReproductorService;

import java.awt.AWTException;
import java.util.List;

/**
 * Todo lo que distingue a una pestaña de grabación de otra: sus textos, sus
 * atajos de fábrica y los servicios con los que graba y reproduce.
 *
 * @param titulo          texto de la pestaña
 * @param descripcion     frase que explica qué graba
 * @param prefijoAtajos   prefijo con el que se guardan sus atajos, por ejemplo "atajo.mouse"
 * @param atajoGrabar     atajo de fábrica para grabar y dejar de grabar
 * @param atajoReproducir atajo de fábrica para reproducir y detener
 * @param grabadora       servicio que graba lo que corresponde a este modo
 * @param reproductor     servicio que reproduce sus grabaciones
 */
public record ModoGrabacion(
        String titulo,
        String descripcion,
        String prefijoAtajos,
        Atajo atajoGrabar,
        Atajo atajoReproducir,
        GrabadoraService grabadora,
        ReproductorService reproductor) {

    public static ModoGrabacion mouse() throws AWTException {
        return new ModoGrabacion("Mouse",
                "Graba movimientos, clics y rueda del mouse",
                "atajo.mouse", AtajoService.F7, AtajoService.F8,
                new GrabadoraService(TipoGrabacion.MOUSE), new ReproductorService());
    }

    public static ModoGrabacion teclado() throws AWTException {
        return new ModoGrabacion("Teclado",
                "Graba las teclas que presionas y sueltas",
                "atajo.teclado", AtajoService.F9, AtajoService.F10,
                new GrabadoraService(TipoGrabacion.TECLADO), new ReproductorService());
    }

    public static ModoGrabacion completo() throws AWTException {
        return new ModoGrabacion("Mouse + Teclado",
                "Graba el mouse y el teclado al mismo tiempo",
                "atajo.completo", AtajoService.F11, AtajoService.F12,
                new GrabadoraService(TipoGrabacion.COMPLETA), new ReproductorService());
    }

    /**
     * Los tres modos de grabación, en el orden de las pestañas.
     * @throws AWTException si el sistema no permite controlar el mouse y el teclado
     */
    public static List<ModoGrabacion> predeterminados() throws AWTException {
        return List.of(mouse(), teclado(), completo());
    }
}