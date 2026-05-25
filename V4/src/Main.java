import controller.Controller;
import model.*;
import storage.Persistenza;
import ui.*;
import java.io.File;
import java.time.LocalDate;

/*
 * VERSIONE 4 – Tutti e tre i tipi di utente.
 * Aggiunge: fruitore, iscrizioni, disdette.
 */
public class Main {

    public static void main(String[] args) {
        new File("json").mkdirs();
        new File("out").mkdirs();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  VISITE GUIDATE MANTOVA  –  Versione 4   ║");
        System.out.println("╚══════════════════════════════════════════╝");

        Sistema sistema = new Sistema();
        java.io.File fUtenti = new java.io.File("json/users.json");
        if (fUtenti.exists()) {
            Persistenza.caricaTutto(sistema);
            System.out.println("  Dati caricati.\n");
        } else {
            System.out.println("  Primo avvio.\n");
            sistema.aggiungiConfiguratore(new Configuratore(Sistema.CRED_USR, Sistema.CRED_PWD));
            Persistenza.salvaUtenti(sistema);
        }

        sistema.aggiornaStati(LocalDate.now());
        Controller ctrl = new Controller(sistema);
        MenuConfiguratore mc = new MenuConfiguratore(ctrl);
        MenuVolontario    mv = new MenuVolontario(ctrl);
        MenuFruitore      mf = new MenuFruitore(ctrl);

        boolean running = true;
        while (running) {
            System.out.println("\n  Chi sei?");
            System.out.println("  1. Configuratore");
            System.out.println("  2. Volontario");
            System.out.println("  3. Fruitore");
            System.out.println("  4. Registra configuratore");
            System.out.println("  5. Registra fruitore");
            System.out.println("  0. Esci");
            int scelta = ui.Console.leggiInt("  Scelta: ", 0, 5);
            switch (scelta) {
                case 1 -> { Configuratore c = mc.login(); if (c != null) mc.menu(c); }
                case 2 -> { Volontario v = mv.login();   if (v != null) mv.menu(v); }
                case 3 -> { Fruitore f = mf.login();     if (f != null) mf.menu(f); }
                case 4 -> mc.registrazione();
                case 5 -> mf.registrazione();
                case 0 -> running = false;
            }
        }
        System.out.println("  Arrivederci!");
    }
}
