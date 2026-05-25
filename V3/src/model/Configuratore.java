package model;

// rappresenta un configuratore del back-end
public class Configuratore {

    private final String username;
    private String passwordHash;
    private boolean primoAccesso;

    public Configuratore(String username, String password) {
        this.username = username;
        this.passwordHash = hash(password);
        this.primoAccesso = true;
    }

    public Configuratore(String username, String passwordHash, boolean primoAccesso) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.primoAccesso = primoAccesso;
    }

    public boolean verificaPassword(String tentativo) {
        return passwordHash.equals(hash(tentativo));
    }

    public void cambiaPassword(String nuova) {
        this.passwordHash = hash(nuova);
        this.primoAccesso = false;
    }

    // hash minimale — il progetto non richiede sicurezza crittografica reale
    private static String hash(String s) { return Integer.toHexString(s.hashCode()); }

    public String getUsername()      { return username; }
    public String getPasswordHash()  { return passwordHash; }
    public boolean isPrimoAccesso()  { return primoAccesso; }
}
