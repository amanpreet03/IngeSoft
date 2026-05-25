package ui;

import controller.Controller;
import model.*;

import java.time.*;
import java.util.*;

// interfaccia testuale per il configuratore (V1 + estensioni V2, V3)
public class MenuConfiguratore {

    private final Controller ctrl;

    public MenuConfiguratore(Controller ctrl) { this.ctrl = ctrl; }

    // ---- accesso ----

    public Configuratore login() {
        Console.titolo("ACCESSO CONFIGURATORE");
        String usr = Console.leggiStringa("  Username: ");
        String pwd = Console.leggiStringa("  Password: ");
        try {
            Configuratore c = ctrl.loginConfiguratore(usr, pwd);
            System.out.println("  Benvenuto, " + c.getUsername() + "!");
            if (c.isPrimoAccesso()) cambioPasswordObbligatorio(c);
            return c;
        } catch (Exception e) {
            System.out.println("  Accesso negato: " + e.getMessage());
            return null;
        }
    }

    public void registrazione() {
        Console.titolo("REGISTRA NUOVO CONFIGURATORE");
        System.out.println("  Usa le credenziali predefinite: admin / admin123");
        String credUsr = Console.leggiStringa("  Username predefinito: ");
        String credPwd = Console.leggiStringa("  Password predefinita: ");
        String nuovoUsr = Console.leggiStringa("  Scegli il tuo username: ");
        String nuovaPwd = Console.leggiStringa("  Scegli la tua password: ");
        try {
            ctrl.registraConfiguratore(credUsr, credPwd, nuovoUsr, nuovaPwd);
            System.out.println("  Registrazione completata.");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void cambioPasswordObbligatorio(Configuratore c) {
        System.out.println("\n  [Primo accesso – scegli una nuova password]");
        boolean passwordAggiornata = false;
        while (!passwordAggiornata) {
            String p1 = Console.leggiStringa("  Nuova password: ");
            String p2 = Console.leggiStringa("  Conferma: ");
            if (!p1.equals(p2)) {
                System.out.println("  Non coincidono.");
            } else {
                ctrl.cambiaPasswordConfiguratore(c, p1);
                System.out.println("  Password aggiornata.");
                passwordAggiornata = true;
            }
        }
    }

    // ---- menu principale ----

    public void menu(Configuratore c) {
        boolean esci = false;
        while (!esci) {
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  CONFIGURATORE: " + c.getUsername());
            if (!ctrl.ambitoImpostato()) {
                System.out.println("  [!] Sistema non inizializzato");
                System.out.println("  1. Inizializza sistema   0. Esci");
                if (Console.leggiInt("  Scelta: ", 0, 1) == 1) inizializza();
                else esci = true;
                continue;
            }
            System.out.println("  Ambito: " + ctrl.getAmbito() + " | Fase: " + ctrl.getFase());
            System.out.println("  ─── Dati ───────────────────────────────");
            System.out.println("  1.  Aggiungi luogo");
            System.out.println("  2.  Aggiungi tipo di visita a luogo");
            System.out.println("  3.  Aggiungi volontario a tipo di visita");
            System.out.println("  4.  Inserisci nuovo volontario");
            System.out.println("  5.  Rimuovi luogo  [V3]");
            System.out.println("  6.  Rimuovi tipo di visita  [V3]");
            System.out.println("  7.  Rimuovi volontario  [V3]");
            System.out.println("  ─── Ciclo mensile [V3] ─────────────────");
            System.out.println("  8.  Chiudi raccolta disponibilità");
            System.out.println("  9.  Genera piano visite");
            System.out.println("  10. Apri nuova raccolta");
            System.out.println("  ─── Visualizza ─────────────────────────");
            System.out.println("  11. Luoghi visitabili");
            System.out.println("  12. Volontari");
            System.out.println("  13. Visite");
            System.out.println("  14. Date precluse");
            System.out.println("  15. Modifica max persone per iscrizione");
            System.out.println("  0.  Esci");
            int s = Console.leggiInt("  Scelta: ", 0, 15);
            switch (s) {
                case 1  -> aggiungiLuogo();
                case 2  -> aggiungiTipoVisita();
                case 3  -> aggiungiVolontarioATipo();
                case 4  -> inserisciVolontario();
                case 5  -> rimuoviLuogo();
                case 6  -> rimuoviTipoVisita();
                case 7  -> rimuoviVolontario();
                case 8  -> chiudiRaccolta();
                case 9  -> generaPiano();
                case 10 -> apriRaccolta();
                case 11 -> mostraLuoghi();
                case 12 -> mostraVolontari();
                case 13 -> mostraVisite();
                case 14 -> gestionePrecluse();
                case 15 -> modificaMaxPersone();
                case 0  -> esci = true;
            }
        }
    }

    // ---- operazioni ----

    private void inizializza() {
        Console.titolo("INIZIALIZZAZIONE");
        String ambito = Console.leggiStringa("  Ambito territoriale: ");
        int max = Console.leggiInt("  Max persone per iscrizione: ", 1);
        try { ctrl.inizializza(ambito, max); System.out.println("  Sistema inizializzato."); }
        catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void aggiungiLuogo() {
        Console.titolo("AGGIUNGI LUOGO");
        String tag   = Console.leggiStringa("  Tag (es. palazzo_ducale): ");
        String nome  = Console.leggiStringa("  Nome: ");
        String descr = Console.leggiStringaOpt("  Descrizione (INVIO per saltare): ");
        String coll  = Console.leggiStringa("  Collocazione: ");
        try {
            Luogo l = ctrl.creaLuogo(tag, nome, descr, coll);
            do {
                TipoVisita tv = raccogliTipoVisita(tag);
                raccogliVolontariPerTipo(tv);
                try { l.aggiungiTipoVisita(tv); System.out.println("  Tipo aggiunto."); }
                catch (Exception e) { System.out.println("  Tipo non aggiunto: " + e.getMessage()); }
            } while (Console.leggiSiNo("  Aggiungere un altro tipo di visita?"));
            ctrl.salvaLuogo(l);
            System.out.println("  Luogo salvato.");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void aggiungiTipoVisita() {
        Console.titolo("AGGIUNGI TIPO DI VISITA");
        mostraLuoghiBreve();
        String tagLuogo = Console.leggiStringa("  Tag luogo: ");
        try {
            Luogo l = ctrl.ottieniLuogo(tagLuogo);
            TipoVisita tv = raccogliTipoVisita(tagLuogo);
            raccogliVolontariPerTipo(tv);
            ctrl.aggiungiTipoVisita(l, tv);
            System.out.println("  Tipo di visita aggiunto.");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void aggiungiVolontarioATipo() {
        Console.titolo("AGGIUNGI VOLONTARIO A TIPO DI VISITA");
        mostraLuoghiBreve();
        String tagLuogo = Console.leggiStringa("  Tag luogo: ");
        try {
            Luogo l = ctrl.ottieniLuogo(tagLuogo);
            l.getTipiVisita().forEach(tv -> System.out.println("    – [" + tv.getTag() + "] " + tv.getTitolo()));
            String tagTipo = Console.leggiStringa("  Tag tipo di visita: ");
            TipoVisita tv = l.trovaTipo(tagTipo)
                .orElseThrow(() -> new IllegalArgumentException("Tipo non trovato."));
            ctrl.collegaVolontario(Console.leggiStringa("  Nickname volontario: "), tv);
            System.out.println("  Volontario aggiunto.");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void inserisciVolontario() {
        Console.titolo("INSERISCI VOLONTARIO");
        try {
            ctrl.creaVolontario(Console.leggiStringa("  Nickname: "),
                                Console.leggiStringa("  Password iniziale: "));
            System.out.println("  Volontario aggiunto.");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void rimuoviLuogo() {
        Console.titolo("RIMUOVI LUOGO");
        mostraLuoghiBreve();
        String tag = Console.leggiStringa("  Tag luogo da rimuovere: ");
        if (!Console.leggiSiNo("  Sicuro? Verranno rimossi anche i tipi di visita associati"))
            { System.out.println("  Annullato."); Console.pausa(); return; }
        try { ctrl.rimuoviLuogo(tag); System.out.println("  Luogo rimosso (cascata applicata)."); }
        catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void rimuoviTipoVisita() {
        Console.titolo("RIMUOVI TIPO DI VISITA");
        mostraLuoghiBreve();
        String tagLuogo = Console.leggiStringa("  Tag luogo: ");
        try {
            ctrl.ottieniLuogo(tagLuogo).getTipiVisita()
                .forEach(tv -> System.out.println("    – [" + tv.getTag() + "] " + tv.getTitolo()));
            String tagTipo = Console.leggiStringa("  Tag tipo da rimuovere: ");
            if (!Console.leggiSiNo("  Confermi?"))
                { System.out.println("  Annullato."); Console.pausa(); return; }
            ctrl.rimuoviTipoVisita(tagLuogo, tagTipo);
            System.out.println("  Tipo rimosso (cascata applicata).");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void rimuoviVolontario() {
        Console.titolo("RIMUOVI VOLONTARIO");
        ctrl.getVolontari().forEach(v -> System.out.println("    – " + v.getNickname()));
        String nick = Console.leggiStringa("  Nickname da rimuovere: ");
        if (!Console.leggiSiNo("  Sicuro? Possono venir rimossi anche tipi/luoghi collegati"))
            { System.out.println("  Annullato."); Console.pausa(); return; }
        try { ctrl.rimuoviVolontario(nick); System.out.println("  Volontario rimosso (cascata applicata)."); }
        catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void chiudiRaccolta() {
        Console.titolo("CHIUDI RACCOLTA DISPONIBILITÀ");
        try { ctrl.chiudiRaccolta(); System.out.println("  Raccolta chiusa."); }
        catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void generaPiano() {
        Console.titolo("GENERA PIANO VISITE");
        try {
            List<Visita> nuove = ctrl.generaPiano();
            System.out.println("  Piano generato: " + nuove.size() + " visite proposte.");
            nuove.forEach(v -> System.out.println(
                "    " + v.getData() + " – [" + v.getTipoTag() + "] guida: " + v.getGuidaNickname()));
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void apriRaccolta() {
        Console.titolo("APRI NUOVA RACCOLTA");
        try {
            ctrl.apriNuovaRaccolta();
            System.out.println("  Raccolta aperta per il mese "
                + ctrl.getMeseRaccolta() + "/" + ctrl.getAnnoRaccolta() + ".");
        } catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    private void mostraLuoghi() {
        Console.titolo("LUOGHI VISITABILI");
        if (ctrl.getLuoghi().isEmpty()) { System.out.println("  Nessun luogo."); Console.pausa(); return; }
        for (Luogo l : ctrl.getLuoghi()) {
            System.out.println("\n  [" + l.getTag() + "] " + l.getNome() + " – " + l.getCollocazione());
            if (l.getDescrizione() != null) System.out.println("  " + l.getDescrizione());
            for (TipoVisita tv : l.getTipiVisita()) {
                System.out.println("    • [" + tv.getTag() + "] " + tv.getTitolo()
                    + "  " + tv.getOraInizio() + "  " + tv.getDurata() + "min"
                    + "  min=" + tv.getMin() + " max=" + tv.getMax()
                    + (tv.vuoleBiglietto() ? "  🎫" : ""));
                System.out.print("      Giorni: ");
                tv.getGiorni().forEach(g -> System.out.print(g + " "));
                System.out.println();
                System.out.println("      Volontari: " + tv.getVolontari());
            }
        }
        Console.pausa();
    }

    private void mostraVolontari() {
        Console.titolo("VOLONTARI");
        if (ctrl.getVolontari().isEmpty()) { System.out.println("  Nessun volontario."); Console.pausa(); return; }
        for (Volontario v : ctrl.getVolontari()) {
            System.out.println("  👤 " + v.getNickname());
            ctrl.tipiDelVolontario(v).forEach(tv ->
                System.out.println("     • [" + tv.getTag() + "] " + tv.getTitolo()));
        }
        Console.pausa();
    }

    private void mostraVisite() {
        Console.titolo("VISITE");
        System.out.println("  1.Proposte  2.Complete  3.Confermate  4.Cancellate  5.Storico  0.Indietro");
        int s = Console.leggiInt("  Scelta: ", 0, 5);
        if (s == 0) return;
        StatoVisita stato = switch (s) {
            case 1 -> StatoVisita.PROPOSTA; case 2 -> StatoVisita.COMPLETA;
            case 3 -> StatoVisita.CONFERMATA; case 4 -> StatoVisita.CANCELLATA;
            default -> StatoVisita.EFFETTUATA;
        };
        List<Visita> visite = ctrl.getVisitePerStato(stato);
        if (visite.isEmpty()) System.out.println("  Nessuna.");
        else visite.forEach(v -> System.out.println(
            "  " + v.getData() + " [" + v.getTipoTag() + "]"
            + " guida:" + v.getGuidaNickname()
            + " iscritti:" + v.totaleIscritti() + " stato:" + v.getStato()));
        Console.pausa();
    }

    private void gestionePrecluse() {
        Console.titolo("DATE PRECLUSE");
        System.out.println("  1. Aggiungi   2. Visualizza   0. Indietro");
        int s = Console.leggiInt("  Scelta: ", 0, 2);
        if (s == 0) return;
        if (s == 1) {
            LocalDate d = Console.leggiData("  Data da precludere");
            ctrl.aggiungiDataPreclusa(d);
            System.out.println("  Preclusa: " + d);
        } else {
            LocalDate p = LocalDate.now().plusMonths(1);
            Set<LocalDate> pr = ctrl.getDatePrecluse(p.getYear(), p.getMonthValue());
            System.out.println("  Mese " + p.getMonthValue() + "/" + p.getYear() + ":");
            if (pr.isEmpty()) System.out.println("  (nessuna)");
            else pr.stream().sorted().forEach(d -> System.out.println("    " + d));
        }
        Console.pausa();
    }

    private void modificaMaxPersone() {
        Console.titolo("MAX PERSONE PER ISCRIZIONE");
        System.out.println("  Attuale: " + ctrl.getMaxPersone());
        ctrl.setMaxPersone(Console.leggiInt("  Nuovo valore: ", 1));
        System.out.println("  Aggiornato.");
        Console.pausa();
    }

    // ---- helper raccolta dati ----

    TipoVisita raccogliTipoVisita(String luogoTag) {
        System.out.println("\n  ─ Dati tipo di visita ─");
        String tag   = Console.leggiStringa("  Tag (es. camera_sposi): ");
        String tit   = Console.leggiStringa("  Titolo: ");
        String descr = Console.leggiStringa("  Descrizione: ");
        String punto = Console.leggiStringa("  Punto di incontro: ");
        MonthDay di  = Console.leggiGiornoMese("  Inizio periodo");
        MonthDay df  = Console.leggiGiornoMese("  Fine periodo");
        Set<GiornoSettimana> giorni = Console.leggiGiorni();
        LocalTime ora = Console.leggiOra("  Ora di inizio");
        int dur  = Console.leggiInt("  Durata (minuti): ", 1);
        boolean big = Console.leggiSiNo("  Biglietto richiesto?");
        int minP = Console.leggiInt("  Min partecipanti: ", 1);
        int maxP = Console.leggiInt("  Max partecipanti (>= " + minP + "): ", minP);
        return TipoVisita.builder(tag, tit, luogoTag)
            .descrizione(descr)
            .puntoIncontro(punto)
            .periodo(di, df)
            .giorni(giorni)
            .oraInizio(ora)
            .durata(dur)
            .bigliettoRichiesto(big)
            .partecipanti(minP, maxP)
            .build();
    }

    void raccogliVolontariPerTipo(TipoVisita tv) {
        System.out.println("  Aggiungi almeno un volontario:");
        do {
            String nick = Console.leggiStringa("  Nickname volontario: ");
            try { ctrl.collegaVolontario(nick, tv); System.out.println("  Aggiunto."); }
            catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        } while (!tv.getVolontari().isEmpty() == false
              || Console.leggiSiNo("  Aggiungere un altro volontario?"));
    }

    private void mostraLuoghiBreve() {
        ctrl.getLuoghi().forEach(l -> System.out.println("    [" + l.getTag() + "] " + l.getNome()));
    }
}
