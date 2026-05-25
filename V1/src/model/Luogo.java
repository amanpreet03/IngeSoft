package model;

import java.util.*;

/*
 * Rappresenta un luogo visitabile nell'ambito territoriale.
 * L'organizzazione verifica prima l'assenza di barriere architettoniche/cantieri.
 * Invariante: tag != null, nome != null, tipiVisita != null
 *           tipiVisita può essere vuota solo temporaneamente durante la costruzione del luogo
 */

public class Luogo {

    private final String tag;         // es. "palazzo_ducale"
    private final String nome;
    private final String descrizione; // una breve descrizione del luogo puramente informativo e opzionale
    private final String collocazione; // indirizzo 
    private final List<TipoVisita> tipiVisita = new ArrayList<>();

    public Luogo(String tag, String nome, String descrizione, String collocazione) {
        this.tag = tag;
        this.nome = nome;
        this.descrizione = descrizione;
        this.collocazione = collocazione;
    }

    /*
     * Aggiunge un tipo di visita verificando unicità del tag e assenza di sovrapposizioni orarie.
     * @pre:  tv != null
     * @post: tipiVisita contiene tv
     */
    public void aggiungiTipoVisita(TipoVisita tv) {
        for (TipoVisita t : tipiVisita) {
            if (t.getTag().equals(tv.getTag()))
                throw new IllegalArgumentException("Tag già presente: " + tv.getTag());
            if (t.sovrappone(tv))
                throw new IllegalArgumentException(
                    "È in sovrapposizione oraria con '" + t.getTitolo() + "'");
        }
        tipiVisita.add(tv);
    }

    public boolean rimuoviTipoVisita(String tagTV) {
        return tipiVisita.removeIf(t -> t.getTag().equals(tagTV));
    }

    public Optional<TipoVisita> trovaTipo(String tagTV) {
        return tipiVisita.stream().filter(t -> t.getTag().equals(tagTV)).findFirst();
    }

    public boolean haTipi() { return !tipiVisita.isEmpty(); }

    // ---- getter ----
    public String getTag()          { return tag; }
    public String getNome()         { return nome; }
    public String getDescrizione()  { return descrizione; }
    public String getCollocazione() { return collocazione; }
    public List<TipoVisita> getTipiVisita() { return Collections.unmodifiableList(tipiVisita); }

    @Override
    public String toString() { return "[" + tag + "] " + nome; }
}
