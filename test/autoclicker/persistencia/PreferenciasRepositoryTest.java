package autoclicker.persistencia;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenciasRepositoryTest {

    private static final ConfiguracionClics PREDETERMINADA =
            new ConfiguracionClics(100, 0, 3, BotonMouse.IZQUIERDO, false);

    private Preferences nodo;
    private PreferenciasRepository repositorio;

    @BeforeEach
    void preparar() {
        nodo = new PreferenciasEnMemoria();
        repositorio = new PreferenciasRepository(nodo);
    }

    @Test
    void sinNadaGuardadoNoDevuelveAtajoNiTema() {
        assertTrue(repositorio.cargarAtajo("atajo").isEmpty());
        assertTrue(repositorio.cargarTema().isEmpty());
    }

    @Test
    void unAtajoDeTecladoSeGuardaYSeRecuperaIgual() {
        Atajo guardado = new Atajo(Atajo.Tipo.TECLA, 66, Atajo.CTRL | Atajo.SHIFT, "Ctrl + Shift + F8");

        repositorio.guardarAtajo("atajo.mouse.grabar", guardado);

        assertEquals(Optional.of(guardado), repositorio.cargarAtajo("atajo.mouse.grabar"));
    }

    @Test
    void unAtajoDeMouseSeGuardaYSeRecuperaIgual() {
        Atajo guardado = new Atajo(Atajo.Tipo.MOUSE, 5, 0, "Botón lateral 2");

        repositorio.guardarAtajo("atajo", guardado);

        assertEquals(Optional.of(guardado), repositorio.cargarAtajo("atajo"));
    }

    @Test
    void cadaClaveGuardaSuPropioAtajo() {
        Atajo grabar = new Atajo(Atajo.Tipo.TECLA, 65, 0, "F7");
        Atajo reproducir = new Atajo(Atajo.Tipo.TECLA, 66, 0, "F8");

        repositorio.guardarAtajo("atajo.mouse.grabar", grabar);
        repositorio.guardarAtajo("atajo.mouse.reproducir", reproducir);

        assertEquals(Optional.of(grabar), repositorio.cargarAtajo("atajo.mouse.grabar"));
        assertEquals(Optional.of(reproducir), repositorio.cargarAtajo("atajo.mouse.reproducir"));
        assertTrue(repositorio.cargarAtajo("atajo.teclado.grabar").isEmpty());
    }

    @Test
    void guardarDeNuevoReemplazaElAtajoAnterior() {
        repositorio.guardarAtajo("atajo", new Atajo(Atajo.Tipo.TECLA, 64, Atajo.CTRL, "Ctrl + F6"));
        Atajo nuevo = new Atajo(Atajo.Tipo.MOUSE, 4, 0, "Botón lateral 1");

        repositorio.guardarAtajo("atajo", nuevo);

        assertEquals(Optional.of(nuevo), repositorio.cargarAtajo("atajo"));
    }

    @Test
    void unTipoDesconocidoSeIgnoraEnLugarDeFallar() {
        repositorio.guardarAtajo("atajo", new Atajo(Atajo.Tipo.TECLA, 64, 0, "F6"));
        nodo.put("atajo.tipo", "JOYSTICK");

        assertTrue(repositorio.cargarAtajo("atajo").isEmpty());
    }

    @Test
    void unCodigoInvalidoSeIgnoraEnLugarDeFallar() {
        repositorio.guardarAtajo("atajo", new Atajo(Atajo.Tipo.TECLA, 64, 0, "F6"));
        nodo.putInt("atajo.codigo", 0);

        assertTrue(repositorio.cargarAtajo("atajo").isEmpty());
    }

    @Test
    void unAtajoAMediasSeIgnora() {
        nodo.put("atajo.tipo", "TECLA");
        nodo.putInt("atajo.codigo", 64);
        // falta el nombre

        assertTrue(repositorio.cargarAtajo("atajo").isEmpty());
    }

    @Test
    void elTemaSeGuardaYSeRecupera() {
        repositorio.guardarTema("OSCURO");

        assertEquals(Optional.of("OSCURO"), repositorio.cargarTema());
    }

    // ---- Parámetros del autoclicker ----

    @Test
    void sinNadaGuardadoLaConfiguracionDeClicsEsLaPredeterminada() {
        assertEquals(PREDETERMINADA, repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    @Test
    void laConfiguracionDeClicsSeGuardaYSeRecuperaIgual() {
        ConfiguracionClics guardada = new ConfiguracionClics(250, 40, 0, BotonMouse.DERECHO, true);

        repositorio.guardarConfiguracionClics(guardada);

        assertEquals(guardada, repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    @Test
    void guardarDeNuevoReemplazaLaConfiguracionDeClicsAnterior() {
        repositorio.guardarConfiguracionClics(new ConfiguracionClics(250, 40, 0, BotonMouse.DERECHO, true));
        ConfiguracionClics nueva = new ConfiguracionClics(5, 0, 10, BotonMouse.CENTRAL, false);

        repositorio.guardarConfiguracionClics(nueva);

        assertEquals(nueva, repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    @Test
    void loQueFaltaDeLaConfiguracionDeClicsSeCompletaConLoPredeterminado() {
        nodo.putLong("clics.intervaloMs", 750);

        assertEquals(new ConfiguracionClics(750, 0, 3, BotonMouse.IZQUIERDO, false),
                repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    @Test
    void unBotonDesconocidoSeIgnoraEnLugarDeFallar() {
        repositorio.guardarConfiguracionClics(new ConfiguracionClics(250, 40, 0, BotonMouse.DERECHO, true));
        nodo.put("clics.boton", "LATERAL");

        assertEquals(PREDETERMINADA, repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    @Test
    void unIntervaloInvalidoSeIgnoraEnLugarDeFallar() {
        repositorio.guardarConfiguracionClics(new ConfiguracionClics(250, 40, 0, BotonMouse.DERECHO, true));
        nodo.putLong("clics.intervaloMs", 0);

        assertEquals(PREDETERMINADA, repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    @Test
    void unNumeroQueNoSePuedeLeerSeCambiaPorElPredeterminado() {
        repositorio.guardarConfiguracionClics(new ConfiguracionClics(250, 40, 0, BotonMouse.DERECHO, true));
        nodo.put("clics.maxClics", "muchos");

        assertEquals(new ConfiguracionClics(250, 0, 0, BotonMouse.DERECHO, true),
                repositorio.cargarConfiguracionClics(PREDETERMINADA));
    }

    // ---- Repeticiones de las pestañas de grabación ----

    @Test
    void sinNadaGuardadoLasRepeticionesSonLasPredeterminadas() {
        assertEquals(1, repositorio.cargarRepeticiones("repeticiones.mouse", 1));
    }

    @Test
    void lasRepeticionesSeGuardanYSeRecuperan() {
        repositorio.guardarRepeticiones("repeticiones.mouse", 25);

        assertEquals(25, repositorio.cargarRepeticiones("repeticiones.mouse", 1));
    }

    @Test
    void ceroRepeticionesTambienSeRecuerda() {
        repositorio.guardarRepeticiones("repeticiones.teclado", 0);

        assertEquals(0, repositorio.cargarRepeticiones("repeticiones.teclado", 1));
    }

    @Test
    void cadaPestanaGuardaSusPropiasRepeticiones() {
        repositorio.guardarRepeticiones("repeticiones.mouse", 5);
        repositorio.guardarRepeticiones("repeticiones.teclado", 8);

        assertEquals(5, repositorio.cargarRepeticiones("repeticiones.mouse", 1));
        assertEquals(8, repositorio.cargarRepeticiones("repeticiones.teclado", 1));
        assertEquals(1, repositorio.cargarRepeticiones("repeticiones.completo", 1));
    }

    @Test
    void unasRepeticionesNegativasSeIgnoran() {
        nodo.putInt("repeticiones.mouse", -4);

        assertEquals(1, repositorio.cargarRepeticiones("repeticiones.mouse", 1));
    }
}