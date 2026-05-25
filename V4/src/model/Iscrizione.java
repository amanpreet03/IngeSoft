package model;

import java.util.UUID;

// prenotazione di un fruitore a una visita proposta
public class Iscrizione {

    private final String codice;
    private final String usernameFruitore;
    private final int numPersone;

    public Iscrizione(String usernameFruitore, int numPersone) {
        // i primi 8 caratteri dell'UUID bastano come codice leggibile
        this.codice = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.usernameFruitore = usernameFruitore;
        this.numPersone = numPersone;
    }

    // costruttore usato dalla persistenza per ricostruire un'iscrizione già esistente
    public Iscrizione(String codice, String usernameFruitore, int numPersone) {
        this.codice = codice;
        this.usernameFruitore = usernameFruitore;
        this.numPersone = numPersone;
    }

    public String getCodice()           { return codice; }
    public String getUsernameFruitore() { return usernameFruitore; }
    public int getNumPersone()          { return numPersone; }

    @Override
    public String toString() {
        return codice + " (" + usernameFruitore + ", " + numPersone + " pers.)";
    }
}
