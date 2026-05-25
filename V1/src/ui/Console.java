package ui;

import model.GiornoSettimana;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

// metodi di supporto per leggere input dalla console in modo pulito
public class Console {

    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private Console() {}

    public static String leggiStringa(String prompt) {
        String s;
        do {
            System.out.print(prompt);
            s = sc.nextLine().trim();
            if (s.isEmpty()) System.out.println("  [campo obbligatorio]");
        } while (s.isEmpty());
        return s;
    }

    public static String leggiStringaOpt(String prompt) {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        return s.isEmpty() ? null : s;
    }

    public static int leggiInt(String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v >= min) return v;
                System.out.println("  [minimo: " + min + "]");
            } catch (NumberFormatException e) {
                System.out.println("  [inserisci un numero intero]");
            }
        }
    }

    public static int leggiInt(String prompt, int min, int max) {
        while (true) {
            int v = leggiInt(prompt, min);
            if (v <= max) return v;
            System.out.println("  [massimo: " + max + "]");
        }
    }

    public static boolean leggiSiNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (s/n): ");
            String r = sc.nextLine().trim().toLowerCase();
            if (r.equals("s") || r.equals("si") || r.equals("sì")) return true;
            if (r.equals("n") || r.equals("no")) return false;
            System.out.println("  [digita s oppure n]");
        }
    }

    public static LocalDate leggiData(String prompt) {
        while (true) {
            System.out.print(prompt + " (GG-MM-AAAA): ");
            try {
                return LocalDate.parse(sc.nextLine().trim(), FMT);
            } catch (Exception e) {
                System.out.println("  [formato: GG-MM-AAAA, es. 25-12-2025]");
            }
        }
    }

    public static LocalTime leggiOra(String prompt) {
        while (true) {
            System.out.print(prompt + " (HH:MM): ");
            try {
                return LocalTime.parse(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("  [formato: HH:MM, es. 10:30]");
            }
        }
    }

    public static MonthDay leggiGiornoMese(String prompt) {
        while (true) {
            System.out.print(prompt + " (GG-MM): ");
            try {
                String[] p = sc.nextLine().trim().split("-");
                return MonthDay.of(Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            } catch (Exception e) {
                System.out.println("  [formato: GG-MM, es. 01-04]");
            }
        }
    }

    public static Set<GiornoSettimana> leggiGiorni() {
        GiornoSettimana[] tutti = GiornoSettimana.values();
        System.out.println("  Giorni programmabili:");
        for (int i = 0; i < tutti.length; i++)
            System.out.println("    " + (i+1) + ". " + tutti[i]);
        Set<GiornoSettimana> scelti = new LinkedHashSet<>();
        while (scelti.isEmpty()) {
            System.out.print("  Numeri separati da spazio (es: 6 7): ");
            for (String tok : sc.nextLine().trim().split("\\s+")) {
                try {
                    int idx = Integer.parseInt(tok) - 1;
                    if (idx >= 0 && idx < tutti.length) scelti.add(tutti[idx]);
                } catch (NumberFormatException ignored) {}
            }
            if (scelti.isEmpty()) System.out.println("  [seleziona almeno un giorno]");
        }
        return scelti;
    }

    public static void pausa() {
        System.out.print("\n  [INVIO per continuare]");
        sc.nextLine();
    }

    public static Scanner getScanner() { return sc; }

    // separatore visivo per i menu
    public static void titolo(String t) {
        System.out.println("\n══ " + t + " " + "═".repeat(Math.max(0, 36 - t.length())));
    }
}
