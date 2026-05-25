package ui;

import controller.Controller;
import model.*;

import java.time.LocalDate;
import java.util.*;

// interfaccia testuale per il volontario 
public class MenuVolontario {

    private final Controller ctrl;

    public MenuVolontario(Controller ctrl) { this.ctrl = ctrl; }

    public Volontario login() {
        Console.titolo("ACCESSO VOLONTARIO");
        String nick = Console.leggiStringa("  Nickname: ");
        String pwd  = Console.leggiStringa("  Password: ");
        try {
            Volontario v = ctrl.loginVolontario(nick, pwd);
            System.out.println("  Benvenuto, " + v.getNickname() + "!");
            if (v.isPrimoAccesso()) cambioPasswordObbligatorio(v);
            return v;
        } catch (Exception e) { System.out.println("  Accesso negato: " + e.getMessage()); 
            return null; 
        }
    }

    private void cambioPasswordObbligatorio(Volontario v) {
        System.out.println("\n  [Primo accesso  scegli una nuova password]");
        while (true) {
            String p1 = Console.leggiStringa("  Nuova password: ");
            String p2 = Console.leggiStringa("  Conferma: ");
            if (!p1.equals(p2)) { System.out.println("  Non coincidono."); continue; }
            ctrl.cambiaPasswordVolontario(v, p1);
            System.out.println("  Password aggiornata.");
            break;
        }
    }

    public void menu(Volontario v) {
        boolean esci = false;
        while (!esci) {
            LocalDate prossimo = LocalDate.now().plusMonths(1);
            String meseProssimo = ctrl.getMonth().name().toLowerCase(Locale.ITALIAN) +" " + prossimo.getYear();
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  VOLONTARIO: " + v.getNickname() + meseProssimo);
            System.out.println("  1. I miei tipi di visita");
            System.out.println("  2. Dichiara disponibilità per " + meseProssimo);
            System.out.println("  3. Visualizza le mie disponibilità");
            System.out.println("  4. Rimuovi una disponibilità");
            System.out.println("  5. Visite confermate in cui sono guida");
            System.out.println("  0. Esci");
            switch (Console.leggiInt("  Scelta: ", 0, 5)) {
                case 1 -> mostraTipiVisita(v);
                case 2 -> dichiaraDisponibilita(v);
                case 3 -> mostraDisponibilita(v);
                case 4 -> rimuoviDisponibilita(v);
                case 5 -> mostraVisiteConfermate(v);
                case 0 -> esci = true;
            }
        }
    }
// ------ metodi per le funzionalità del menu -------------
    private void mostraTipiVisita(Volontario v) {
        Console.titolo("I MIEI TIPI DI VISITA");
        List<TipoVisita> tipi = ctrl.tipiDelVolontario(v);
        if (tipi.isEmpty()) { System.out.println("  Nessun tipo associato."); Console.pausa(); return; }
        for (TipoVisita tv : tipi) {
            System.out.println("\n  [" + tv.getTag() + "] " + tv.getTitolo());
            System.out.println("    Periodo: " + tv.getInizioPeriodo() + " → " + tv.getFinePeriodo());
            System.out.print("    Giorni: ");
            tv.getGiorni().forEach(g -> System.out.print(g + " "));
            System.out.println();
            System.out.println("    Ora: " + tv.getOraInizio() + ", durata: " + tv.getDurata() + "min");
            System.out.println("    Biglietto: " + (tv.vuoleBiglietto() ? "sì" : "no"));
        }
        Console.pausa();
    }

    private void dichiaraDisponibilita(Volontario v) {
        Console.titolo("DICHIARA DISPONIBILITÀ");
        System.out.println("  Inserisci le date in cui sei disponibile (digita 'fine' per terminare).");
        while (true) {
            System.out.print("  Data (GG-MM-AAAA) o 'fine': ");
            String input = Console.getScanner().nextLine().trim();
            if (input.equalsIgnoreCase("fine")) break;
            try {
                LocalDate data = LocalDate.parse(input,
                    java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                ctrl.aggiungiDisponibilita(v, data);
                System.out.println("  ✓ " + data + " aggiunta.");
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("  [formato: GG-MM-AAAA]");
            } catch (Exception e) {
                System.out.println("  Non aggiunta: " + e.getMessage());
            }
        }
        mostraDisponibilita(v);
    }

    private void mostraDisponibilita(Volontario v) {
        Console.titolo("LE MIE DISPONIBILITÀ");
        Set<LocalDate> disp = ctrl.getDisponibilita(v, 0, 0);
        if (disp.isEmpty()) System.out.println("  Nessuna disponibilità dichiarata.");
        else disp.stream().sorted().forEach(d ->
            System.out.println("  ✓ " + d + " (" + GiornoSettimana.da(d.getDayOfWeek()) + ")"));
        Console.pausa();
    }

    private void rimuoviDisponibilita(Volontario v) {
        Console.titolo("RIMUOVI DISPONIBILITÀ");
        LocalDate d = Console.leggiData("  Data da rimuovere");
        ctrl.rimuoviDisponibilita(v, d);
        System.out.println("  Rimossa (se era presente).");
        Console.pausa();
    }

    private void mostraVisiteConfermate(Volontario v) {
        Console.titolo("LE MIE VISITE CONFERMATE");
        List<Visita> visite = ctrl.getVisiteConfermate(v);
        if (visite.isEmpty()) { System.out.println("  Nessuna visita confermata."); Console.pausa(); return; }
        for (Visita vis : visite) {
            ctrl.trovaTipo(vis.getTipoTag()).ifPresent(tv -> {
                System.out.println("\n  📅 " + vis.getData() + "  " + tv.getTitolo());
                System.out.println("     Ora: " + tv.getOraInizio()
                    + "  Luogo: " + vis.getLuogoTag());
                System.out.println("     Incontro: " + tv.getPuntoIncontro());
                System.out.println("     Iscritti: " + vis.totaleIscritti() + "/" + tv.getMax());
                if (!vis.getIscrizioni().isEmpty()) {
                    System.out.println("     Prenotazioni:");
                    vis.getIscrizioni().forEach(i ->
                        System.out.println("       " + i.getCodice()
                            + "  " + i.getNumPersone() + " pers. (" + i.getUsernameFruitore() + ")"));
                }
            });
        }
        Console.pausa();
    }
}
