/*package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class RegisterPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton registerButton;
    private JLabel statusLabel;

    public RegisterPage() {
        setTitle("Register");
        setSize(350, 200);
        setLayout(new GridLayout(4, 1));

        JPanel userPanel = new JPanel();
        userPanel.setLayout(new FlowLayout());
        userPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(15);
        userPanel.add(usernameField);

        JPanel passPanel = new JPanel();
        passPanel.setLayout(new FlowLayout());
        passPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField(15);
        passPanel.add(passwordField);

        registerButton = new JButton("Register");
        registerButton.addActionListener(this::handleRegister);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        add(userPanel);
        add(passPanel);
        add(registerButton);
        add(statusLabel);

        setLocationRelativeTo(null);
    }

    private void handleRegister(ActionEvent e) {
        String username = usernameField.getText();
        String password = hashPassword(new String(passwordField.getPassword()));

        if (registerUser(username, password)) {
            statusLabel.setForeground(Color.GREEN);
            statusLabel.setText("Registration successful! Please login.");
        } else {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Username already exists or error occurred.");
        }
    }

    private boolean registerUser(String username, String hashedPassword) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load MySQL JDBC Driver
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/login_system?useSSL=false&allowPublicKeyRetrieval=true", "root", "yourpassword");


            // Check if username already exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return false; // Username already exists

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.executeUpdate();

            return true; // Successfully registered
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Database connection error.");
            return false;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Error hashing password", ex);
        }
    }
}


 */