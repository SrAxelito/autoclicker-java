package autoclicker.persistencia;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;

/**
 * Preferences que vive solo en memoria. Las pruebas la usan para no tocar
 * las preferencias reales del usuario (en Windows, el registro).
 */
final class PreferenciasEnMemoria extends AbstractPreferences {

    private final Map<String, String> valores = new HashMap<>();
    private final Map<String, PreferenciasEnMemoria> hijos = new HashMap<>();

    PreferenciasEnMemoria() {
        this(null, "");
    }

    private PreferenciasEnMemoria(AbstractPreferences padre, String nombre) {
        super(padre, nombre);
    }

    @Override protected void putSpi(String clave, String valor) { valores.put(clave, valor); }
    @Override protected String getSpi(String clave) { return valores.get(clave); }
    @Override protected void removeSpi(String clave) { valores.remove(clave); }
    @Override protected void removeNodeSpi() { valores.clear(); }
    @Override protected String[] keysSpi() { return valores.keySet().toArray(new String[0]); }
    @Override protected String[] childrenNamesSpi() { return hijos.keySet().toArray(new String[0]); }

    @Override
    protected AbstractPreferences childSpi(String nombre) {
        return hijos.computeIfAbsent(nombre, n -> new PreferenciasEnMemoria(this, n));
    }

    @Override protected void syncSpi() { }
    @Override protected void flushSpi() { }
}