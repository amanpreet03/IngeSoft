package storage;

// percorsi dei file JSON usati dall'applicazione
// modificare qui se si sposta la cartella json
public class Percorsi {
    public static final String AMBITO                        = "json/ambito_territoriale.json";
    public static final String UTENTI                        = "json/users.json";
    public static final String PIANO                         = "json/piano_visite.json";
    public static final String STORICO_TIPO                  = "json/storico/tipo_visite.json";
    public static final String STORICO_VISITE_DA_PUB         = "json/storico/visite_da_pubblicare.json";
    public static final String STORICO_VISITE_EFFETTUATE     = "json/storico/visite_effettuate.json";
    public static final String PRENOTAZIONI                  = "json/prenotazioni.json";

    private Percorsi() {}
}
