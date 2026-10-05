package autoclicker.persistencia;

import autoclicker.modelo.Atajo;
import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Guarda y recupera las preferencias del usuario entre ejecuciones: los
 * atajos, el tema y los parámetros de cada pestaña.
 * Usa java.util.prefs, que en Windows las guarda en el registro del usuario.
 */
public class PreferenciasRepository {

    private static final String NODO = "autoclicker-java";

    private static final String TEMA = "tema";

    private static final String CLICS_INTERVALO = "clics.intervaloMs";
    private static final String CLICS_MAXIMO    = "clics.maxClics";
    private static final String CLICS_ESPERA    = "clics.esperaSegundos";
    private static final String CLICS_BOTON     = "clics.boton";
    private static final String CLICS_DOBLE     = "clics.dobleClic";

    private final Preferences prefs;

    public PreferenciasRepository() {
        this(Preferences.userRoot().node(NODO));
    }

    /** Permite usar otro nodo (útil para pruebas). */
    public PreferenciasRepository(Preferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Carga un atajo guardado.
     * @param clave prefijo con el que se guardó, por ejemplo "atajo.mouse.grabar"
     */
    public Optional<Atajo> cargarAtajo(String clave) {
        String tipo = prefs.get(clave + ".tipo", null);
        String nombre = prefs.get(clave + ".nombre", null);
        if (tipo == null || nombre == null) return Optional.empty();
        try {
            return Optional.of(new Atajo(
                    Atajo.Tipo.valueOf(tipo),
                    prefs.getInt(clave + ".codigo", -1),
                    prefs.getInt(clave + ".modificadores", 0),
                    nombre));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // datos guardados corruptos: se usa el predeterminado
        }
    }

    public void guardarAtajo(String clave, Atajo atajo) {
        prefs.put(clave + ".tipo", atajo.tipo().name());
        prefs.putInt(clave + ".codigo", atajo.codigo());
        prefs.putInt(clave + ".modificadores", atajo.modificadores());
        prefs.put(clave + ".nombre", atajo.nombre());
        guardar();
    }

    public Optional<String> cargarTema() {
        return Optional.ofNullable(prefs.get(TEMA, null));
    }

    public void guardarTema(String tema) {
        prefs.put(TEMA, tema);
        guardar();
    }

    /**
     * Carga los parámetros del autoclicker de la última vez.
     * @param predeterminada valores que se usan para lo que no esté guardado;
     *                       también se devuelve completa si lo guardado no es válido
     */
    public ConfiguracionClics cargarConfiguracionClics(ConfiguracionClics predeterminada) {
        try {
            return new ConfiguracionClics(
                    prefs.getLong(CLICS_INTERVALO, predeterminada.intervaloMs()),
                    prefs.getInt(CLICS_MAXIMO, predeterminada.maxClics()),
                    prefs.getInt(CLICS_ESPERA, predeterminada.esperaSegundos()),
                    BotonMouse.valueOf(prefs.get(CLICS_BOTON, predeterminada.boton().name())),
                    prefs.getBoolean(CLICS_DOBLE, predeterminada.dobleClic()));
        } catch (IllegalArgumentException e) {
            return predeterminada; // datos guardados corruptos: se usan los predeterminados
        }
    }

    public void guardarConfiguracionClics(ConfiguracionClics config) {
        prefs.putLong(CLICS_INTERVALO, config.intervaloMs());
        prefs.putInt(CLICS_MAXIMO, config.maxClics());
        prefs.putInt(CLICS_ESPERA, config.esperaSegundos());
        prefs.put(CLICS_BOTON, config.boton().name());
        prefs.putBoolean(CLICS_DOBLE, config.dobleClic());
        guardar();
    }

    /**
     * Carga las repeticiones de una pestaña de grabación.
     * @param clave           con la que se guardaron, por ejemplo "repeticiones.mouse"
     * @param predeterminadas valor que se devuelve si no hay nada guardado o no es válido
     */
    public int cargarRepeticiones(String clave, int predeterminadas) {
        int guardadas = prefs.getInt(clave, predeterminadas);
        return (guardadas >= 0) ? guardadas : predeterminadas;
    }

    public void guardarRepeticiones(String clave, int repeticiones) {
        prefs.putInt(clave, repeticiones);
        guardar();
    }

    private void guardar() {
        try {
            prefs.flush();
        } catch (BackingStoreException ignored) {
            // Si no se puede guardar, la app sigue funcionando con los valores en memoria.
        }
    }
}