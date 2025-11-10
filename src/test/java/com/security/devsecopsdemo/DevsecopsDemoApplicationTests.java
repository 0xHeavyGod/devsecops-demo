package com.security.devsecopsdemo;

import org.junit.jupiter.api.Test;

import java.sql.*;

public class DevsecopsDemoApplicationTests {

    // ❌ FAILLE : SQL Injection
    public void unsafeQuery(String userInput) throws SQLException {
        // ⚠️ Simulation d'une requête vulnérable (pas de vraie connexion)
        String query = "SELECT * FROM users WHERE name = '" + userInput + "'";
        System.out.println("Executing unsafe query: " + query);
    }

    // ❌ FAILLE : Hardcoded credentials
    private static final String PASSWORD = "admin123"; // Secret exposé !

    // ✅ CORRECT : Prepared Statement
    public void safeQuery(String userInput) throws SQLException {
        // ⚙️ Simulation de requête sécurisée
        String query = "SELECT * FROM users WHERE name = ?";
        System.out.println("Executing safe query: " + query + " with userInput=" + userInput);
    }

    // ===========================
    // Tests unitaires
    // ===========================
    @Test
    public void testUnsafeQuery() {
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        try {
            demo.unsafeQuery("alice");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSafeQuery() {
        DevsecopsDemoApplicationTests demo = new DevsecopsDemoApplicationTests();
        try {
            demo.safeQuery("alice");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
