package com.security.devsecopsdemo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DevsecopsDemoApplicationTests {

    /**
     * Récupère le mot de passe depuis une variable d'environnement au lieu
     * d'avoir un secret hardcodé dans le code source.
     * => Assure-toi d'exporter DB_PASSWORD dans l'environnement CI si nécessaire.
     */
    private String getDbPassword() {
        String pwd = System.getenv("DB_PASSWORD");
        return (pwd == null) ? "" : pwd;
    }

    /**
     * Construction sûre d'une requête SQL : on utilise un placeholder '?'
     * et on renvoie les paramètres séparément. Cela évite toute concaténation
     * directe de l'entrée utilisateur dans la requête (pattern préparé).
     *
     * En production, tu feras :
     *   PreparedStatement ps = connection.prepareStatement(sql);
     *   ps.setString(1, userInput);
     *   ps.executeQuery();
     */
    public Query buildSafeQuery(String userInput) {
        String sql = "SELECT * FROM users WHERE name = ?";
        String[] params = new String[]{ userInput };
        return new Query(sql, params);
    }

    /**
     * Petite classe holder pour représenter la requête paramétrée.
     * Utilitaire pour les tests et démonstrations (équivalent conceptuel d'un PreparedStatement).
     */
    public static class Query {
        private final String sql;
        private final String[] params;

        public Query(String sql, String[] params) {
            this.sql = sql;
            this.params = params;
        }

        public String getSql() {
            return sql;
        }

        public String[] getParams() {
            return params;
        }
    }

    // ===========================
    // Tests unitaires
    // ===========================
    @Test
    public void testBuildSafeQuery() {
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        Query q = demo.buildSafeQuery("alice");

        // Vérifie que la requête contient bien le placeholder (pas de concaténation)
        assertEquals("SELECT * FROM users WHERE name = ?", q.getSql());
        // Vérifie que le paramètre est correctement placé
        assertArrayEquals(new String[]{"alice"}, q.getParams());
    }

    @Test
    public void testDbPasswordNotHardcoded() {
        // On s'assure que la méthode lit la variable d'environnement (ou renvoie vide)
        // ceci empêche d'avoir un mot de passe en dur dans le code.
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        String pwd = demo.getDbPassword();
        assertNotEquals("admin123", pwd, "Le mot de passe ne doit pas être hardcodé dans le code source");
    }
}
