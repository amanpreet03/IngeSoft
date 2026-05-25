package storage;

import model.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
 * Facade per tutta la persistenza JSON.
 * Ogni metodo pubblico carica un file, aggiorna il Sistema in memoria
 * oppure serializza lo stato corrente su disco.
 *
 * Formato date nei JSON: dd-MM-yyyy
 * Formato mesi nei JSON: YYYY-MM  (es. "2026-07")
 */
public class Persistenza {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FMT_MD = DateTimeFormatter.ofPattern("dd-MM");

    private Persistenza() {}

    
    // Caricamento all'avvio
    

    public static void caricaTutto(Sistema s) {
        caricaAmbito(s);
        caricaUtenti(s);
        caricaPiano(s);
        caricaStorico(s);
        caricaPrenotazioni(s);
    }

    // ---- ambito.json ----

    public static void caricaAmbito(Sistema s) {
        Map<String, Object> root = JsonIO.leggi(Percorsi.AMBITO);
        if (root.isEmpty()) return;

        String nome = JsonIO.strSafe(root.get("nome"));
        if (!nome.isEmpty()) s.setAmbitoForza(nome);
        s.setMaxPersone(JsonIO.intSafe(root.get("max-prenotazione")));

        // luoghi
        Map<String, Object> luoghi = JsonIO.oggettoSafe(root.get("luoghi"));
        for (Map.Entry<String, Object> el : luoghi.entrySet()) {
            Map<String, Object> ld = JsonIO.oggettoSafe(el.getValue());
            Luogo luogo = new Luogo(
                el.getKey(),
                JsonIO.strSafe(ld.get("nome")),
                JsonIO.strSafe(ld.get("descrizione")),
                JsonIO.strSafe(ld.get("collocazione"))
            );
            try { s.aggiungiLuogo(luogo); } catch (Exception ignored) {}
        }

        // tipi di visita
        Map<String, Object> tipi = JsonIO.oggettoSafe(root.get("tipi-visita"));
        for (Map.Entry<String, Object> et : tipi.entrySet()) {
            TipoVisita tv = parseTipoVisita(et.getKey(), JsonIO.oggettoSafe(et.getValue()));
            if (tv == null) continue;
            try {
                s.indiceTipoVisita(tv);
                s.trovaLuogo(tv.getLuogoTag()).ifPresent(l -> {
                    try { l.aggiungiTipoVisita(tv); } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }
    }

    private static TipoVisita parseTipoVisita(String tag, Map<String, Object> d) {
        try {
            String luogoTag     = JsonIO.strSafe(d.get("luogo"));
            String titolo       = JsonIO.strSafe(d.get("titolo"));
            String descr        = JsonIO.strSafe(d.get("descrizione"));
            String puntoInc     = JsonIO.strSafe(d.get("punto-incontro"));
            String diStr        = JsonIO.strSafe(d.get("data-inizio")); // "dd-MM"
            String dfStr        = JsonIO.strSafe(d.get("data-fine"));
            MonthDay di = MonthDay.parse("--" + diStr.replace("-", "-"), DateTimeFormatter.ofPattern("--dd-MM"));
            MonthDay df = MonthDay.parse("--" + dfStr.replace("-", "-"), DateTimeFormatter.ofPattern("--dd-MM"));
            Set<GiornoSettimana> giorni = new LinkedHashSet<>();
            for (Object g : JsonIO.listaSafe(d.get("giorni")))
                giorni.add(GiornoSettimana.daTag(JsonIO.strSafe(g)));
            LocalTime ora = LocalTime.parse(JsonIO.strSafe(d.get("ora-inizio")));
            int durata    = JsonIO.intSafe(d.get("durata"));
            boolean bgl   = JsonIO.boolSafe(d.get("biglietto"));
            int minP      = JsonIO.intSafe(d.get("min-partecipanti"));
            int maxP      = JsonIO.intSafe(d.get("max-partecipanti"));
            List<String> vol = new ArrayList<>();
            for (Object v : JsonIO.listaSafe(d.get("volontari"))) vol.add(JsonIO.strSafe(v));
            return new TipoVisita(tag, titolo, luogoTag, descr, puntoInc,
                di, df, giorni, ora, durata, bgl, minP, maxP, vol);
        } catch (Exception e) {
            System.err.println("[Persistenza] Errore parsing tipo " + tag + ": " + e.getMessage());
            return null;
        }
    }

    // ---- users.json ----

    public static void caricaUtenti(Sistema s) {
        Map<String, Object> root = JsonIO.leggi(Percorsi.UTENTI);
        for (Map.Entry<String, Object> e : root.entrySet()) {
            if (e.getKey().equals("primo_avvio")) continue;
            Map<String, Object> ud = JsonIO.oggettoSafe(e.getValue());
            int tipo = JsonIO.intSafe(ud.get("tipo"));
            String pwdHash = JsonIO.strSafe(ud.get("password-hash"));
            // se non c'è l'hash ma c'è la password in chiaro (JSON di demo), la usiamo come hash già calcolato
            if (pwdHash.isEmpty()) pwdHash = JsonIO.strSafe(ud.get("password"));
            boolean primo = JsonIO.boolSafe(ud.get("primo-accesso"));
            try {
                switch (tipo) {
                    case 1 -> s.aggiungiConfiguratore(new Configuratore(e.getKey(), pwdHash, primo));
                    case 2 -> s.aggiungiVolontario(new Volontario(e.getKey(), pwdHash, primo));
                    case 3 -> s.aggiungiFruitore(new Fruitore(e.getKey(), pwdHash, false));
                }
            } catch (Exception ignored) {}
        }
    }

    // ---- piano.json ----

    public static void caricaPiano(Sistema s) {
        Map<String, Object> root = JsonIO.leggi(Percorsi.PIANO);
        if (root.isEmpty()) return;

        s.setFase(FaseOperativa.daJson(JsonIO.strSafe(root.get("fase"))));
        s.setMeseRaccolta(JsonIO.intSafe(root.get("anno-raccolta")),
                          JsonIO.intSafe(root.get("mese-raccolta")));

        // date precluse
        for (Object d : JsonIO.listaSafe(root.get("date-precluse")))
            s.aggiungiDataPreclusa(LocalDate.parse(JsonIO.strSafe(d), FMT));

        // disponibilità volontari
        Map<String, Object> disp = JsonIO.oggettoSafe(root.get("disponibilita"));
        for (Map.Entry<String, Object> ed : disp.entrySet()) {
            s.trovaVolontario(ed.getKey()).ifPresent(v -> {
                for (Object d : JsonIO.listaSafe(ed.getValue()))
                    v.aggiungiDisponibilita(LocalDate.parse(JsonIO.strSafe(d), FMT));
            });
        }

        // visite pianificate correnti
        Map<String, Object> visite = JsonIO.oggettoSafe(root.get("visite"));
        for (Map.Entry<String, Object> ev : visite.entrySet()) {
            // chiave: "dd-MM-yyyy|tagTipo"
            String[] parti = ev.getKey().split("\\|");
            if (parti.length != 2) continue;
            LocalDate data = LocalDate.parse(parti[0], FMT);
            String tipoTag = parti[1];
            Map<String, Object> vd = JsonIO.oggettoSafe(ev.getValue());
            String guida   = JsonIO.strSafe(vd.get("guida"));
            String luogoTag = s.trovaTipo(tipoTag).map(TipoVisita::getLuogoTag).orElse("");
            Visita vis = new Visita(tipoTag, luogoTag, data, guida);
            vis.setStato(StatoVisita.daJson(JsonIO.strSafe(vd.get("stato"))));
            s.aggiungiVisita(vis);
        }
    }

    // ---- storico.json ----

    public static void caricaStorico(Sistema s) {
        Map<String, Object> root = JsonIO.leggi(Percorsi.STORICO);
        for (Map.Entry<String, Object> e : root.entrySet()) {
            // chiave: "dd-MM-yyyy|tagTipo"
            String[] parti = e.getKey().split("\\|");
            if (parti.length != 2) continue;
            LocalDate data = LocalDate.parse(parti[0], FMT);
            String tipoTag = parti[1];
            String luogoTag = JsonIO.strSafe(e.getValue());
            Visita vis = new Visita(tipoTag, luogoTag, data, "");
            vis.setStato(StatoVisita.EFFETTUATA);
            // le visite storiche vanno nell'archivio — trick: usiamo getArchivio riflessione no,
            // usiamo aggiungiVisita poi segnaEffettuata
            // più semplice: getArchivio è unmodifiable, quindi usiamo un workaround
            // → aggiungiamo come CONFERMATA poi segnaEffettuata
            vis.segnaEffettuata();
            // non possiamo aggiungerla all'archivio direttamente (è unmodifiable)
            // il Sistema espone aggiornaStati che la sposta; qui usiamo un metodo dedicato
            s.aggiungiVisitaStorico(vis);
        }
    }

    // ---- prenotazioni.json ----

    public static void caricaPrenotazioni(Sistema s) {
        Map<String, Object> root = JsonIO.leggi(Percorsi.PRENOTAZIONI);
        for (Map.Entry<String, Object> e : root.entrySet()) {
            String codice = e.getKey();
            Map<String, Object> pd = JsonIO.oggettoSafe(e.getValue());
            String username = JsonIO.strSafe(pd.get("username"));
            LocalDate data  = LocalDate.parse(JsonIO.strSafe(pd.get("giorno")), FMT);
            String tipoTag  = JsonIO.strSafe(pd.get("tipo"));
            int persone     = JsonIO.intSafe(pd.get("persone"));
            // trova la visita corrispondente e aggiungi l'iscrizione
            s.getVisite().stream()
                .filter(v -> v.getData().equals(data) && v.getTipoTag().equals(tipoTag))
                .findFirst()
                .ifPresent(v -> {
                    int maxP = s.trovaTipo(tipoTag).map(TipoVisita::getMax).orElse(Integer.MAX_VALUE);
                    try { v.aggiungiIscrizione(new Iscrizione(codice, username, persone), maxP); }
                    catch (Exception ignored) {}
                });
        }
    }

    
    // Salvataggio
    

    public static void salvaAmbito(Sistema s) {
        Map<String, Object> root = JsonIO.nuovaMappa();
        root.put("nome", s.getAmbito() != null ? s.getAmbito() : "");
        root.put("max-prenotazione", (long) s.getMaxPersone());

        Map<String, Object> luoghi = JsonIO.nuovaMappa();
        for (Luogo l : s.getLuoghi()) {
            Map<String, Object> ld = JsonIO.nuovaMappa();
            ld.put("nome",         l.getNome());
            ld.put("descrizione",  l.getDescrizione() != null ? l.getDescrizione() : "");
            ld.put("collocazione", l.getCollocazione());
            luoghi.put(l.getTag(), ld);
        }
        root.put("luoghi", luoghi);

        Map<String, Object> tipi = JsonIO.nuovaMappa();
        for (TipoVisita tv : s.getTipiVisita()) {
            Map<String, Object> td = JsonIO.nuovaMappa();
            td.put("luogo",            tv.getLuogoTag());
            td.put("titolo",           tv.getTitolo());
            td.put("descrizione",      tv.getDescrizione());
            td.put("punto-incontro",   tv.getPuntoIncontro());
            td.put("data-inizio", tv.getInizioPeriodo().format(DateTimeFormatter.ofPattern("dd-MM")));
            td.put("data-fine",   tv.getFinePeriodo().format(DateTimeFormatter.ofPattern("dd-MM")));
            List<Object> gg = new ArrayList<>();
            tv.getGiorni().forEach(g -> gg.add(g.tag()));
            td.put("giorni",           gg);
            td.put("ora-inizio",       tv.getOraInizio().toString());
            td.put("durata",           (long) tv.getDurata());
            td.put("biglietto",        tv.vuoleBiglietto());
            td.put("min-partecipanti", (long) tv.getMin());
            td.put("max-partecipanti", (long) tv.getMax());
            td.put("volontari",        new ArrayList<>(tv.getVolontari()));
            tipi.put(tv.getTag(), td);
        }
        root.put("tipi-visita", tipi);
        JsonIO.scrivi(Percorsi.AMBITO, root);
    }

    public static void salvaUtenti(Sistema s) {
        Map<String, Object> root = JsonIO.nuovaMappa();
        root.put("primo_avvio", false);
        for (Configuratore c : s.getConfiguratori()) {
            Map<String, Object> ud = JsonIO.nuovaMappa();
            ud.put("tipo",          1L);
            ud.put("password-hash", c.getPasswordHash());
            ud.put("primo-accesso", c.isPrimoAccesso());
            root.put(c.getUsername(), ud);
        }
        for (Volontario v : s.getVolontari()) {
            Map<String, Object> ud = JsonIO.nuovaMappa();
            ud.put("tipo",          2L);
            ud.put("password-hash", v.getPasswordHash());
            ud.put("primo-accesso", v.isPrimoAccesso());
            root.put(v.getNickname(), ud);
        }
        for (Fruitore f : s.getFruitori()) {
            Map<String, Object> ud = JsonIO.nuovaMappa();
            ud.put("tipo",          3L);
            ud.put("password-hash", f.getPasswordHash());
            ud.put("primo-accesso", false);
            root.put(f.getUsername(), ud);
        }
        JsonIO.scrivi(Percorsi.UTENTI, root);
    }

    public static void salvaPiano(Sistema s) {
        Map<String, Object> root = JsonIO.nuovaMappa();
        root.put("fase",           s.getFase().toJson());
        root.put("anno-raccolta",  (long) s.getAnnoRaccolta());
        root.put("mese-raccolta",  (long) s.getMeseRaccolta());

        // date precluse mese corrente
        List<Object> precluse = new ArrayList<>();
        s.getDatePrecluse(s.getAnnoRaccolta(), s.getMeseRaccolta())
            .forEach(d -> precluse.add(d.format(FMT)));
        root.put("date-precluse", precluse);

        // disponibilità
        Map<String, Object> disp = JsonIO.nuovaMappa();
        for (Volontario v : s.getVolontari()) {
            List<Object> date = new ArrayList<>();
            v.getDisponibilita(s.getAnnoRaccolta(), s.getMeseRaccolta())
                .forEach(d -> date.add(d.format(FMT)));
            if (!date.isEmpty()) disp.put(v.getNickname(), date);
        }
        root.put("disponibilita", disp);

        // visite correnti
        Map<String, Object> visite = JsonIO.nuovaMappa();
        for (Visita v : s.getVisite()) {
            Map<String, Object> vd = JsonIO.nuovaMappa();
            vd.put("stato", v.getStato().toJson());
            vd.put("guida", v.getGuidaNickname());
            visite.put(v.chiaveJson(), vd);
        }
        root.put("visite", visite);
        JsonIO.scrivi(Percorsi.PIANO, root);
    }

    public static void salvaStorico(Sistema s) {
        Map<String, Object> root = JsonIO.nuovaMappa();
        for (Visita v : s.getArchivio())
            root.put(v.chiaveJson(), v.getLuogoTag());
        JsonIO.scrivi(Percorsi.STORICO, root);
    }

    public static void salvaPrenotazioni(Sistema s) {
        Map<String, Object> root = JsonIO.nuovaMappa();
        for (Visita v : s.getVisite()) {
            for (Iscrizione i : v.getIscrizioni()) {
                Map<String, Object> pd = JsonIO.nuovaMappa();
                pd.put("username", i.getUsernameFruitore());
                pd.put("giorno",   v.getData().format(FMT));
                pd.put("tipo",     v.getTipoTag());
                pd.put("persone",  (long) i.getNumPersone());
                root.put(i.getCodice(), pd);
            }
        }
        JsonIO.scrivi(Percorsi.PRENOTAZIONI, root);
    }

    // salva tutto in una volta sola
    public static void salvaTutto(Sistema s) {
        salvaAmbito(s);
        salvaUtenti(s);
        salvaPiano(s);
        salvaStorico(s);
        salvaPrenotazioni(s);
    }
}
