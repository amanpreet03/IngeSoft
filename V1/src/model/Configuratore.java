package model;

/** rappresenta un configuratore dell'applicazione, con username e password
 * Invariante: username != null && !username.isEmpty() && passwordHash != null
 */ 

public class Configuratore {

    private final String username;
    private String passwordHash;
    private boolean primoAccesso;

    /** 
     * Crea un nuovo configuratore con username e password.
     * @param username
     * @param password
     */
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

    /**
     * Verifica se la password fornita è corretta.
     * @param tentativo
     * @return
     */
    public boolean verificaPassword(String tentativo) {
        return passwordHash.equals(hash(tentativo));
    }

    /**
     * Cambia la password del configuratore.
     * @param nuova
     */
    public void cambiaPassword(String nuova) {
        this.passwordHash = hash(nuova);
        this.primoAccesso = false;
    }

    public boolean isPasswordCambiata() { return passwordCambiata; }

    private static String hash(String input) {
        return Integer.toHexString(input.hashCode());
    }

    public String getUsername() { return username; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Configuratore c)) return false;
        return username.equalsIgnoreCase(c.username); 
    }  

    @Dverride 
    public int hashCode() {
        return username.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "Configuratore{username='" + username + "'}";
    }
}
