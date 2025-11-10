package com.security.devsecopsdemo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de tests et utilitaires montrant des patterns sûrs :
 *  - aucune concaténation directe d'input utilisateur dans les requêtes SQL
 *  - aucun secret hardcodé dans le code source
 *  - tests unitaires qui valident la construction des requêtes paramétrées
 */
public class DevsecopsDemoApplicationTests {

    /**
     * Ne stocke pas de mot de passe en dur. Récupère depuis la variable d'environnement
     * si présente (sinon renvoie une chaîne vide).
     * Dans CI/Jenkins : injecte ce secret via les credentials plutôt que de le hardcoder.
     */
    private String getDbPassword() {
        String pwd = System.getenv("DB_PASSWORD");
        return (pwd == null) ? "" : pwd;
    }

    /**
     * Représentation immuable d'une requête SQL paramétrée.
     * Permet de séparer la requête (avec placeholders) des paramètres réels.
     */
    public static final class ParameterizedQuery {
        private final String sql;
        private final Object[] params;

        public ParameterizedQuery(String sql, Object... params) {
            this.sql = sql;
            this.params = params == null ? new Object[0] : params.clone();
        }

        public String getSql() {
            return sql;
        }

        public Object[] getParams() {
            return params.clone();
        }
    }

    /**
     * Méthode qui construit une requête SQL paramétrée en utilisant des placeholders.
     * Ne concatène jamais userInput dans la chaîne SQL.
     */
    public ParameterizedQuery buildSafeQuery(String userInput) {
        // Utilisation d'un placeholder '?' pour éviter toute injection
        String sql = "SELECT id, name, email FROM users WHERE name = ?";
        return new ParameterizedQuery(sql, userInput);
    }

    /**
     * Exemple d'usage (documentation) montrant comment utiliser un PreparedStatement
     * réel en production. On le laisse ici comme pattern à suivre :
     *
     * try (PreparedStatement ps = connection.prepareStatement(sql)) {
     *    ps.setString(1, userInput);
     *    try (ResultSet rs = ps.executeQuery()) {
     *        ...
     *    }
     * }
     *
     * (Ne pas exécuter dans les tests unitaires sans vraie connexion.)
     */

    // ===========================
    // Tests unitaires
    // ===========================

    @Test
    public void testBuildSafeQuery_returnsPlaceholderAndParams() {
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        ParameterizedQuery pq = demo.buildSafeQuery("alice");

        // La SQL doit contenir le placeholder et ne doit pas contenir la valeur littérale "alice"
        assertEquals("SELECT id, name, email FROM users WHERE name = ?", pq.getSql());
        assertArrayEquals(new Object[]{"alice"}, pq.getParams());

        // S'assurer qu'il n'y ait pas de concaténation évidente
        assertFalse(pq.getSql().contains("alice"));
    }

    @Test
    public void testGetDbPassword_notHardcoded() {
        // Assure que la méthode ne renvoie pas une valeur hardcodée connue
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        String pwd = demo.getDbPassword();
        assertNotEquals("admin123", pwd, "Le mot de passe ne doit pas être hardcodé dans le code source");
    }

    @Test
    public void testParameterizedQuery_immutability() {
        ParameterizedQuery q = new ParameterizedQuery("SELECT ? FROM DUAL", "p1", "p2");
        Object[] params = q.getParams();

        // modification locale ne doit pas impacter l'objet original
        params[0] = "changed";
        Object[] originalParams = q.getParams();
        assertEquals("p1", originalParams[0], "Les params internes doivent rester immuables depuis l'extérieur");
    }

    @Test
    public void testBuildSafeQuery_withNullInput() {
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        ParameterizedQuery pq = demo.buildSafeQuery(null);
        assertEquals(1, pq.getParams().length);
        assertNull(pq.getParams()[0], "Lorsque l'input est null, le paramètre doit être null (handled)");
    }
}
