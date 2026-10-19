package autoclicker.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionTecleoTest {

    private static final Atajo TECLA_ESPACIO = new Atajo(Atajo.Tipo.TECLA, 57, 0, "Espacio");
    private static final Atajo BOTON_MOUSE = new Atajo(Atajo.Tipo.MOUSE, 4, 0, "Botón lateral 1");

    @Test
    void aceptaValoresValidos() {
        assertDoesNotThrow(() -> new ConfiguracionTecleo(TECLA_ESPACIO, 10, 0, 0));
        assertDoesNotThrow(() -> new ConfiguracionTecleo(TECLA_ESPACIO, 100, 20, 3));
    }

    @Test
    void ceroRepeticionesSignificaInfinito() {
        assertTrue(new ConfiguracionTecleo(TECLA_ESPACIO, 100, 0, 3).esInfinito());
        assertFalse(new ConfiguracionTecleo(TECLA_ESPACIO, 100, 10, 3).esInfinito());
    }

    @Test
    void rechazaIntervalosMenoresAlMinimo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionTecleo(TECLA_ESPACIO, 9, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionTecleo(TECLA_ESPACIO, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionTecleo(TECLA_ESPACIO, -10, 0, 0));
    }

    @Test
    void rechazaRepeticionesOEsperaNegativas() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionTecleo(TECLA_ESPACIO, 100, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionTecleo(TECLA_ESPACIO, 100, 0, -1));
    }

    @Test
    void exigeTeclaNoNula() {
        assertThrows(NullPointerException.class,
                () -> new ConfiguracionTecleo(null, 100, 0, 0));
    }

    @Test
    void rechazaTeclaDeTipoMouse() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionTecleo(BOTON_MOUSE, 100, 0, 0));
    }
}
