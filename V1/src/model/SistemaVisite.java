package model;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Rappresenta il corpo dati principale del sistema. E la classe Radice del modello:
 * contiene configuratori, volontari, luoghi e visite.
 * Caricata dai file JSON all'avvio e salvata dopo ogni modifica.
 *
 * Invariante: tutte le liste/mappe non sono null (es. configuratori != null, volontari != null, ...), 
 *             (maxPersonePerIscrizione>=1), (ambitoTerritoriale != null dopo l'inizializzazione)
 */
public class SistemaVisite {

    // credenziali predefinite per il "primo" configuratore
    public static final String CRED_USR = "admin";
    public static final String CRED_PWD = "admin123";

    private String ambitoTerritoriale;    // impostato una sola volta
    private int maxPersonePerIscrizione; // modificabile

    private final Map<String, Luogo>          luoghi         = new LinkedHashMap<>();
    private final Map<String, TipoVisita>     tipiVisita     = new LinkedHashMap<>();
    private final Map<String, Configuratore>  configuratori  = new LinkedHashMap<>();
    private final Map<String, Volontario>     volontari      = new LinkedHashMap<>();
    private final Map<String, Fruitore>       fruitori       = new LinkedHashMap<>();

    // visite correnti (proposte / complete / confermate / cancellate)
    private final List<Visita> visite   = new ArrayList<>();
    // visite effettuate
    private final List<Visita> archivio = new ArrayList<>();

    // date precluse per mese: "YYYY-MM" -> liste di date
    private final Map<String, Set<LocalDate>> datePrecluse = new HashMap<>();
    public SistemaVisite() {
        this.maxPersonePerIscrizione = 1; // default conservativo
    }

    // === AMBITO TERRITORIALE ===
/** imposta l'ambito territoriale. Può essere chiamato una sola volta.
 * @pre ambito != null && !ambito.isBlank()
 * @pre ambitoTerritoriale == null (non ancora inizializzato)
 */
    public void setAmbito(String a) {
        if (ambitoTerritoriale != null)
            throw new IllegalStateException("Ambito già impostato.");
        this.ambitoTerritoriale = a.trim();
    }

    public String getAmbito()           { return ambitoTerritoriale; }
    public boolean ambitoImpostato()    { return ambitoTerritoriale != null; }
    public void setAmbitoForza(String a){ this.ambitoTerritoriale = a; } // usato dalla persistenza

    // === MAX PERSONE ===
/**
 * @pre max >= 1
 */
    public void setMaxPersone(int max)    { this.maxPersonePerIscrizione = max; }
    public int getMaxPersone()          { return maxPersonePerIscrizione; }

    // === LUOGHI ===
/**
 * Aggiunge un luogo al sistema.
 * @pre l != null
 * @pre il tag del luogo deve essere univoco
 */
    public void aggiungiLuogo(Luogo l) {
        if (luoghi.containsKey(l.getTag()))
            throw new IllegalArgumentException("Luogo già presente: " + l.getTag());
        luoghi.put(l.getTag(), l);
    }

    public Optional<Luogo> trovaLuogo(String tag) { return Optional.ofNullable(luoghi.get(tag)); }
    public boolean rimuoviLuogo(String tag)        { return luoghi.remove(tag) != null; }
    public Collection<Luogo> getLuoghi()           { return Collections.unmodifiableCollection(luoghi.values()); }

    // === tipi di visita (indice globale per accesso rapido) ===

    public void indiceTipoVisita(TipoVisita tv)     { tipiVisita.put(tv.getTag(), tv); }
    public void rimuoviIndiceTipo(String tag)        { tipiVisita.remove(tag); }
    public Optional<TipoVisita> trovaTipo(String tag){ return Optional.ofNullable(tipiVisita.get(tag)); }
    public Collection<TipoVisita> getTipiVisita()   { return Collections.unmodifiableCollection(tipiVisita.values()); }

    // restituisce tutti i tipi associati a un volontario
    public List<TipoVisita> tipiDelVolontario(String nickname) {
        return tipiVisita.values().stream()
            .filter(tv -> tv.getVolontari().contains(nickname))
            .collect(Collectors.toList());
    }

    // === CONFIGURATORI ===

/** Registra un nuovo configuratore.
 * @pre username univoco tra volontari e configuratori
 */
    public void aggiungiConfiguratore(Configuratore c) {
        if (usernameOccupato(c.getUsername()))
            throw new IllegalArgumentException("Username già in uso: " + c.getUsername());
        configuratori.put(c.getUsername(), c);
    }

    public Optional<Configuratore> trovaConfiguratore(String u) {
        return Optional.ofNullable(configuratori.get(u));
    }

    public Collection<Configuratore> getConfiguratori() {
        return Collections.unmodifiableCollection(configuratori.values());
    }

    // === VOLONTARI ===

/** Aggiunge un volontario al sistema.
 * @pre nickname univoco tra vollontari e configuratori
 */
    public void aggiungiVolontario(Volontario v) {
        if (usernameOccupato(v.getNickname()))
            throw new IllegalArgumentException("Nickname già in uso: " + v.getNickname());
        volontari.put(v.getNickname(), v);
    }

    public Optional<Volontario> trovaVolontario(String n) {
        return Optional.ofNullable(volontari.get(n));
    }

    public boolean rimuoviVolontario(String n) { return volontari.remove(n) != null; }

    public Collection<Volontario> getVolontari() {
        return Collections.unmodifiableCollection(volontari.values());
    }

    // === VISITE ===

    public void aggiungiVisita(Visita v)  { visite.add(v); }
    public List<Visita> getVisite()       { return Collections.unmodifiableList(visite); }
    public List<Visita> getArchivio()     { return Collections.unmodifiableList(archivio); }

    public List<Visita> getVisitePerStato(StatoVisita s) {
        return visite.stream().filter(v -> v.getStato() == s).collect(Collectors.toList());
    }

    /*
     * Aggiorna stati in base alla data corrente/odierna.
     * Chiamato ad ogni avvio per tenere il sistema coerente.
     */
    public void aggiornaStati(LocalDate oggi) {
        List<Visita> daRimuovere = new ArrayList<>();
        for (Visita v : visite) {
            LocalDate scadenza = v.getData().minusDays(3);
            if (!oggi.isBefore(scadenza) &&
                (v.getStato() == StatoVisita.PROPOSTA || v.getStato() == StatoVisita.COMPLETA)) {
                TipoVisita tv = tipiVisita.get(v.getTipoTag());
                int min = tv != null ? tv.getMin() : 1;
                v.chiudiIscrizioni(min);
            }
            if (oggi.isAfter(v.getData())) {
                if (v.getStato() == StatoVisita.CONFERMATA) {
                    v.segnaEffettuata();
                    archivio.add(v);
                    daRimuovere.add(v);
                } else if (v.getStato() == StatoVisita.CANCELLATA) {
                    daRimuovere.add(v);
                }
            }
        }
        visite.removeAll(daRimuovere);
    }

    // === DATE PRECLUSE===

    public void aggiungiDataPreclusa(LocalDate d) {
        datePrecluse.computeIfAbsent(chiaveMese(d), k -> new HashSet<>()).add(d);
    }

    public boolean isPreclusa(LocalDate d) {
        Set<LocalDate> s = datePrecluse.get(chiaveMese(d));
        return s != null && s.contains(d);
    }

    public Set<LocalDate> getDatePrecluse(int anno, int mese) {
        return Collections.unmodifiableSet(
            datePrecluse.getOrDefault(anno + "-" + String.format("%02d", mese),
                Collections.emptySet()));
    }

    public void cancellaDatePrecluse(int anno, int mese) {
        datePrecluse.remove(anno + "-" + String.format("%02d", mese));
    }

    // === UTILITY ===

/** verifica se uno username e gia in uso tra configuratori e volontari
 * usato per validare l'aggiunta di nuovi utenti e per l'autenticazione
*/
    public boolean usernameOccupato(String u) {
        return configuratori.containsKey(u)
            || volontari.containsKey(u)
            || fruitori.containsKey(u);
    }

    // usato dalla persistenza per caricare lo storico
    public void aggiungiVisitaStorico(Visita v) { archivio.add(v); }

    private static String chiaveMese(LocalDate d) {
        return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
    }
}
// non c'è un metodo diretto; lo aggiungiamo qui sotto la classe
