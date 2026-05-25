package model;

// fase corrente del ciclo mensile dell'applicazione (V3)
public enum FaseOperativa {
    RACCOLTA,    // i volontari dichiarano disponibilità
    CHIUSURA,    // raccolta chiusa, si attende la pianificazione
    PIANO,       // piano generato, si possono fare modifiche ai dati
    APERTA;      // modifiche concluse, raccolta nuova aperta

    public String toJson() { return name().toLowerCase(); }

    public static FaseOperativa daJson(String s) {
        return switch (s.toLowerCase()) {
            case "chiusura" -> CHIUSURA;
            case "piano"    -> PIANO;
            case "aperta"   -> APERTA;
            default         -> RACCOLTA;
        };
    }
}
