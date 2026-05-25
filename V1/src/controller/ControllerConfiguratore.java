package controller;

import model.*;
import storage.Persistenza;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Unico controller dell'applicazione.
 * Tutta la logica applicativa passa da qui; la UI non tocca il modello direttamente.
 * Fa da intermediario tra la UI e il modello, garantendo i vincoli di dominio.
 *
 * Invariante: sistema != null
 */
public class ControllerConfiguratore {

    private final Sistema sistema;

    public ControllerConfiguratore(Sistema sistema) {
        this.sistema = sistema;
    }

    // ======================== LOGIN / REGISTRAZIONE CONFIGURATORE ========================
    /**
     * Autentica un configuratore.
     * @pre username != null && password != null
     * @return il Configuratore autenticato
     * @throws IllegalArgumentException se le credenziali non sono valide
     */

    public Configuratore loginConfiguratore(String username, String password) {
        
        Configuratore c = sistema.trovaConfiguratore(username)
            .orElseThrow(() -> new IllegalArgumentException("Username non trovato."));
        
        if (!c.verificaPassword(password))
            throw new IllegalArgumentException("Password errata.");
        return c;
    }

    /**
     * Cambia la password di un configuratore.
     * @pre configuratore != null && nuovaPassword non vuota
     * @post configuratore.isPasswordCambiata()
     */
    public void cambiaPasswordConfiguratore(Configuratore c, String nuova) {
        
        if (c == null || nuova == null || nuova.isEmpty())
            throw new IllegalArgumentException("Configuratore e nuova password non possono essere vuoti.");
        c.cambiaPassword(nuova);
        salva();
    }
    /**
     * Registra un nuovo configuratore.
     * @pre username e password non vuoti
     * @pre username non già in uso
     */
    public void registraConfiguratore(String credUsr, String credPwd, String nuovoUsr, String nuovaPwd) {
        
        if (!Sistema.CRED_USR.equals(credUsr) || !Sistema.CRED_PWD.equals(credPwd))
            throw new IllegalArgumentException("Credenziali predefinite errate.");
        if (sistema.usernameOccupato(nuovoUsr))
            throw new IllegalArgumentException("Username già in uso: " + nuovoUsr);
        
        Configuratore c = new Configuratore(nuovoUsr, nuovaPwd);
        c.cambiaPassword(nuovaPwd); // segna come non primo accesso
        sistema.aggiungiConfiguratore(c);
        salva();
    }

    // ============================== INIZIALIZZAZIONE SISTEMA ==============================

    /**
     * inizializza il sistema con i dati di base: ambito, max persone per visita, mese di raccolta
     * @pre ambito non vuoto, maxPersone >= 1
     * @param ambito
     * @param maxPersone
     */

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

    // ================== GESTIONE LUOGHI  ==================

    /*
     * Crea un Luogo in memoria senza ancora salvarlo.
     * Il salvataggio avviene con salvaLuogo() dopo aver aggiunto i tipi.
     */
    public Luogo creaLuogo(String tag, String nome, String descr, String colloc) {
        if (sistema.trovaLuogo(tag).isPresent())
            throw new IllegalArgumentException("Tag luogo già in uso: " + tag);
        return new Luogo(tag, nome, descr, colloc);
    }

/**    
 * Salva un luogo già creato e popolato di tipi di visita.
 * @pre l != null && l.haTipi()
 */
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

// ====================== TIPI DI VISITA  ======================
/** 
 * Aggiunge un tipo di visita a un luogo. il tipo deve avere almeno un volontario.
 * @pre l != null, tv != null
 * @param l
 * @param tv
 */
    public void aggiungiTipoVisita(Luogo l, TipoVisita tv) {
        if (!tv.getVolontari().isEmpty() == false)
            throw new IllegalStateException("Il tipo di visita deve avere almeno un volontario.");
        l.aggiungiTipoVisita(tv);
        sistema.indiceTipoVisita(tv);
        salva();
    }

    public Collection<Luogo> getLuoghi() { return sistema.getLuoghi(); }


    public Collection<TipoVisita> getTipiVisita()     { return sistema.getTipiVisita(); }

    public Optional<TipoVisita> trovaTipo(String tag) { return sistema.trovaTipo(tag); }
  
// ========================= GESTIONE VOLONTARI =========================

/**
 * Crea un nuovo volontario e lo aggiunge al sistema.
 * @pre nickname e password non vuoti
 */
  public Volontario creaVolontario(String nickname, String password) {
        if (sistema.usernameOccupato(nickname))
            throw new IllegalArgumentException("Nickname già in uso: " + nickname);
        Volontario v = new Volontario(nickname, password);
        sistema.aggiungiVolontario(v);
        Persistenza.salvaUtenti(sistema);
        return v;
    }

/**
 * Autentica un volontario.
 * @param nickname
 * @param password
 * @return
 */
    public Volontario loginVolontario(String nickname, String password) {
        Volontario v = sistema.trovaVolontario(nickname)
            .orElseThrow(() -> new IllegalArgumentException("Nickname non trovato."));
        if (!v.verificaPassword(password))
            throw new IllegalArgumentException("Password errata.");
        return v;
    }

    public void cambiaPasswordVolontario(Volontario v, String nuova) {
        v.cambiaPassword(nuova);
        salva();
    }
    public void collegaVolontario(String nickname, TipoVisita tv) {
        sistema.trovaVolontario(nickname)
            .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + nickname));
        tv.aggiungiVolontario(nickname);
        salva();
    }

    public Collection<Volontario> getVolontari()    { return sistema.getVolontari(); }

    public List<TipoVisita> tipiDelVolontario(Volontario v) {
        return sistema.tipiDelVolontario(v.getNickname());
    }

    // ====================== DATE PRECLUSE ======================
/**
 * Aggiunge una data preclusa al sistema. Le visite non possono essere programmate in queste date.
 */
    public void aggiungiDataPreclusa(LocalDate d) {
        sistema.aggiungiDataPreclusa(d);
        Persistenza.salvaPiano(sistema);
    }

    public Set<LocalDate> getDatePrecluse(int anno, int mese) {
        return sistema.getDatePrecluse(anno, mese);
    }

    // ================ VISITE ================

/**
 * Restituisce la lista delle visite filtrate per stato. Se lo stato è EFFETTUATA, restituisce l'archivio storico.  
 */
    public List<Visita> getVisitePerStato(StatoVisita s) {
        return s == StatoVisita.EFFETTUATA
            ? sistema.getArchivio()
            : sistema.getVisitePerStato(s);
    }

    // ================ UTILITY ================
    
    private void salva() {
        Persistenza.salvaUtenti(sistema);
        Persistenza.salvaAmbito(sistema);
        Persistenza.salvaPiano(sistema);
    }
}
    