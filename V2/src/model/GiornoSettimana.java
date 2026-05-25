package model;

import java.time.DayOfWeek;

// i sette giorni della settimana usati per definire quando una visita è programmabile
public enum GiornoSettimana {
    LUNEDI, MARTEDI, MERCOLEDI, GIOVEDI, VENERDI, SABATO, DOMENICA;

    // converte da DayOfWeek Java
    public static GiornoSettimana da(DayOfWeek d) {
        return switch (d) {
            case MONDAY    -> LUNEDI;
            case TUESDAY   -> MARTEDI;
            case WEDNESDAY -> MERCOLEDI;
            case THURSDAY  -> GIOVEDI;
            case FRIDAY    -> VENERDI;
            case SATURDAY  -> SABATO;
            case SUNDAY    -> DOMENICA;
        };
    }

    // etichetta breve per i JSON (es. LUNEDI -> "lun")
    public String tag() { return name().substring(0, 3).toLowerCase(); }

    public static GiornoSettimana daTag(String t) {
        return switch (t.toLowerCase()) {
            case "lun" -> LUNEDI;
            case "mar" -> MARTEDI;
            case "mer" -> MERCOLEDI;
            case "gio" -> GIOVEDI;
            case "ven" -> VENERDI;
            case "sab" -> SABATO;
            case "dom" -> DOMENICA;
            default    -> throw new IllegalArgumentException("Tag giorno sconosciuto: " + t);
        };
    }

    @Override
    public String toString() {
        String n = name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }
}
