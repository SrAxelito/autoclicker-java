package autoclicker.persistencia;

import autoclicker.modelo.Atajo;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Guarda y recupera las preferencias del usuario entre ejecuciones.
 * Usa java.util.prefs, que en Windows las guarda en el registro del usuario.
 */
public class PreferenciasRepository {

    private static final String NODO = "autoclicker-java";

    private static final String TEMA = "tema";

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

    private void guardar() {
        try {
            prefs.flush();
        } catch (BackingStoreException ignored) {
            // Si no se puede guardar, la app sigue funcionando con los valores en memoria.
        }
    }
}
