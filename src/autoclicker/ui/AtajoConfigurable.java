package autoclicker.ui;

import autoclicker.modelo.Atajo;
import autoclicker.persistencia.PreferenciasRepository;
import autoclicker.servicio.AtajoService;
import autoclicker.ui.componentes.SelectorAtajo;

import java.util.ArrayList;
import java.util.List;

/**
 * Une un atajo con su selector visual: lo registra en el servicio, lo guarda
 * en las preferencias y evita que dos atajos del mismo modo sean iguales.
 */
final class AtajoConfigurable {

    private final String clave;
    private final AtajoService atajos;
    private final PreferenciasRepository preferencias;
    private final SelectorAtajo selector = new SelectorAtajo();
    private final List<AtajoConfigurable> hermanos = new ArrayList<>();
    private Runnable alCambiar = () -> { };

    AtajoConfigurable(String clave, Atajo predeterminado, Runnable accion,
                      AtajoService atajos, PreferenciasRepository preferencias) {
        this.clave = clave;
        this.atajos = atajos;
        this.preferencias = preferencias;

        Atajo inicial = preferencias.cargarAtajo(clave).orElse(predeterminado);
        atajos.registrar(clave, inicial, accion);

        selector.setDisponible(atajos.estaDisponible());
        selector.setNombre(inicial.nombre());
        selector.setAlActivar(this::alternarCaptura);
    }

    /** Los atajos del mismo modo no pueden repetirse entre sí. */
    static void sonHermanos(AtajoConfigurable a, AtajoConfigurable b) {
        a.hermanos.add(b);
        b.hermanos.add(a);
    }

    String clave() {
        return clave;
    }

    SelectorAtajo selector() {
        return selector;
    }

    String nombre() {
        return atajos.atajo(clave).nombre();
    }

    void setAlCambiar(Runnable accion) {
        this.alCambiar = (accion != null) ? accion : () -> { };
    }

    void setEnabled(boolean habilitado) {
        selector.setEnabled(habilitado);
    }

    private void alternarCaptura() {
        if (atajos.estaCapturando()) {
            boolean eraEste = selector.estaCapturando();
            atajos.cancelarCaptura(); // avisa al selector que estaba capturando
            if (eraEste) return;
        }
        selector.setCapturando(true);
        atajos.capturar(nuevo -> {
            selector.setCapturando(false);
            if (nuevo == null) return; // cancelado con Esc
            for (AtajoConfigurable otro : hermanos) {
                if (mismaEntrada(otro.atajos.atajo(otro.clave), nuevo)) {
                    selector.mostrarAviso("Ya se usa en este modo");
                    return;
                }
            }
            atajos.cambiarAtajo(clave, nuevo);
            preferencias.guardarAtajo(clave, nuevo);
            selector.setNombre(nuevo.nombre());
            alCambiar.run();
        });
    }

    private static boolean mismaEntrada(Atajo a, Atajo b) {
        return a != null && a.tipo() == b.tipo() && a.codigo() == b.codigo() && a.modificadores() == b.modificadores();
    }
}
