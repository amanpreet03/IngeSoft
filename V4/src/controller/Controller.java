package controller;

import model.*;
import storage.Persistenza;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Unico controller dell'applicazione.
 * Tutta la logica applicativa passa da qui; la UI non tocca il modello direttamente.
 *
 * Invariante: sistema != null
 */
public class Controller {

    private final Sistema sistema;

    public Controller(Sistema sistema) {
        this.sistema = sistema;
    }

    // 
    // LOGIN / REGISTRAZIONE
    // 

    public Configuratore loginConfiguratore(String username, String password) {
        Configuratore c = sistema.trovaConfiguratore(username)
            .orElseThrow(() -> new IllegalArgumentException("Username non trovato."));
        if (!c.verificaPassword(password))
            throw new IllegalArgumentException("Password errata.");
        return c;
    }

    public void cambiaPasswordConfiguratore(Configuratore c, String nuova) {
        c.cambiaPassword(nuova);
        Persistenza.salvaUtenti(sistema);
    }

    public void registraConfiguratore(String credUsr, String credPwd, String nuovoUsr, String nuovaPwd) {
        if (!Sistema.CRED_USR.equals(credUsr) || !Sistema.CRED_PWD.equals(credPwd))
            throw new IllegalArgumentException("Credenziali predefinite errate.");
        if (sistema.usernameOccupato(nuovoUsr))
            throw new IllegalArgumentException("Username già in uso: " + nuovoUsr);
        Configuratore c = new Configuratore(nuovoUsr, nuovaPwd);
        c.cambiaPassword(nuovaPwd); // segna come non primo accesso
        sistema.aggiungiConfiguratore(c);
        Persistenza.salvaUtenti(sistema);
    }

    public Volontario loginVolontario(String nickname, String password) {
        Volontario v = sistema.trovaVolontario(nickname)
            .orElseThrow(() -> new IllegalArgumentException("Nickname non trovato."));
        if (!v.verificaPassword(password))
            throw new IllegalArgumentException("Password errata.");
        return v;
    }

    public void cambiaPasswordVolontario(Volontario v, String nuova) {
        v.cambiaPassword(nuova);
        Persistenza.salvaUtenti(sistema);
    }

    public Fruitore loginFruitore(String username, String password) {
        Fruitore f = sistema.trovaFruitore(username)
            .orElseThrow(() -> new IllegalArgumentException("Username non trovato."));
        if (!f.verificaPassword(password))
            throw new IllegalArgumentException("Password errata.");
        return f;
    }

    public void registraFruitore(String username, String password) {
        if (sistema.usernameOccupato(username))
            throw new IllegalArgumentException("Username già in uso: " + username);
        sistema.aggiungiFruitore(new Fruitore(username, password));
        Persistenza.salvaUtenti(sistema);
    }

    
    // INIZIALIZZAZIONE 

    public void inizializza(String ambito, int maxPersone) {
        sistema.setAmbito(ambito);
        sistema.setMaxPersone(maxPersone);
        LocalDate prossimo = LocalDate.now().plusMonths(1);
        sistema.setMeseRaccolta(prossimo.getYear(), prossimo.getMonthValue());
        Persistenza.salvaAmbito(sistema);
        Persistenza.salvaPiano(sistema);
    }

    public boolean ambitoImpostato()  { return sistema.ambitoImpostato(); }
    public String  getAmbito()        { return sistema.getAmbito(); }
    public int     getMaxPersone()    { return sistema.getMaxPersone(); }

    public void setMaxPersone(int max) {
        sistema.setMaxPersone(max);
        Persistenza.salvaAmbito(sistema);
    }

    
    // LUOGHI 
    

    /*
     * Crea un Luogo in memoria senza ancora salvarlo.
     * Il salvataggio avviene con salvaLuogo() dopo aver aggiunto i tipi.
     */
    public Luogo creaLuogo(String tag, String nome, String descr, String colloc) {
        if (sistema.trovaLuogo(tag).isPresent())
            throw new IllegalArgumentException("Tag luogo già in uso: " + tag);
        return new Luogo(tag, nome, descr, colloc);
    }

    public void salvaLuogo(Luogo l) {
        if (!l.haTipi())
            throw new IllegalStateException("Il luogo deve avere almeno un tipo di visita.");
        sistema.aggiungiLuogo(l);
        for (TipoVisita tv : l.getTipiVisita()) sistema.indiceTipoVisita(tv);
        Persistenza.salvaAmbito(sistema);
    }

    public Luogo ottieniLuogo(String tag) {
        return sistema.trovaLuogo(tag)
            .orElseThrow(() -> new IllegalArgumentException("Luogo non trovato: " + tag));
    }

    public void aggiungiTipoVisita(Luogo l, TipoVisita tv) {
        if (!tv.getVolontari().isEmpty() == false)
            throw new IllegalStateException("Il tipo di visita deve avere almeno un volontario.");
        l.aggiungiTipoVisita(tv);
        sistema.indiceTipoVisita(tv);
        Persistenza.salvaAmbito(sistema);
    }

    public Collection<Luogo> getLuoghi() { return sistema.getLuoghi(); }

    
    // TIPI DI VISITA 
    

    public Collection<TipoVisita> getTipiVisita()     { return sistema.getTipiVisita(); }

    public Optional<TipoVisita> trovaTipo(String tag) { return sistema.trovaTipo(tag); }

    
    // VOLONTARI 
    
    public Volontario creaVolontario(String nickname, String password) {
        if (sistema.usernameOccupato(nickname))
            throw new IllegalArgumentException("Nickname già in uso: " + nickname);
        Volontario v = new Volontario(nickname, password);
        sistema.aggiungiVolontario(v);
        Persistenza.salvaUtenti(sistema);
        return v;
    }

    public void collegaVolontario(String nickname, TipoVisita tv) {
        sistema.trovaVolontario(nickname)
            .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + nickname));
        tv.aggiungiVolontario(nickname);
        Persistenza.salvaAmbito(sistema);
    }

    public Collection<Volontario> getVolontari()    { return sistema.getVolontari(); }

    public List<TipoVisita> tipiDelVolontario(Volontario v) {
        return sistema.tipiDelVolontario(v.getNickname());
    }

    
    // RIMOZIONI CON CASCATA 
    

    private void verificaFasePiano() {
        if (sistema.getFase() != FaseOperativa.PIANO)
            throw new IllegalStateException(
                "Le modifiche ai dati si fanno solo dopo aver generato il piano.");
    }

    /*
     * Rimuove un luogo con cascata:
     *  1. per ogni tipo del luogo: rimuove il tipo dal luogo e dall'indice globale
     *  2. se un volontario resta senza tipi -> rimosso
     */
    public void rimuoviLuogo(String tagLuogo) {
        verificaFasePiano();
        Luogo luogo = ottieniLuogo(tagLuogo);
        for (TipoVisita tv : new ArrayList<>(luogo.getTipiVisita())) {
            sistema.rimuoviIndiceTipo(tv.getTag());
        }
        sistema.rimuoviLuogo(tagLuogo);
        rimuoviVolontariSenzaTipi();
        Persistenza.salvaAmbito(sistema);
        Persistenza.salvaUtenti(sistema);
    }

    /*
     * Rimuove un tipo di visita con cascata:
     *  1. lo rimuove dal luogo e dall'indice
     *  2. se il luogo rimane vuoto -> rimosso
     *  3. se un volontario resta senza tipi -> rimosso
     */
    public void rimuoviTipoVisita(String tagLuogo, String tagTipo) {
        verificaFasePiano();
        Luogo luogo = ottieniLuogo(tagLuogo);
        luogo.rimuoviTipoVisita(tagTipo);
        sistema.rimuoviIndiceTipo(tagTipo);
        if (!luogo.haTipi()) sistema.rimuoviLuogo(tagLuogo);
        rimuoviVolontariSenzaTipi();
        Persistenza.salvaAmbito(sistema);
        Persistenza.salvaUtenti(sistema);
    }

    /*
     * Rimuove un volontario con cascata:
     *  1. lo rimuove da ogni tipo di visita
     *  2. se un tipo resta senza volontari -> rimosso
     *  3. se un luogo resta senza tipi -> rimosso
     */
    public void rimuoviVolontario(String nickname) {
        verificaFasePiano();
        for (TipoVisita tv : sistema.getTipiVisita()) tv.rimuoviVolontario(nickname);
        rimuoviTipiSenzaVolontari();
        rimuoviLuoghiSenzaTipi();
        sistema.rimuoviVolontario(nickname);
        Persistenza.salvaAmbito(sistema);
        Persistenza.salvaUtenti(sistema);
    }

    private void rimuoviVolontariSenzaTipi() {
        List<String> daRimuovere = sistema.getVolontari().stream()
            .filter(v -> sistema.tipiDelVolontario(v.getNickname()).isEmpty())
            .map(Volontario::getNickname)
            .collect(Collectors.toList());
        daRimuovere.forEach(sistema::rimuoviVolontario);
    }

    private void rimuoviTipiSenzaVolontari() {
        List<TipoVisita> daRimuovere = sistema.getTipiVisita().stream()
            .filter(tv -> tv.getVolontari().isEmpty())
            .collect(Collectors.toList());
        for (TipoVisita tv : daRimuovere) {
            sistema.rimuoviIndiceTipo(tv.getTag());
            sistema.trovaLuogo(tv.getLuogoTag())
                .ifPresent(l -> l.rimuoviTipoVisita(tv.getTag()));
        }
    }

    private void rimuoviLuoghiSenzaTipi() {
        List<String> daRimuovere = sistema.getLuoghi().stream()
            .filter(l -> !l.haTipi())
            .map(Luogo::getTag)
            .collect(Collectors.toList());
        daRimuovere.forEach(sistema::rimuoviLuogo);
    }

    
    // CICLO MENSILE 
    

    public FaseOperativa getFase()        { return sistema.getFase(); }
    public int getAnnoRaccolta()          { return sistema.getAnnoRaccolta(); }
    public int getMeseRaccolta()          { return sistema.getMeseRaccolta(); }

    // passo 1: chiude la raccolta disponibilità
    public void chiudiRaccolta() {
        if (sistema.getFase() != FaseOperativa.RACCOLTA)
            throw new IllegalStateException("Non siamo in fase di raccolta.");
        sistema.setFase(FaseOperativa.CHIUSURA);
        Persistenza.salvaPiano(sistema);
    }

    // passo 2: genera il piano visite per il mese di raccolta
    public List<Visita> generaPiano() {
        if (sistema.getFase() != FaseOperativa.CHIUSURA)
            throw new IllegalStateException("Prima chiudi la raccolta disponibilità.");
        int anno = sistema.getAnnoRaccolta();
        int mese = sistema.getMeseRaccolta();
        List<Visita> nuove = Pianificatore.genera(sistema, anno, mese);
        nuove.forEach(sistema::aggiungiVisita);
        // le disponibilità usate si possono dimenticare
        sistema.getVolontari().forEach(v -> v.cancellaDisponibilita(anno, mese));
        sistema.cancellaDatePrecluse(anno, mese);
        sistema.setFase(FaseOperativa.PIANO);
        Persistenza.salvaPiano(sistema);
        return nuove;
    }

    // passo 3 (opzionale): modifiche dati — gestite dai metodi aggiungi/rimuovi
    // che verificano internamente che la fase sia PIANO

    // passo 4: apre la nuova raccolta per il mese successivo
    public void apriNuovaRaccolta() {
        if (sistema.getFase() != FaseOperativa.PIANO)
            throw new IllegalStateException("Prima genera il piano.");
        LocalDate att = LocalDate.of(sistema.getAnnoRaccolta(), sistema.getMeseRaccolta(), 1);
        LocalDate prox = att.plusMonths(1);
        sistema.setMeseRaccolta(prox.getYear(), prox.getMonthValue());
        sistema.setFase(FaseOperativa.RACCOLTA);
        Persistenza.salvaPiano(sistema);
    }

    
    // DISPONIBILITÀ 
    

    public void aggiungiDisponibilita(Volontario v, LocalDate data) {
        if (sistema.getFase() != FaseOperativa.RACCOLTA)
            throw new IllegalStateException("Non è il momento di dichiarare disponibilità.");
        if (data.getYear() != sistema.getAnnoRaccolta()
                || data.getMonthValue() != sistema.getMeseRaccolta())
            throw new IllegalArgumentException(
                "Puoi dichiarare disponibilità solo per il mese "
                + sistema.getMeseRaccolta() + "/" + sistema.getAnnoRaccolta() + ".");
        if (sistema.isPreclusa(data))
            throw new IllegalArgumentException("Il " + data + " è precluso a ogni visita.");
        GiornoSettimana gds = GiornoSettimana.da(data.getDayOfWeek());
        boolean haTipi = sistema.tipiDelVolontario(v.getNickname())
            .stream().anyMatch(tv -> tv.attivaNelGiorno(gds) && tv.nelPeriodo(data));
        if (!haTipi)
            throw new IllegalArgumentException(
                "Nessun tuo tipo di visita è programmabile il " + gds + ".");
        v.aggiungiDisponibilita(data);
        Persistenza.salvaPiano(sistema);
    }

    public void rimuoviDisponibilita(Volontario v, LocalDate data) {
        v.rimuoviDisponibilita(data);
        Persistenza.salvaPiano(sistema);
    }

    public Set<LocalDate> getDisponibilita(Volontario v) {
        return v.getDisponibilita(sistema.getAnnoRaccolta(), sistema.getMeseRaccolta());
    }

    
    // DATE PRECLUSE 
    

    public void aggiungiDataPreclusa(LocalDate d) {
        sistema.aggiungiDataPreclusa(d);
        Persistenza.salvaPiano(sistema);
    }

    public Set<LocalDate> getDatePrecluse(int anno, int mese) {
        return sistema.getDatePrecluse(anno, mese);
    }
 public Set<LocalDate> getallDatePrecluse() {
        return sistema.getAllDatePrecluse();
    }
    
    // VISITE 
    

    public List<Visita> getVisitePerStato(StatoVisita s) {
        return s == StatoVisita.EFFETTUATA
            ? sistema.getArchivio()
            : sistema.getVisitePerStato(s);
    }

    // visite confermate in cui questo volontario è la guida (V2+)
    public List<Visita> getVisiteConfermate(Volontario v) {
        return sistema.getVisitePerStato(StatoVisita.CONFERMATA).stream()
            .filter(vis -> vis.getGuidaNickname().equals(v.getNickname()))
            .collect(Collectors.toList());
    }

    // tutte le visite visibili al fruitore, ordinate per data (V4)
    public List<Visita> getVisiteFruitore() {
        List<Visita> out = new ArrayList<>();
        out.addAll(sistema.getVisitePerStato(StatoVisita.PROPOSTA));
        out.addAll(sistema.getVisitePerStato(StatoVisita.CONFERMATA));
        out.addAll(sistema.getVisitePerStato(StatoVisita.CANCELLATA));
        out.sort(Comparator.comparing(Visita::getData));
        return out;
    }

    public List<Visita> getVisiteProposte() {
        return sistema.getVisitePerStato(StatoVisita.PROPOSTA).stream()
            .sorted(Comparator.comparing(Visita::getData))
            .collect(Collectors.toList());
    }

    // visite a cui il fruitore si è iscritto (V4)
    public List<Visita> getMieIscrizioni(Fruitore f) {
        return getVisiteFruitore().stream()
            .filter(v -> v.getIscrizioni().stream()
                .anyMatch(i -> i.getUsernameFruitore().equalsIgnoreCase(f.getUsername())))
            .collect(Collectors.toList());
    }

    
    // ISCRIZIONI FRUITORE 
    

    public String iscriviAVisita(Fruitore f, Visita visita, int persone) {
        int maxConsentito = sistema.getMaxPersone();
        if (persone < 1 || persone > maxConsentito)
            throw new IllegalArgumentException(
                "Il numero di persone deve essere tra 1 e " + maxConsentito + ".");
        if (visita.getStato() != StatoVisita.PROPOSTA)
            throw new IllegalStateException("La visita non è aperta alle iscrizioni.");
        TipoVisita tv = sistema.trovaTipo(visita.getTipoTag())
            .orElseThrow(() -> new IllegalStateException("Tipo di visita non trovato."));
        Iscrizione i = new Iscrizione(f.getUsername(), persone);
        visita.aggiungiIscrizione(i, tv.getMax());
        Persistenza.salvaPrenotazioni(sistema);
        Persistenza.salvaPiano(sistema);
        return i.getCodice();
    }

    public void disdiciIscrizione(Fruitore f, Visita visita, String codice) {
        if (visita.getStato() != StatoVisita.PROPOSTA
                && visita.getStato() != StatoVisita.COMPLETA)
            throw new IllegalStateException(
                "Non puoi disdire: visita già " + visita.getStato() + ".");
        Iscrizione trovata = visita.cercaIscrizione(codice)
            .orElseThrow(() -> new IllegalArgumentException("Codice non trovato."));
        if (!trovata.getUsernameFruitore().equalsIgnoreCase(f.getUsername()))
            throw new IllegalArgumentException("Questo codice non appartiene al tuo account.");
        visita.rimuoviIscrizione(codice);
        Persistenza.salvaPrenotazioni(sistema);
        Persistenza.salvaPiano(sistema);
    }

    public Collection<Fruitore> getFruitori() { return sistema.getFruitori(); }

    // accesso al sistema (usato dalla UI per ricerche puntuali)
    public Sistema getSistema() { return sistema; }
}
