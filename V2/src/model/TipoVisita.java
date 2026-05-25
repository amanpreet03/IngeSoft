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
    private final String descrizione; // opzionale, può essere vuota
    private final String puntoIncontro; // indiizzo o punto di riferimento per il ritrovo dei partecipanti
    private final MonthDay inizioPeriodo;
    private final MonthDay finePeriodo;
    private final Set<GiornoSettimana> giorni;
    private final LocalTime oraInizio;
    private final int durata;         // minuti
    private final boolean bigliettoRichiesto;
    private final int minPartecipanti;
    private final int maxPartecipanti;
    private final List<String> volontari; // nickname dei volontari abilitati

    public TipoVisita(String tag, String titolo, String luogoTag, String descrizione,
                      String puntoIncontro, MonthDay inizioPeriodo, MonthDay finePeriodo,
                      Set<GiornoSettimana> giorni, LocalTime oraInizio, int durata,
                      boolean bigliettoRichiesto, int minPartecipanti, int maxPartecipanti,
                      List<String> volontari) {
        this.tag = tag;
        this.titolo = titolo;
        this.luogoTag = luogoTag;
        this.descrizione = descrizione;
        this.puntoIncontro = puntoIncontro;
        this.inizioPeriodo = inizioPeriodo;
        this.finePeriodo = finePeriodo;
        this.giorni = Collections.unmodifiableSet(new LinkedHashSet<>(giorni));
        this.oraInizio = oraInizio;
        this.durata = durata;
        this.bigliettoRichiesto = bigliettoRichiesto;
        this.minPartecipanti = minPartecipanti;
        this.maxPartecipanti = maxPartecipanti;
        this.volontari = new ArrayList<>(volontari);
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
