package autoclicker.persistencia;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import autoclicker.modelo.ConfiguracionTecleo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenciasRepositoryTest {

    private static final ConfiguracionClics PREDETERMINADA =
            new ConfiguracionClics(100, 0, 3, BotonMouse.IZQUIERDO, false);
    private static final ConfiguracionTecleo PREDETERMINADA_TECLEO =
            new ConfiguracionTecleo(new Atajo(Atajo.Tipo.TECLA, 57, 0, "Espacio"), 100, 0, 3);

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
        ConfiguracionClics nueva = new ConfiguracionClics(15, 0, 10, BotonMouse.CENTRAL, false);

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
    void unIntervaloMenorAlMinimoSeSubeAlMinimoConservandoLosDemasParametros() {
        repositorio.guardarConfiguracionClics(new ConfiguracionClics(250, 40, 5, BotonMouse.DERECHO, false));
        nodo.putLong("clics.intervaloMs", 1);

        assertEquals(new ConfiguracionClics(10, 40, 5, BotonMouse.DERECHO, false),
                repositorio.cargarConfiguracionClics(PREDETERMINADA));

        nodo.putBoolean("clics.dobleClic", true);
        assertEquals(new ConfiguracionClics(20, 40, 5, BotonMouse.DERECHO, true),
                repositorio.cargarConfiguracionClics(PREDETERMINADA));

        nodo.putBoolean("clics.dobleClic", false);
        nodo.putLong("clics.intervaloMs", 0);
        assertEquals(new ConfiguracionClics(10, 40, 5, BotonMouse.DERECHO, false),
                repositorio.cargarConfiguracionClics(PREDETERMINADA));
        nodo.putLong("clics.intervaloMs", -15);
        assertEquals(new ConfiguracionClics(10, 40, 5, BotonMouse.DERECHO, false),
                repositorio.cargarConfiguracionClics(PREDETERMINADA));
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

    // ---- Parámetros del autoteclado ----

    @Test
    void sinNadaGuardadoLaConfiguracionDeTecleoEsLaPredeterminada() {
        assertEquals(PREDETERMINADA_TECLEO, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));
    }

    @Test
    void laConfiguracionDeTecleoSeGuardaYSeRecupera() {
        Atajo ctrlV = new Atajo(Atajo.Tipo.TECLA, 47, Atajo.CTRL, "Ctrl + V");
        ConfiguracionTecleo guardada = new ConfiguracionTecleo(ctrlV, 50, 10, 1);

        repositorio.guardarConfiguracionTecleo(guardada);

        assertEquals(guardada, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));
    }

    @Test
    void datosCorruptosVuelvenLaConfiguracionPredeterminada() {
        Atajo ctrlV = new Atajo(Atajo.Tipo.TECLA, 47, Atajo.CTRL, "Ctrl + V");
        repositorio.guardarConfiguracionTecleo(new ConfiguracionTecleo(ctrlV, 50, 10, 1));

        // Tecla de tipo MOUSE en tecleo: corrupto
        nodo.put("tecleo.tecla.tipo", "MOUSE");
        assertEquals(PREDETERMINADA_TECLEO, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));

        // Repeticiones negativas: corrupto
        nodo.put("tecleo.tecla.tipo", "TECLA");
        nodo.putInt("tecleo.repeticiones", -5);
        assertEquals(PREDETERMINADA_TECLEO, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));

        // Espera negativa: corrupto
        nodo.putInt("tecleo.repeticiones", 0);
        nodo.putInt("tecleo.esperaSegundos", -1);
        assertEquals(PREDETERMINADA_TECLEO, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));
    }

    @Test
    void unIntervaloDeTecleoMenorAlMinimoSeSubeAlMinimo() {
        Atajo espacio = new Atajo(Atajo.Tipo.TECLA, 57, 0, "Espacio");
        repositorio.guardarConfiguracionTecleo(new ConfiguracionTecleo(espacio, 50, 10, 1));
        nodo.putLong("tecleo.intervaloMs", 1);

        assertEquals(new ConfiguracionTecleo(espacio, 10, 10, 1),
                repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));

        nodo.putLong("tecleo.intervaloMs", 0);
        assertEquals(new ConfiguracionTecleo(espacio, 10, 10, 1),
                repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));

        nodo.putLong("tecleo.intervaloMs", -10);
        assertEquals(new ConfiguracionTecleo(espacio, 10, 10, 1),
                repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO));
    }

    // ---- Monitor activo ----

    @Test
    void elMonitorDeCadaConfiguracionSeGuardaYSeRecupera() {
        ConfiguracionClics clics = new ConfiguracionClics(100, 0, 3, BotonMouse.IZQUIERDO, false, 1);
        repositorio.guardarConfiguracionClics(clics);
        assertEquals(1, repositorio.cargarConfiguracionClics(PREDETERMINADA).monitor());

        Atajo espacio = new Atajo(Atajo.Tipo.TECLA, 57, 0, "Espacio");
        ConfiguracionTecleo tecleo = new ConfiguracionTecleo(espacio, 100, 0, 3, 2);
        repositorio.guardarConfiguracionTecleo(tecleo);
        assertEquals(2, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO).monitor());
    }

    @Test
    void sinDatoDeMonitorVuelveElPredeterminado() {
        assertEquals(ConfiguracionClics.TODOS_LOS_MONITORES,
                repositorio.cargarConfiguracionClics(PREDETERMINADA).monitor());
        assertEquals(ConfiguracionClics.TODOS_LOS_MONITORES,
                repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO).monitor());
    }

    @Test
    void unValorMenorQueMenosUnoVuelveElPredeterminado() {
        nodo.putInt("clics.monitor", -2);
        assertEquals(ConfiguracionClics.TODOS_LOS_MONITORES,
                repositorio.cargarConfiguracionClics(PREDETERMINADA).monitor());

        nodo.putInt("tecleo.monitor", -5);
        assertEquals(ConfiguracionClics.TODOS_LOS_MONITORES,
                repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO).monitor());
    }

    @Test
    void clicsMonitorYTecleoMonitorNoSeMezclan() {
        ConfiguracionClics clics = new ConfiguracionClics(100, 0, 3, BotonMouse.IZQUIERDO, false, 0);
        Atajo espacio = new Atajo(Atajo.Tipo.TECLA, 57, 0, "Espacio");
        ConfiguracionTecleo tecleo = new ConfiguracionTecleo(espacio, 100, 0, 3, 1);

        repositorio.guardarConfiguracionClics(clics);
        repositorio.guardarConfiguracionTecleo(tecleo);

        assertEquals(0, repositorio.cargarConfiguracionClics(PREDETERMINADA).monitor());
        assertEquals(1, repositorio.cargarConfiguracionTecleo(PREDETERMINADA_TECLEO).monitor());
    }
}