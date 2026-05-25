package model;

import java.time.LocalDate;
import java.util.*;

// guida volontaria accreditata dall'organizzazione
public class Volontario {

    private final String nickname;    // funge anche da username
    private String passwordHash;
    private boolean primoAccesso;

    // disponibilità dichiarate: chiave "YYYY-MM" -> insieme di date
    private final Map<String, Set<LocalDate>> disponibilita = new HashMap<>();

    public Volontario(String nickname, String password) {
        this.nickname = nickname;
        this.passwordHash = hash(password);
        this.primoAccesso = true;
    }

    public Volontario(String nickname, String passwordHash, boolean primoAccesso) {
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.primoAccesso = primoAccesso;
    }

    // ---- autenticazione ----

    public boolean verificaPassword(String tentativo) {
        return passwordHash.equals(hash(tentativo));
    }

    public void cambiaPassword(String nuova) {
        this.passwordHash = hash(nuova);
        this.primoAccesso = false;
    }

    // ---- disponibilità ----

    public void aggiungiDisponibilita(LocalDate data) {
        disponibilita.computeIfAbsent(chiave(data), k -> new HashSet<>()).add(data);
    }

    public void rimuoviDisponibilita(LocalDate data) {
        Set<LocalDate> s = disponibilita.get(chiave(data));
        if (s != null) s.remove(data);
    }

    public boolean isDisponibileIn(LocalDate data) {
        Set<LocalDate> s = disponibilita.get(chiave(data));
        return s != null && s.contains(data);
    }

    public Set<LocalDate> getDisponibilita(int anno, int mese) {
        return Collections.unmodifiableSet(
            disponibilita.getOrDefault(anno + "-" + String.format("%02d", mese),
                Collections.emptySet()));
    }

    public void cancellaDisponibilita(int anno, int mese) {
        disponibilita.remove(anno + "-" + String.format("%02d", mese));
    }

    // ---- util ----

    private static String chiave(LocalDate d) {
        return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
    }

    private static String hash(String s) { return Integer.toHexString(s.hashCode()); }

    public String getNickname()      { return nickname; }
    public String getPasswordHash()  { return passwordHash; }
    public boolean isPrimoAccesso()  { return primoAccesso; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Volontario v && nickname.equalsIgnoreCase(v.nickname);
    }

    @Override
    public int hashCode() { return nickname.toLowerCase().hashCode(); }
}
