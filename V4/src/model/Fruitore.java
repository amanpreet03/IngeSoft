package model;

// utente che si iscrive alle visite guidate 
public class Fruitore {

    private final String username;
    private String passwordHash;

    // il fruitore sceglie subito le sue credenziali alla registrazione: nessun primo accesso
    public Fruitore(String username, String password) {
        this.username = username;
        this.passwordHash = hash(password);
    }

    public Fruitore(String username, String passwordHash, boolean ignorato) {
        // costruttore usato dalla persistenza che ha già l'hash
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public boolean verificaPassword(String tentativo) {
        return passwordHash.equals(hash(tentativo));
    }

    public void cambiaPassword(String nuova) { this.passwordHash = hash(nuova); }

    private static String hash(String s) { return Integer.toHexString(s.hashCode()); }

    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fruitore f && username.equalsIgnoreCase(f.username);
    }

    @Override
    public int hashCode() { return username.toLowerCase().hashCode(); }
}
