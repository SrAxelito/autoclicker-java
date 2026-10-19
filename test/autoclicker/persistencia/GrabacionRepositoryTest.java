package autoclicker.persistencia;

import autoclicker.modelo.EventoMacro;
import autoclicker.modelo.EventoMacro.*;
import autoclicker.modelo.Grabacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrabacionRepositoryTest {

    @Test
    void guardaYLeeIntegraUnaGrabacionConTodosLosEventos(@TempDir Path tempDir) throws IOException {
        List<EventoMacro> eventos = List.of(
                new MovimientoMouse(0, 100, 200),
                new PresionBoton(50, 1, 100, 200),
                new SueltaBoton(120, 1, 105, 202),
                new RuedaMouse(200, -2),
                new PresionTecla(300, 65), // 'A'
                new SueltaTecla(400, 65),
                new PresionBoton(450, 3, 150, 250), // Botón derecho
                new SueltaBoton(490, 3, 150, 250)
        );
        Grabacion original = new Grabacion(eventos, 600);

        File archivo = tempDir.resolve("macro_test.json").toFile();
        GrabacionRepository.guardar(archivo, original);

        assertTrue(archivo.exists());
        assertTrue(archivo.length() > 0);

        Grabacion cargada = GrabacionRepository.cargar(archivo);

        assertNotNull(cargada);
        assertEquals(original.duracionMs(), cargada.duracionMs());
        assertEquals(original.eventos().size(), cargada.eventos().size());
        assertEquals(original.cantidadClics(), cargada.cantidadClics());
        assertEquals(original.cantidadTeclas(), cargada.cantidadTeclas());

        for (int i = 0; i < original.eventos().size(); i++) {
            EventoMacro eOriginal = original.eventos().get(i);
            EventoMacro eCargado = cargada.eventos().get(i);

            assertEquals(eOriginal.tiempoMs(), eCargado.tiempoMs(), "Tiempo del evento en índice " + i);
            assertEquals(eOriginal.getClass(), eCargado.getClass(), "Tipo del evento en índice " + i);

            switch (eOriginal) {
                case MovimientoMouse m -> {
                    MovimientoMouse mc = (MovimientoMouse) eCargado;
                    assertEquals(m.x(), mc.x());
                    assertEquals(m.y(), mc.y());
                }
                case PresionBoton pb -> {
                    PresionBoton pbc = (PresionBoton) eCargado;
                    assertEquals(pb.boton(), pbc.boton());
                    assertEquals(pb.x(), pbc.x());
                    assertEquals(pb.y(), pbc.y());
                }
                case SueltaBoton sb -> {
                    SueltaBoton sbc = (SueltaBoton) eCargado;
                    assertEquals(sb.boton(), sbc.boton());
                    assertEquals(sb.x(), sbc.x());
                    assertEquals(sb.y(), sbc.y());
                }
                case RuedaMouse rm -> {
                    RuedaMouse rmc = (RuedaMouse) eCargado;
                    assertEquals(rm.giro(), rmc.giro());
                }
                case PresionTecla pt -> {
                    PresionTecla ptc = (PresionTecla) eCargado;
                    assertEquals(pt.codigoTecla(), ptc.codigoTecla());
                }
                case SueltaTecla st -> {
                    SueltaTecla stc = (SueltaTecla) eCargado;
                    assertEquals(st.codigoTecla(), stc.codigoTecla());
                }
            }
        }
    }

    @Test
    void guardaYLeeGrabacionVacia() throws IOException {
        String json = GrabacionRepository.aJson(Grabacion.VACIA);
        Grabacion recuperada = GrabacionRepository.desdeJson(json);

        assertTrue(recuperada.estaVacia());
        assertEquals(0, recuperada.duracionMs());
        assertEquals(0, recuperada.eventos().size());
    }

    @Test
    void ordenaEventosDesordenadosPorTiempo() throws IOException {
        String json = """
                {
                  "version": 1,
                  "duracionMs": 500,
                  "eventos": [
                    {"tipo": "SUELTA_TECLA", "tiempoMs": 200, "codigoTecla": 65},
                    {"tipo": "MOVIMIENTO_MOUSE", "tiempoMs": 10, "x": 10, "y": 20},
                    {"tipo": "PRESION_TECLA", "tiempoMs": 50, "codigoTecla": 65}
                  ]
                }
                """;

        Grabacion cargada = GrabacionRepository.desdeJson(json);
        assertEquals(3, cargada.eventos().size());
        assertEquals(10, cargada.eventos().get(0).tiempoMs());
        assertInstanceOf(MovimientoMouse.class, cargada.eventos().get(0));
        assertEquals(50, cargada.eventos().get(1).tiempoMs());
        assertInstanceOf(PresionTecla.class, cargada.eventos().get(1));
        assertEquals(200, cargada.eventos().get(2).tiempoMs());
        assertInstanceOf(SueltaTecla.class, cargada.eventos().get(2));
    }

    @Test
    void ajustaDuracionSiElUltimoEventoSuperaLaDuracionIndicada() throws IOException {
        String json = """
                {
                  "version": 1,
                  "duracionMs": 100,
                  "eventos": [
                    {"tipo": "PRESION_TECLA", "tiempoMs": 250, "codigoTecla": 32}
                  ]
                }
                """;

        Grabacion cargada = GrabacionRepository.desdeJson(json);
        assertEquals(250, cargada.duracionMs());
    }

    @Test
    void rechazaJsonInvalidoOIncompleto() {
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson(null));
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson(""));
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("{ esto no es json }"));
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("{\"duracionMs\": 100}")); // falta eventos
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("{\"eventos\": []}")); // falta duracionMs
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("{\"duracionMs\": -10, \"eventos\": []}"));
    }

    @Test
    void rechazaEventosConCamposFaltantesOTiposDesconocidos() {
        // Falta campo x e y
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("""
                {
                  "duracionMs": 100,
                  "eventos": [
                    {"tipo": "MOVIMIENTO_MOUSE", "tiempoMs": 10}
                  ]
                }
                """));

        // Tipo desconocido
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("""
                {
                  "duracionMs": 100,
                  "eventos": [
                    {"tipo": "TELETRANSPORTE", "tiempoMs": 10}
                  ]
                }
                """));

        // Tiempo negativo
        assertThrows(IOException.class, () -> GrabacionRepository.desdeJson("""
                {
                  "duracionMs": 100,
                  "eventos": [
                    {"tipo": "PRESION_TECLA", "tiempoMs": -5, "codigoTecla": 65}
                  ]
                }
                """));
    }
}
