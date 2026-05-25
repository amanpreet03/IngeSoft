package model;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.LocalTime;
import java.util.*;

/*
 * Template da cui si generano le visite effettive.
 * Invariante: tag != null, titolo != null, volontari != null,
 *             durata > 0, min >= 1, max >= min
 */
public class TipoVisita {

    private final String tag;         // identificatore unico (es. "camera_sposi")
    private final String titolo;
    private final String luogoTag;    // tag del luogo a cui appartiene
    private final String descrizione;
    private final String puntoIncontro;
    private final MonthDay inizioPeriodo; // es. 01-11
    private final MonthDay finePeriodo;
    private final Set<GiornoSettimana> giorni;
    private final LocalTime oraInizio; // es. 15:30
    private final int durata;         // minuti
    private final boolean bigliettoRichiesto; // si/no
    private final int minPartecipanti;
    private final int maxPartecipanti;
    private final List<String> volontari; // nickname dei volontari abilitati

/** Costruisce un tipo di visita.
 * Pre: tag != null && titolo != null && luogoTag != null
 *      descrizione != null && puntoIncontro != null
 *      inizioPeriodo != null && finePeriodo != null
 *      giorni != null && !giorni.isEmpty()
 *      oraInizio != null
 *      durata > 0
 *      minPartecipanti >= 1 && maxPartecipanti >= minPartecipanti
 *      volontari != null
 */
    private TipoVisita(Builder builder) {
        this.tag = builder.tag;
        this.titolo = builder.titolo;
        this.luogoTag = builder.luogoTag;
        this.descrizione = builder.descrizione;
        this.puntoIncontro = builder.puntoIncontro;
        this.inizioPeriodo = builder.inizioPeriodo;
        this.finePeriodo = builder.finePeriodo;
        this.giorni = Collections.unmodifiableSet(new LinkedHashSet<>(builder.giorni));
        this.oraInizio = builder.oraInizio;
        this.durata = builder.durata;
        this.bigliettoRichiesto = builder.bigliettoRichiesto;
        this.minPartecipanti = builder.minPartecipanti;
        this.maxPartecipanti = builder.maxPartecipanti;
        this.volontari = new ArrayList<>(builder.volontari);
    }

/** Restituisce un builder per costruire un tipo di visita.
 * Pre: tag != null && titolo != null && luogoTag != null
 * Post: il builder è inizializzato con i parametri specificati
 */
    public static Builder builder(String tag, String titolo, String luogoTag) {
        return new Builder(tag, titolo, luogoTag);
    }

    public static final class Builder {
        private final String tag;
        private final String titolo;
        private final String luogoTag;
        private String descrizione = "";
        private String puntoIncontro = "";
        private MonthDay inizioPeriodo;
        private MonthDay finePeriodo;
        private Set<GiornoSettimana> giorni = new LinkedHashSet<>();
        private LocalTime oraInizio;
        private int durata;
        private boolean bigliettoRichiesto;
        private int minPartecipanti;
        private int maxPartecipanti;
        private List<String> volontari = new ArrayList<>();

        private Builder(String tag, String titolo, String luogoTag) {
            this.tag = tag;
            this.titolo = titolo;
            this.luogoTag = luogoTag;
        }

        public Builder descrizione(String descrizione) {
            this.descrizione = descrizione;
            return this;
        }

        public Builder puntoIncontro(String puntoIncontro) {
            this.puntoIncontro = puntoIncontro;
            return this;
        }

        public Builder periodo(MonthDay inizioPeriodo, MonthDay finePeriodo) {
            this.inizioPeriodo = inizioPeriodo;
            this.finePeriodo = finePeriodo;
            return this;
        }

        public Builder giorni(Set<GiornoSettimana> giorni) {
            this.giorni = new LinkedHashSet<>(giorni);
            return this;
        }

        public Builder oraInizio(LocalTime oraInizio) {
            this.oraInizio = oraInizio;
            return this;
        }

        public Builder durata(int durata) {
            this.durata = durata;
            return this;
        }

        public Builder bigliettoRichiesto(boolean bigliettoRichiesto) {
            this.bigliettoRichiesto = bigliettoRichiesto;
            return this;
        }

        public Builder partecipanti(int minPartecipanti, int maxPartecipanti) {
            this.minPartecipanti = minPartecipanti;
            this.maxPartecipanti = maxPartecipanti;
            return this;
        }

        public Builder volontari(List<String> volontari) {
            this.volontari = new ArrayList<>(volontari);
            return this;
        }

        public TipoVisita build() {
            return new TipoVisita(this);
        }
    }

    // la visita può svolgersi in questo giorno della settimana?
    public boolean attivaNelGiorno(GiornoSettimana g) { return giorni.contains(g); }

    // la data cade nel periodo annuale di programmabilità?
    public boolean nelPeriodo(LocalDate d) {
        MonthDay md = MonthDay.from(d);
        if (!inizioPeriodo.isAfter(finePeriodo))
            return !md.isBefore(inizioPeriodo) && !md.isAfter(finePeriodo);
        // periodo a cavallo d'anno (es. nov -> gen)
        return !md.isBefore(inizioPeriodo) || !md.isAfter(finePeriodo);
    }

    // due tipi si sovrappongono se condividono un giorno E i loro orari si toccano
    public boolean sovrappone(TipoVisita altro) {
        boolean giornoComune = giorni.stream().anyMatch(altro.giorni::contains);
        if (!giornoComune) return false;
        LocalTime mioFine    = oraInizio.plusMinutes(durata);
        LocalTime suoFine    = altro.oraInizio.plusMinutes(altro.durata);
        return oraInizio.isBefore(suoFine) && altro.oraInizio.isBefore(mioFine);
    }

    public void aggiungiVolontario(String nickname) {
        if (!volontari.contains(nickname)) volontari.add(nickname);
    }

    public boolean rimuoviVolontario(String nickname) { return volontari.remove(nickname); }

    // ---- getter ----
    public String getTag()              { return tag; }
    public String getTitolo()           { return titolo; }
    public String getLuogoTag()         { return luogoTag; }
    public String getDescrizione()      { return descrizione; }
    public String getPuntoIncontro()    { return puntoIncontro; }
    public MonthDay getInizioPeriodo()  { return inizioPeriodo; }
    public MonthDay getFinePeriodo()    { return finePeriodo; }
    public Set<GiornoSettimana> getGiorni() { return giorni; }
    public LocalTime getOraInizio()     { return oraInizio; }
    public int getDurata()              { return durata; }
    public boolean vuoleBiglietto()     { return bigliettoRichiesto; }
    public int getMin()                 { return minPartecipanti; }
    public int getMax()                 { return maxPartecipanti; }
    public List<String> getVolontari()  { return Collections.unmodifiableList(volontari); }

    @Override
    public String toString() { return "[" + tag + "] " + titolo; }
}
