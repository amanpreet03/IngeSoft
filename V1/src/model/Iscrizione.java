package model;

import java.util.UUID;

/**
 * Prenotazione di un iscritto a una visita proposta.
 * Invariante: codice != null && !codice.ismpty()
 *           usernameIscritto != null
 *          numPersone >= 1
 */
public class Iscrizione {

    private final String codice; // codice univoco generato automaticamente
    private final String usernameIscritto; // username della persona che si iscrive
    private final int numPersone; //  persone inlcuse in questa iscrizione (>=1)

    public Iscrizione(String usernameIscritto, int numPersone) {
        // i primi 8 caratteri dell'UUID bastano come codice leggibile
        this.codice = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.usernameIscritto = usernameIscritto;
        this.numPersone = numPersone;
    }

    // costruttore usato dalla persistenza per ricostruire un'iscrizione già esistente
    public Iscrizione(String codice, String usernameIscritto, int numPersone) {
        this.codice = codice;
        this.usernameIscritto = usernameIscritto;
        this.numPersone = numPersone;
    }

    public String getCodice()           { return codice; }
    public String getUsernameIscritto() { return usernameIscritto; }
    public int getNumPersone()          { return numPersone; }

    @Override
    public String toString() {
        return codice + " (" + usernameIscritto + ", " + numPersone + " pers.)";
    }
}
