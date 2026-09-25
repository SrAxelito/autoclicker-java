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

    private static final String ATAJO_TIPO   = "atajo.tipo";
    private static final String ATAJO_CODIGO = "atajo.codigo";
    private static final String ATAJO_MODS   = "atajo.modificadores";
    private static final String ATAJO_NOMBRE = "atajo.nombre";
    private static final String TEMA         = "tema";

    private final Preferences prefs;

    public PreferenciasRepository() {
        this(Preferences.userRoot().node(NODO));
    }

    /** Permite usar otro nodo (útil para pruebas). */
    public PreferenciasRepository(Preferences prefs) {
        this.prefs = prefs;
    }

    public Optional<Atajo> cargarAtajo() {
        String tipo = prefs.get(ATAJO_TIPO, null);
        String nombre = prefs.get(ATAJO_NOMBRE, null);
        if (tipo == null || nombre == null) return Optional.empty();
        try {
            return Optional.of(new Atajo(
                    Atajo.Tipo.valueOf(tipo),
                    prefs.getInt(ATAJO_CODIGO, -1),
                    prefs.getInt(ATAJO_MODS, 0),
                    nombre));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // datos guardados corruptos: se usa el predeterminado
        }
    }

    public void guardarAtajo(Atajo atajo) {
        prefs.put(ATAJO_TIPO, atajo.tipo().name());
        prefs.putInt(ATAJO_CODIGO, atajo.codigo());
        prefs.putInt(ATAJO_MODS, atajo.modificadores());
        prefs.put(ATAJO_NOMBRE, atajo.nombre());
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
