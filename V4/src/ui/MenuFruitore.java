package ui;

import controller.Controller;
import model.*;

import java.util.List;

// interfaccia testuale per il fruitore (V4)
public class MenuFruitore {

    private final Controller ctrl;

    public MenuFruitore(Controller ctrl) { this.ctrl = ctrl; }

    public Fruitore login() {
        Console.titolo("ACCESSO FRUITORE");
        String usr = Console.leggiStringa("  Username: ");
        String pwd = Console.leggiStringa("  Password: ");
        try {
            Fruitore f = ctrl.loginFruitore(usr, pwd);
            System.out.println("  Benvenuto, " + f.getUsername() + "!");
            return f;
        } catch (Exception e) { System.out.println("  Accesso negato: " + e.getMessage()); return null; }
    }

    public void registrazione() {
        Console.titolo("REGISTRAZIONE FRUITORE");
        String usr = Console.leggiStringa("  Scegli username: ");
        String pwd = Console.leggiStringa("  Scegli password: ");
        String pwd2 = Console.leggiStringa("  Conferma password: ");
        if (!pwd.equals(pwd2)) { System.out.println("  Le password non coincidono."); Console.pausa(); return; }
        try { ctrl.registraFruitore(usr, pwd); System.out.println("  Registrazione completata!"); }
        catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }

    public void menu(Fruitore f) {
        boolean esci = false;
        while (!esci) {
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  FRUITORE: " + f.getUsername());
            System.out.println("  1. Visualizza tutte le visite");
            System.out.println("  2. Iscriviti a una visita");
            System.out.println("  3. Le mie prenotazioni");
            System.out.println("  4. Disdici una prenotazione");
            System.out.println("  0. Esci");
            switch (Console.leggiInt("  Scelta: ", 0, 4)) {
                case 1 -> visualizzaVisite();
                case 2 -> iscrivitiAVisita(f);
                case 3 -> miePrenotazioni(f);
                case 4 -> disdiciPrenotazione(f);
                case 0 -> esci = true;
            }
        }
    }

    private void visualizzaVisite() {
        Console.titolo("VISITE DISPONIBILI");
        List<Visita> visite = ctrl.getVisiteFruitore();
        if (visite.isEmpty()) { System.out.println("  Nessuna visita al momento."); Console.pausa(); return; }
        for (Visita v : visite) {
            System.out.println();
            if (v.getStato() == StatoVisita.CANCELLATA) {
                System.out.println("  ✗ CANCELLATA | " + v.getData()
                    + " – " + ctrl.trovaTipo(v.getTipoTag()).map(TipoVisita::getTitolo).orElse(v.getTipoTag()));
            } else {
                String stato = v.getStato() == StatoVisita.CONFERMATA ? "✓ CONFERMATA" : "● PROPOSTA";
                ctrl.trovaTipo(v.getTipoTag()).ifPresent(tv -> {
                    System.out.println("  " + stato + " | " + v.getData() + " – " + tv.getTitolo());
                    System.out.println("    📍 " + tv.getPuntoIncontro());
                    System.out.println("    🕐 " + tv.getOraInizio() + "  durata: " + tv.getDurata() + "min");
                    System.out.println("    📋 " + tv.getDescrizione());
                    System.out.println("    🎫 Biglietto: " + (tv.vuoleBiglietto() ? "sì" : "no"));
                    if (v.getStato() == StatoVisita.PROPOSTA)
                        System.out.println("    👥 Posti liberi: " + (tv.getMax() - v.totaleIscritti()));
                });
            }
        }
        Console.pausa();
    }

    private void iscrivitiAVisita(Fruitore f) {
        Console.titolo("ISCRIVITI A UNA VISITA");
        List<Visita> proposte = ctrl.getVisiteProposte();
        if (proposte.isEmpty()) { System.out.println("  Nessuna visita proposta."); Console.pausa(); return; }

        for (int i = 0; i < proposte.size(); i++) {
            Visita v = proposte.get(i);
            int postiLiberi = ctrl.trovaTipo(v.getTipoTag())
                .map(tv -> tv.getMax() - v.totaleIscritti()).orElse(0);
            System.out.println("  " + (i+1) + ". " + v.getData()
                + " – " + ctrl.trovaTipo(v.getTipoTag()).map(TipoVisita::getTitolo).orElse(v.getTipoTag())
                + "  (posti: " + postiLiberi + ")");
        }
        System.out.println("  0. Annulla");
        int idx = Console.leggiInt("  Scegli: ", 0, proposte.size());
        if (idx == 0) return;

        Visita scelta = proposte.get(idx - 1);
        System.out.println("  Max persone per iscrizione: " + ctrl.getMaxPersone());
        int persone = Console.leggiInt("  Quante persone (incluso te)? ", 1, ctrl.getMaxPersone());
        try {
            String codice = ctrl.iscriviAVisita(f, scelta, persone);
            System.out.println("\n  ✓ Iscrizione confermata!");
            System.out.println("  Codice di prenotazione: " + codice);
            System.out.println("  Conservalo per presentarti o per disdire.");
        } catch (Exception e) { System.out.println("  Non riuscito: " + e.getMessage()); }
        Console.pausa();
    }

    private void miePrenotazioni(Fruitore f) {
        Console.titolo("LE MIE PRENOTAZIONI");
        List<Visita> mie = ctrl.getMieIscrizioni(f);
        if (mie.isEmpty()) { System.out.println("  Nessuna prenotazione."); Console.pausa(); return; }
        for (Visita v : mie) {
            v.getIscrizioni().stream()
                .filter(i -> i.getUsernameFruitore().equalsIgnoreCase(f.getUsername()))
                .forEach(i -> {
                    String titolo = ctrl.trovaTipo(v.getTipoTag())
                        .map(TipoVisita::getTitolo).orElse(v.getTipoTag());
                    System.out.println("\n  [" + i.getCodice() + "] " + v.getData()
                        + " – " + titolo);
                    System.out.println("    " + i.getNumPersone() + " persone | stato: " + v.getStato());
                    if (v.getStato() == StatoVisita.CONFERMATA || v.getStato() == StatoVisita.PROPOSTA) {
                        ctrl.trovaTipo(v.getTipoTag()).ifPresent(tv ->
                            System.out.println("    📍 " + tv.getPuntoIncontro()
                                + "  ore " + tv.getOraInizio()));
                    }
                });
        }
        Console.pausa();
    }

    private void disdiciPrenotazione(Fruitore f) {
        Console.titolo("DISDICI PRENOTAZIONE");
        List<Visita> disdiribili = ctrl.getMieIscrizioni(f).stream()
            .filter(v -> v.getStato() == StatoVisita.PROPOSTA || v.getStato() == StatoVisita.COMPLETA)
            .toList();

        if (disdiribili.isEmpty()) { System.out.println("  Nessuna prenotazione disdiribile."); Console.pausa(); return; }

        System.out.println("  Prenotazioni disdiribili:");
        disdiribili.forEach(v ->
            v.getIscrizioni().stream()
                .filter(i -> i.getUsernameFruitore().equalsIgnoreCase(f.getUsername()))
                .forEach(i -> System.out.println("    [" + i.getCodice() + "] "
                    + v.getData() + " – " + v.getTipoTag()
                    + " – " + i.getNumPersone() + " pers.")));

        String codice = Console.leggiStringa("  Codice da disdire (0 = annulla): ");
        if (codice.equals("0")) return;

        Visita target = disdiribili.stream()
            .filter(v -> v.cercaIscrizione(codice).isPresent())
            .findFirst().orElse(null);

        if (target == null) { System.out.println("  Codice non trovato."); Console.pausa(); return; }

        try { ctrl.disdiciIscrizione(f, target, codice); System.out.println("  ✓ Prenotazione disdetta."); }
        catch (Exception e) { System.out.println("  Errore: " + e.getMessage()); }
        Console.pausa();
    }
}
