package model;

import java.time.LocalDate;
import java.util.*;

/*
 * Una visita è un'istanza concreta di un TipoVisita su una data specifica.
 *
 * Ciclo di vita:
 *   PROPOSTA <-> COMPLETA   (prima della chiusura iscrizioni)
 *   PROPOSTA/COMPLETA -> CONFERMATA/CANCELLATA  (alla chiusura, 3 gg prima)
 *   CONFERMATA -> EFFETTUATA  (dopo il giorno di svolgimento)
 *
 * Invariante: tipoTag != null, data != null, guidaNickname != null, stato != null, iscrizioni != null, totaleIscritti >= 0
 */
public class Visita {

    private final String tipoTag;       // tag del TipoVisita
    private final String luogoTag;      // tag del Luogo (per comodità di accesso)
    private final LocalDate data;
    private final String guidaNickname;
    private StatoVisita stato;
    private final List<Iscrizione> iscrizioni = new ArrayList<>();

    /** Costruisce una visita.
     * Pre: tipoTag != null, data != null, guidaNickname != null
     * Post: la visita è inizializzata con i parametri specificati
     */
    public Visita(String tipoTag, String luogoTag, LocalDate data, String guidaNickname) {
        this.tipoTag = tipoTag;
        this.luogoTag = luogoTag;
        this.data = data;
        this.guidaNickname = guidaNickname;
        this.stato = StatoVisita.PROPOSTA;
    }

    // === ISCRIZIONI ===

    /** Calcola il totale delle persone iscirtte.
     * @return la somma del numero di persone in tutte le iscrizioni
     */
    public int totaleIscritti() {
        return iscrizioni.stream().mapToInt(Iscrizione::getNumPersone).sum();
    }

    /**
     * Aggiunge un'iscrizione alla visita.
     * aggiorna lo stato se necessario (PROPOSTA -> COMPLETA).
     * @pre stato == PROPOSTA, totale + nuovi <= maxPartecipanti del tipo
     * @post se totale == max -> stato = COMPLETA
     */
    
    public void aggiungiIscrizione(Iscrizione i, int maxPartecipanti) {
        if (stato != StatoVisita.PROPOSTA)
            throw new IllegalStateException("La visita non accetta più iscrizioni.");
        if (totaleIscritti() + i.getNumPersone() > maxPartecipanti)
            throw new IllegalStateException("Posti insufficienti.");
        iscrizioni.add(i);
        if (totaleIscritti() >= maxPartecipanti) stato = StatoVisita.COMPLETA;
    }

    /**
     * Rimuove un'iscrizione per codice.
     * Se la visita era COMPLETA torna PROPOSTA.
     * @pre: codice != null
     * @pre stato == PROPOSTA || stato == COMPLETA
     * @return: l'iscrizione rimossa, o null se non trovata
     */
    public Iscrizione rimuoviIscrizione(String codice) {
        Iscrizione trovata = iscrizioni.stream()
            .filter(i -> i.getCodice().equalsIgnoreCase(codice))
            .findFirst().orElse(null);
        if (trovata != null) {
            iscrizioni.remove(trovata);
            if (stato == StatoVisita.COMPLETA) stato = StatoVisita.PROPOSTA; // torna proposta se disponibilità
        }
        return trovata;
    }
    /**
     * Cerca un'iscrizione per codice di prenotazione.
     * @pre: codice != null
     * @return: l'iscrizione trovata, o null se non trovata
     */
    public Optional<Iscrizione> cercaIscrizione(String codice) {
        return iscrizioni.stream()
            .filter(i -> i.getCodice().equalsIgnoreCase(codice))
            .findFirst();
    }

    // === TRANSIIZIONI  DI STATO ===

    /** 
     * Chiude le iscrizioni e aggiorna lo stato della visita.
     * Chiamato 3 giorni prima della data di svolgimento.
     * @pre stato == PROPOSTA || stato == COMPLETA
     * @post stato == CONFERMATA || stato == CANCELLATA
     */
    public void chiudiIscrizioni(int minPartecipanti) {
        stato = totaleIscritti() >= minPartecipanti
            ? StatoVisita.CONFERMATA
            : StatoVisita.CANCELLATA;
        
    }
 /**
     * Marca la visita come effettuata (il giorno successivo allo svolgimento).
     * @pre stato == CONFERMATA
     * @post stato == EFFETTUATA
     */
    public void segnaEffettuata() { stato = StatoVisita.EFFETTUATA; }

    // === GETTER ===

    public String getTipoTag()         { return tipoTag; }
    public String getLuogoTag()        { return luogoTag; }
    public LocalDate getData()         { return data; }
    public String getGuidaNickname()   { return guidaNickname; }
    public StatoVisita getStato()      { return stato; }
    public void setStato(StatoVisita s){ this.stato = s; }
    public List<Iscrizione> getIscrizioni() { return Collections.unmodifiableList(iscrizioni); }

    // chiave unica della visita usata nel JSON: "dd-MM-yyyy|tagTipo"
    public String chiaveJson() {
        return data.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
               + "|" + tipoTag;
    }

    @Override
    public String toString() {
        return data + " – " + tipoTag + " [" + stato + "]";
    }
}
