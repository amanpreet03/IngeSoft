package storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/*
 * Parser e writer JSON artigianale senza dipendenze esterne.
 * Supporta oggetti, array, stringhe, numeri, booleani e null.
 * Tutte le operazioni su file sono bufferizzate.
 */
public class JsonIO {

    private JsonIO() {}

    // Lettura

    @SuppressWarnings("unchecked")
    public static Map<String, Object> leggi(String percorso) {
        try {
            String testo = new String(Files.readAllBytes(Paths.get(percorso)),
                StandardCharsets.UTF_8).trim();
            if (testo.isEmpty()) return nuovaMappa();
            Object r = parse(testo, new int[]{0});
            return r instanceof Map ? (Map<String, Object>) r : nuovaMappa();
        } catch (IOException e) {
            System.err.println("[JsonIO] Impossibile leggere " + percorso + ": " + e.getMessage());
            return nuovaMappa();
        }
    }

    // Scrittura

    public static void scrivi(String percorso, Map<String, Object> dati) {
        try {
            Files.createDirectories(Paths.get(percorso).getParent());
            Files.write(Paths.get(percorso),
                formato(dati, 0).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[JsonIO] Impossibile scrivere " + percorso + ": " + e.getMessage());
        }
    }

    // Parser ricorsivo

    private static Object parse(String s, int[] p) {
        avanza(s, p);
        if (p[0] >= s.length()) return null;
        return switch (s.charAt(p[0])) {
            case '{' -> parseOggetto(s, p);
            case '[' -> parseArray(s, p);
            case '"' -> parseStringa(s, p);
            case 't' -> { p[0] += 4; yield Boolean.TRUE; }
            case 'f' -> { p[0] += 5; yield Boolean.FALSE; }
            case 'n' -> { p[0] += 4; yield null; }
            default  -> parseNumero(s, p);
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseOggetto(String s, int[] p) {
        Map<String, Object> m = new LinkedHashMap<>();
        p[0]++; // '{'
        avanza(s, p);
        while (p[0] < s.length() && s.charAt(p[0]) != '}') {
            avanza(s, p);
            if (s.charAt(p[0]) == '}') break;
            String k = parseStringa(s, p);
            avanza(s, p); p[0]++; // ':'
            Object v = parse(s, p);
            m.put(k, v);
            avanza(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == ',') p[0]++;
        }
        if (p[0] < s.length()) p[0]++; // '}'
        return m;
    }

    private static List<Object> parseArray(String s, int[] p) {
        List<Object> l = new ArrayList<>();
        p[0]++; // '['
        avanza(s, p);
        while (p[0] < s.length() && s.charAt(p[0]) != ']') {
            l.add(parse(s, p));
            avanza(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == ',') p[0]++;
        }
        if (p[0] < s.length()) p[0]++; // ']'
        return l;
    }

    private static String parseStringa(String s, int[] p) {
        p[0]++; // '"' iniziale
        StringBuilder sb = new StringBuilder();
        while (p[0] < s.length() && s.charAt(p[0]) != '"') {
            char c = s.charAt(p[0]++);
            if (c == '\\' && p[0] < s.length()) {
                switch (s.charAt(p[0]++)) {
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case 'r'  -> sb.append('\r');
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default   -> sb.append('?');
                }
            } else sb.append(c);
        }
        if (p[0] < s.length()) p[0]++; // '"' finale
        return sb.toString();
    }

    private static Object parseNumero(String s, int[] p) {
        int i = p[0];
        boolean dec = false;
        if (p[0] < s.length() && s.charAt(p[0]) == '-') p[0]++;
        while (p[0] < s.length()) {
            char c = s.charAt(p[0]);
            if (c == '.') dec = true;
            if (!Character.isDigit(c) && c != '.' && c != 'e' && c != 'E'
                    && c != '+' && c != '-') break;
            p[0]++;
        }
        String n = s.substring(i, p[0]);
        if (n.isEmpty()) return null;
        try { return dec ? Double.parseDouble(n) : Long.parseLong(n); }
        catch (NumberFormatException e) { return null; }
    }

    private static void avanza(String s, int[] p) {
        while (p[0] < s.length() && Character.isWhitespace(s.charAt(p[0]))) p[0]++;
    }

    // Serializzatore con indentazione

    public static String formato(Object o, int lv) {
        if (o == null)     return "null";
        if (o instanceof Boolean || o instanceof Long || o instanceof Integer
                || o instanceof Double) return o.toString();
        if (o instanceof Number)  return String.valueOf(((Number) o).longValue());
        if (o instanceof String)  return '"' + esc((String) o) + '"';
        if (o instanceof Map)     return formatoMappa((Map<?,?>) o, lv);
        if (o instanceof List)    return formatoLista((List<?>) o, lv);
        return '"' + esc(o.toString()) + '"';
    }

    private static String formatoMappa(Map<?,?> m, int lv) {
        if (m.isEmpty()) return "{}";
        String ind = "  ".repeat(lv + 1);
        String chiusura = "  ".repeat(lv);
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<?,?> e : m.entrySet()) {
            sb.append(ind).append('"').append(e.getKey()).append("\": ")
              .append(formato(e.getValue(), lv + 1));
            if (++i < m.size()) sb.append(',');
            sb.append('\n');
        }
        return sb.append(chiusura).append('}').toString();
    }

    private static String formatoLista(List<?> l, int lv) {
        if (l.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            sb.append(formato(l.get(i), lv));
            if (i < l.size() - 1) sb.append(", ");
        }
        return sb.append(']').toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // Helpers per accesso sicuro ai valori

    @SuppressWarnings("unchecked")
    public static Map<String, Object> oggettoSafe(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : nuovaMappa();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> listaSafe(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    public static String strSafe(Object o) {
        if (o instanceof String) return (String) o;
        if (o != null) return o.toString();
        return "";
    }

    public static int intSafe(Object o) {
        if (o instanceof Long)    return ((Long) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Double)  return ((Double) o).intValue();
        return 0;
    }

    public static boolean boolSafe(Object o) { return Boolean.TRUE.equals(o); }

    public static Map<String, Object> nuovaMappa() { return new LinkedHashMap<>(); }
}
