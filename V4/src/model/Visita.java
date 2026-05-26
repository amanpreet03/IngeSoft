package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
 * Una visita è un'istanza concreta di un TipoVisita su una data specifica.
 *
 * Ciclo di vita:
 *   PROPOSTA <-> COMPLETA   (prima della chiusura iscrizioni)
 *   PROPOSTA/COMPLETA -> CONFERMATA/CANCELLATA  (alla chiusura, 3 gg prima)
 *   CONFERMATA -> EFFETTUATA  (dopo il giorno di svolgimento)
 *
 * Invariante: tipoTag != null, data != null, guidaNickname != null, stato != null
 */
public class Visita {

    private final String tipoTag;       // tag del TipoVisita
    private final String luogoTag;      // tag del Luogo (per comodità di accesso)
    private final LocalDate data;
    private final String guidaNickname;
    private StatoVisita stato;
    private final List<Iscrizione> iscrizioni = new ArrayList<>();

    public Visita(String tipoTag, String luogoTag, LocalDate data, String guidaNickname) {
        this.tipoTag = tipoTag;
        this.luogoTag = luogoTag;
        this.data = data;
        this.guidaNickname = guidaNickname;
        this.stato = StatoVisita.PROPOSTA;
    }

    // ---- iscrizioni ----

    public int totaleIscritti() {
        return iscrizioni.stream().mapToInt(Iscrizione::getNumPersone).sum();
    }

    /*
     * Aggiunge un'iscrizione.
     * Pre:  stato == PROPOSTA, totale + nuovi <= maxPartecipanti del tipo
     * Post: se totale == max -> stato = COMPLETA
     */
    public void aggiungiIscrizione(Iscrizione i, int maxPartecipanti) {
        if (stato != StatoVisita.PROPOSTA)
            throw new IllegalStateException("La visita non accetta più iscrizioni.");
        if (totaleIscritti() + i.getNumPersone() > maxPartecipanti)
            throw new IllegalStateException("Posti insufficienti.");
        iscrizioni.add(i);
        if (totaleIscritti() >= maxPartecipanti) stato = StatoVisita.COMPLETA;
    }

    /*
     * Rimuove un'iscrizione per codice.
     * Se la visita era COMPLETA torna PROPOSTA.
     */
    public Iscrizione rimuoviIscrizione(String codice) {
        Iscrizione trovata = iscrizioni.stream()
            .filter(i -> i.getCodice().equalsIgnoreCase(codice))
            .findFirst().orElse(null);
        if (trovata != null) {
            iscrizioni.remove(trovata);
            if (stato == StatoVisita.COMPLETA) stato = StatoVisita.PROPOSTA;
        }
        return trovata;
    }

    public Optional<Iscrizione> cercaIscrizione(String codice) {
        return iscrizioni.stream()
            .filter(i -> i.getCodice().equalsIgnoreCase(codice))
            .findFirst();
    }

    // ---- transizioni di stato ----

    // chiamata 3 giorni prima dello svolgimento
    public void chiudiIscrizioni(int minPartecipanti) {
        stato = totaleIscritti() >= minPartecipanti
            ? StatoVisita.CONFERMATA
            : StatoVisita.CANCELLATA;
    }

    public void segnaEffettuata() { stato = StatoVisita.EFFETTUATA; }

    // ---- getter ----

    public String getTipoTag()         { return tipoTag; }
    public String getLuogoTag()        { return luogoTag; }
    public LocalDate getData()         { return data; }
    public String getGuidaNickname()   { return guidaNickname; }
    public StatoVisita getStato()      { return stato; }
    public void setStato(StatoVisita s){ this.stato = s; }
    public List<Iscrizione> getIscrizioni() { return Collections.unmodifiableList(iscrizioni); }

    // chiave unica della visita usata nel JSON: "dd-MM-yyyy|tagTipo"
    public String chiaveJson() {
        return data.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
               + "|" + tipoTag;
    }

    @Override
    public String toString() {
        return data + "  " + tipoTag + " [" + stato + "]";
    }
}
