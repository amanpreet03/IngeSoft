package controller;

import model.*;

import java.time.*;
import java.util.*;

/*
 * Genera l'elenco delle visite proponibili per un dato mese.
 *
 * Algoritmo greedy: per ogni giorno del mese scorre tutti i tipi di visita
 * e cerca un volontario disponibile non ancora impegnato in quel giorno.
 *
 * Vincoli rispettati:
 *   - al massimo una visita per tipo per giorno
 *   - al massimo una visita per volontario per giorno
 *   - la data non è preclusa
 *   - il tipo è programmabile nel giorno della settimana e nel suo periodo annuale
 *   - esiste un volontario disponibile quel giorno per quel tipo
 *
 * La spec dice "elevato numero, non necessariamente il massimo":
 * l'approccio greedy è quindi adeguato e non richiede algoritmi di ottimizzazione.
 */
public class Pianificatore {

    private Pianificatore() {}

    public static List<Visita> genera(Sistema sistema, int anno, int mese) {
        List<Visita> piano = new ArrayList<>();
        YearMonth ym = YearMonth.of(anno, mese);
        Set<LocalDate> precluse = sistema.getDatePrecluse(anno, mese);

        for (int g = 1; g <= ym.lengthOfMonth(); g++) {
            LocalDate data = LocalDate.of(anno, mese, g);
            if (precluse.contains(data)) continue;

            GiornoSettimana gds = GiornoSettimana.da(data.getDayOfWeek());
            Set<String> guidePrese = new HashSet<>(); // volontari già assegnati oggi

            for (Luogo luogo : sistema.getLuoghi()) {
                for (TipoVisita tv : luogo.getTipiVisita()) {
                    if (!tv.attivaNelGiorno(gds)) continue;
                    if (!tv.nelPeriodo(data))     continue;

                    String guida = trovaPrimaGuida(tv, data, guidePrese, sistema);
                    if (guida == null) continue;

                    piano.add(new Visita(tv.getTag(), luogo.getTag(), data, guida));
                    guidePrese.add(guida);
                }
            }
        }
        return piano;
    }

    private static String trovaPrimaGuida(TipoVisita tv, LocalDate data,
                                          Set<String> occupati, Sistema sistema) {
        for (String nick : tv.getVolontari()) {
            if (occupati.contains(nick)) continue;
            Volontario v = sistema.trovaVolontario(nick).orElse(null);
            if (v != null && v.isDisponibileIn(data)) return nick;
        }
        return null;
    }
}
