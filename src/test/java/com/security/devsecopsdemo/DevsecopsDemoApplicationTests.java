package com.security.devsecopsdemo;

import org.junit.jupiter.api.Test;

import java.sql.*;

public class DevsecopsDemoApplicationTests {

    // ❌ FAILLE : SQL Injection
    public void unsafeQuery(String userInput) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "root", "password123");
        Statement stmt = conn.createStatement();
        String query = "SELECT * FROM users WHERE name = '" + userInput + "'"; // Vulnérable !
        ResultSet rs = stmt.executeQuery(query);
    }

    // ❌ FAILLE : Hardcoded credentials
    private static final String PASSWORD = "admin123"; // Secret exposé !

    // ✅ CORRECT : Prepared Statement
    public void safeQuery(String userInput) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "root", "password123");
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        pstmt.setString(1, userInput);
        ResultSet rs = pstmt.executeQuery();
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
