/*package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class LoginPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private JLabel statusLabel;

    public LoginPage() {
        setTitle("Login Page");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        loginButton.addActionListener(this::handleLogin);
        registerButton.addActionListener(e -> new RegisterPage().setVisible(true)); // Open register page

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        add(userPanel);
        add(passPanel);
        add(loginButton);
        add(registerButton);
        add(statusLabel);

        setLocationRelativeTo(null);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = hashPassword(new String(passwordField.getPassword()));

        if (authenticate(username, password)) {
            statusLabel.setForeground(Color.GREEN);
            statusLabel.setText("Login successful!");
            dispose(); // Close login window
            SwingUtilities.invokeLater(() -> new MainUI().setVisible(true)); // Open main UI
        } else {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Invalid credentials. Try again.");
        }
    }

    private boolean authenticate(String username, String hashedPassword) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load MySQL JDBC Driver
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/login_system?useSSL=false&allowPublicKeyRetrieval=true", "root", "yourpassword");


            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // If a row is returned, credentials are valid
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}


 */