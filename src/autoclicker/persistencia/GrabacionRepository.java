package autoclicker.persistencia;

import autoclicker.modelo.EventoMacro;
import autoclicker.modelo.EventoMacro.*;
import autoclicker.modelo.Grabacion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Guarda y carga grabaciones de macro en formato JSON estructurado y legible.
 * Almacena timestamps relativos, coordenadas (x, y), botones del mouse y códigos de teclas.
 * No requiere librerías externas.
 */
public final class GrabacionRepository {

    private GrabacionRepository() { }

    public static void guardar(File archivo, Grabacion grabacion) throws IOException {
        if (archivo == null) throw new IllegalArgumentException("El archivo no puede ser nulo.");
        guardar(archivo.toPath(), grabacion);
    }

    public static void guardar(Path ruta, Grabacion grabacion) throws IOException {
        if (ruta == null) throw new IllegalArgumentException("La ruta no puede ser nula.");
        String json = aJson(grabacion);
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        Files.writeString(ruta, json, StandardCharsets.UTF_8);
    }

    public static Grabacion cargar(File archivo) throws IOException {
        if (archivo == null) throw new IllegalArgumentException("El archivo no puede ser nulo.");
        return cargar(archivo.toPath());
    }

    public static Grabacion cargar(Path ruta) throws IOException {
        if (ruta == null) throw new IllegalArgumentException("La ruta no puede ser nula.");
        String contenido = Files.readString(ruta, StandardCharsets.UTF_8);
        return desdeJson(contenido);
    }

    /**
     * Serializa una grabación a formato JSON estructurado.
     */
    public static String aJson(Grabacion grabacion) {
        if (grabacion == null) {
            throw new IllegalArgumentException("La grabación no puede ser nula.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"duracionMs\": ").append(grabacion.duracionMs()).append(",\n");
        sb.append("  \"eventos\": [");

        List<EventoMacro> eventos = grabacion.eventos();
        if (eventos.isEmpty()) {
            sb.append("]\n");
        } else {
            sb.append("\n");
            for (int i = 0; i < eventos.size(); i++) {
                EventoMacro e = eventos.get(i);
                sb.append("    ");
                sb.append(eventoAJson(e));
                if (i < eventos.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String eventoAJson(EventoMacro e) {
        return switch (e) {
            case MovimientoMouse m -> String.format(Locale.ROOT,
                    "{\"tipo\": \"MOVIMIENTO_MOUSE\", \"tiempoMs\": %d, \"x\": %d, \"y\": %d}",
                    m.tiempoMs(), m.x(), m.y());
            case PresionBoton p -> String.format(Locale.ROOT,
                    "{\"tipo\": \"PRESION_BOTON\", \"tiempoMs\": %d, \"boton\": %d, \"x\": %d, \"y\": %d}",
                    p.tiempoMs(), p.boton(), p.x(), p.y());
            case SueltaBoton s -> String.format(Locale.ROOT,
                    "{\"tipo\": \"SUELTA_BOTON\", \"tiempoMs\": %d, \"boton\": %d, \"x\": %d, \"y\": %d}",
                    s.tiempoMs(), s.boton(), s.x(), s.y());
            case RuedaMouse r -> String.format(Locale.ROOT,
                    "{\"tipo\": \"RUEDA_MOUSE\", \"tiempoMs\": %d, \"giro\": %d}",
                    r.tiempoMs(), r.giro());
            case PresionTecla pt -> String.format(Locale.ROOT,
                    "{\"tipo\": \"PRESION_TECLA\", \"tiempoMs\": %d, \"codigoTecla\": %d}",
                    pt.tiempoMs(), pt.codigoTecla());
            case SueltaTecla st -> String.format(Locale.ROOT,
                    "{\"tipo\": \"SUELTA_TECLA\", \"tiempoMs\": %d, \"codigoTecla\": %d}",
                    st.tiempoMs(), st.codigoTecla());
        };
    }

    /**
     * Reconstruye una grabación a partir de un texto JSON.
     */
    public static Grabacion desdeJson(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("El contenido JSON no puede estar vacío.");
        }
        Object parsed;
        try {
            parsed = new JsonParser(json).parse();
        } catch (Exception e) {
            throw new IOException("Formato JSON inválido: " + e.getMessage(), e);
        }

        if (!(parsed instanceof Map<?, ?> rootMap)) {
            throw new IOException("El JSON raíz debe ser un objeto.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) rootMap;

        Object duracionObj = map.get("duracionMs");
        if (duracionObj == null) {
            throw new IOException("Falta la propiedad 'duracionMs' en la grabación.");
        }
        if (!(duracionObj instanceof Number numDuracion)) {
            throw new IOException("La propiedad 'duracionMs' debe ser numérica.");
        }
        long duracionMs = numDuracion.longValue();
        if (duracionMs < 0) {
            throw new IOException("La duración no puede ser negativa.");
        }

        Object eventosObj = map.get("eventos");
        if (eventosObj == null) {
            throw new IOException("Falta la lista de 'eventos' en la grabación.");
        }
        if (!(eventosObj instanceof List<?> eventosList)) {
            throw new IOException("La propiedad 'eventos' debe ser una lista.");
        }

        List<EventoMacro> eventos = new ArrayList<>();
        for (int i = 0; i < eventosList.size(); i++) {
            Object item = eventosList.get(i);
            if (!(item instanceof Map<?, ?> itemMap)) {
                throw new IOException("El evento en índice " + i + " debe ser un objeto.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> evMap = (Map<String, Object>) itemMap;
            eventos.add(mapAEvento(evMap, i));
        }

        eventos.sort(Comparator.comparingLong(EventoMacro::tiempoMs));

        long maxTiempo = eventos.isEmpty() ? 0 : eventos.get(eventos.size() - 1).tiempoMs();
        if (duracionMs < maxTiempo) {
            duracionMs = maxTiempo;
        }

        return new Grabacion(eventos, duracionMs);
    }

    private static EventoMacro mapAEvento(Map<String, Object> map, int index) throws IOException {
        Object tipoObj = map.get("tipo");
        if (!(tipoObj instanceof String tipo)) {
            throw new IOException("Evento " + index + ": propiedad 'tipo' faltante o inválida.");
        }

        long tiempoMs = getRequiredLong(map, "tiempoMs", index);
        if (tiempoMs < 0) {
            throw new IOException("Evento " + index + ": 'tiempoMs' no puede ser negativo.");
        }

        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "MOVIMIENTO_MOUSE", "MOVIMIENTO", "MOUSE_MOVE" -> {
                int x = getRequiredInt(map, "x", index);
                int y = getRequiredInt(map, "y", index);
                yield new MovimientoMouse(tiempoMs, x, y);
            }
            case "PRESION_BOTON", "BOTON_PRESIONADO", "MOUSE_PRESS" -> {
                int boton = getRequiredInt(map, "boton", index);
                int x = getRequiredInt(map, "x", index);
                int y = getRequiredInt(map, "y", index);
                yield new PresionBoton(tiempoMs, boton, x, y);
            }
            case "SUELTA_BOTON", "BOTON_SOLTADO", "MOUSE_RELEASE" -> {
                int boton = getRequiredInt(map, "boton", index);
                int x = getRequiredInt(map, "x", index);
                int y = getRequiredInt(map, "y", index);
                yield new SueltaBoton(tiempoMs, boton, x, y);
            }
            case "RUEDA_MOUSE", "RUEDA", "MOUSE_WHEEL" -> {
                int giro = getRequiredInt(map, "giro", index);
                yield new RuedaMouse(tiempoMs, giro);
            }
            case "PRESION_TECLA", "TECLA_PRESIONADA", "KEY_PRESS" -> {
                int codigoTecla = getRequiredInt(map, "codigoTecla", index);
                yield new PresionTecla(tiempoMs, codigoTecla);
            }
            case "SUELTA_TECLA", "TECLA_SOLTADA", "KEY_RELEASE" -> {
                int codigoTecla = getRequiredInt(map, "codigoTecla", index);
                yield new SueltaTecla(tiempoMs, codigoTecla);
            }
            default -> throw new IOException("Evento " + index + ": tipo desconocido '" + tipo + "'.");
        };
    }

    private static int getRequiredInt(Map<String, Object> map, String key, int index) throws IOException {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        throw new IOException("Evento " + index + ": falta la propiedad entera '" + key + "'.");
    }

    private static long getRequiredLong(Map<String, Object> map, String key, int index) throws IOException {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.longValue();
        }
        throw new IOException("Evento " + index + ": falta la propiedad numérica '" + key + "'.");
    }

    // ---- Parser JSON simple y autónomo ----

    private static final class JsonParser {
        private final String src;
        private int pos = 0;

        JsonParser(String src) {
            this.src = src;
        }

        Object parse() throws IOException {
            skipWhitespace();
            Object result = parseValue();
            skipWhitespace();
            if (pos < src.length()) {
                throw new IOException("Caracteres inesperados al final del JSON en la posición " + pos);
            }
            return result;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (pos >= src.length()) throw new IOException("Fin inesperado del JSON");
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            throw new IOException("Carácter inesperado '" + c + "' en la posición " + pos);
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                char c = peek();
                if (c == '}') {
                    pos++;
                    break;
                } else if (c == ',') {
                    pos++;
                } else {
                    throw new IOException("Se esperaba ',' o '}' en la posición " + pos);
                }
            }
            return map;
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                char c = peek();
                if (c == ']') {
                    pos++;
                    break;
                } else if (c == ',') {
                    pos++;
                } else {
                    throw new IOException("Se esperaba ',' o ']' en la posición " + pos);
                }
            }
            return list;
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) throw new IOException("Secuencia de escape incompleta");
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) throw new IOException("Escape unicode incompleto");
                            String hex = src.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new IOException("Carácter de escape no soportado: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IOException("Cadena de texto sin cerrar");
        }

        private Number parseNumber() throws IOException {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            boolean isFloating = false;
            if (pos < src.length() && src.charAt(pos) == '.') {
                isFloating = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                isFloating = true;
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            String numStr = src.substring(start, pos);
            try {
                if (isFloating) {
                    return Double.parseDouble(numStr);
                }
                long val = Long.parseLong(numStr);
                if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                    return (int) val;
                }
                return val;
            } catch (NumberFormatException e) {
                throw new IOException("Número no válido '" + numStr + "' en posición " + start);
            }
        }

        private Boolean parseBoolean() throws IOException {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IOException("Booleano inválido en la posición " + pos);
        }

        private Object parseNull() throws IOException {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IOException("Literal inválido en la posición " + pos);
        }

        private void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private void expect(char expected) throws IOException {
            if (pos >= src.length() || src.charAt(pos) != expected) {
                throw new IOException("Se esperaba '" + expected + "' en la posición " + pos);
            }
            pos++;
        }

        private char peek() throws IOException {
            if (pos >= src.length()) throw new IOException("Fin inesperado del JSON");
            return src.charAt(pos);
        }
    }
}
