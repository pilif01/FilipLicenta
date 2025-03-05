package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RegisterPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton registerButton, loginButton;
    private JLabel statusLabel;

    public RegisterPage() {
        setTitle("Register Page");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Main panel layout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel titleLabel = new JLabel("User Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));

        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));

        registerButton = new JButton("Register");
        loginButton = new JButton("Login");
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        // Add components to panel
        gbc.gridy = 0;
        mainPanel.add(titleLabel, gbc);

        gbc.gridy = 1;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridy = 2;
        mainPanel.add(usernameField, gbc);

        gbc.gridy = 3;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridy = 4;
        mainPanel.add(passwordField, gbc);

        gbc.gridy = 5;
        mainPanel.add(registerButton, gbc);

        gbc.gridy = 6;
        mainPanel.add(loginButton, gbc);

        gbc.gridy = 7;
        mainPanel.add(statusLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button actions
        registerButton.addActionListener(this::handleRegister);
        loginButton.addActionListener(e -> {
            dispose(); // Close Register Page
            new LoginPage().setVisible(true); // Open Login Page
        });
    }

    private void handleRegister(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (registerUser(username, password)) {
            statusLabel.setForeground(Color.GREEN);
            statusLabel.setText("Registration successful! Please login.");
        } else {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Username already exists.");
        }
    }

    private boolean registerUser(String username, String password) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check if the username exists
            String checkQuery = "SELECT * FROM users WHERE username=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) return false;

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, SHA2(?, 256))";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.executeUpdate();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Database connection error.");
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterPage().setVisible(true));
    }
}
