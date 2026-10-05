package autoclicker.persistencia;

import autoclicker.modelo.Atajo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenciasRepositoryTest {

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
}