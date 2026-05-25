package archivio;

// stati del ciclo di vita di una visita guidata
public enum StatoVisita {
    PROPOSTA,    // visibile, aperta alle iscrizioni
    COMPLETA,    // posti esauriti
    CONFERMATA,  // iscrizioni chiuse, soglia minima raggiunta
    CANCELLATA,  // iscrizioni chiuse, sotto la soglia
    EFFETTUATA;  // svolta, finisce nell'archivio

    public String toJson() { return name().toLowerCase(); }

    public static StatoVisita daJson(String s) {
        return switch (s.toLowerCase()) {
            case "proposta"   -> PROPOSTA;
            case "completa"   -> COMPLETA;
            case "confermata" -> CONFERMATA;
            case "cancellata" -> CANCELLATA;
            case "effettuata" -> EFFETTUATA;
            default           -> PROPOSTA;
        };
    }
}
