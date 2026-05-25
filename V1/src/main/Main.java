import controller.Controller;
import model.*;
import storage.Persistenza;
import ui.*;
import java.io.File;
import java.time.LocalDate;

/*
 * VERSIONE 1 – Solo configuratore.
 * Abilita: login/registrazione configuratore, gestione luoghi/tipi/volontari,
 *          visualizzazioni, date precluse, max persone per iscrizione.
 */
public class Main {

    public static void main(String[] args) {
        new File("json").mkdirs();
        new File("out").mkdirs();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║           VISITE GUIDATE MANTOVA         ║");
        System.out.println("╚══════════════════════════════════════════╝");

        Sistema sistema = new Sistema();

        // se il file users.json esiste carichiamo i dati, altrimenti primo avvio
        java.io.File fUtenti = new java.io.File("json/users.json");
        if (fUtenti.exists()) {
            Persistenza.caricaTutto(sistema);
            System.out.println("  Dati caricati da JSON.\n");
        } else {
            System.out.println("  Primo avvio: creo il configuratore predefinito (admin/admin123).\n");
            sistema.aggiungiConfiguratore(new Configuratore(Sistema.CRED_USR, Sistema.CRED_PWD));
            Persistenza.salvaUtenti(sistema);
        }

        sistema.aggiornaStati(LocalDate.now());

        Controller ctrl = new Controller(sistema);
        MenuConfiguratore mc = new MenuConfiguratore(ctrl);

        boolean running = true;
        while (running) {
            System.out.println("\n  Chi sei?");
            System.out.println("  1. Configuratore");
            System.out.println("  2. Registra nuovo configuratore");
            System.out.println("  0. Esci");
            int scelta = ui.Console.leggiInt("  Scelta: ", 0, 2);
            switch (scelta) {
                case 1 -> { Configuratore c = mc.login(); if (c != null) mc.menu(c); }
                case 2 -> mc.registrazione();
                case 0 -> running = false;
            }
        }
        System.out.println("  Arrivederci!");
    }
}
