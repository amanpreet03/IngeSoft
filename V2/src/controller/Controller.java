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

    // LOGIN / REGISTRAZIONE

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

    // DISPONIBILITÀ 

    public void aggiungiDisponibilita(Volontario v, LocalDate data) {
    
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
    //  VISITE 

    public List<Visita> getVisitePerStato(StatoVisita s) {
        return s == StatoVisita.EFFETTUATA
            ? sistema.getArchivio()
            : sistema.getVisitePerStato(s);
    }

    // visite confermate in cui questo volontario è la guida 
    public List<Visita> getVisiteConfermate(Volontario v) {
        return sistema.getVisitePerStato(StatoVisita.CONFERMATA).stream()
            .filter(vis -> vis.getGuidaNickname().equals(v.getNickname()))
            .collect(Collectors.toList());
    }

    public List<Visita> getVisiteProposte() {
        return sistema.getVisitePerStato(StatoVisita.PROPOSTA).stream()
            .sorted(Comparator.comparing(Visita::getData))
            .collect(Collectors.toList());
    }
    // accesso al sistema (usato dalla UI per ricerche puntuali)
    public Sistema getSistema() { return sistema; }
}
