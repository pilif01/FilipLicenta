package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MainLoginPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private JLabel statusLabel;

    public MainLoginPage() {
        setTitle("Login Page");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel titleLabel = new JLabel("User Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));

        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));

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
        mainPanel.add(loginButton, gbc);

        gbc.gridy = 6;
        mainPanel.add(registerButton, gbc);

        gbc.gridy = 7;
        mainPanel.add(statusLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        loginButton.addActionListener(this::handleLogin);
        registerButton.addActionListener(e -> {
            dispose(); // Close Login Page
            new MainRegisterPage().setVisible(true); // Open Register Page
        });
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (authenticate(username, password)) {
            statusLabel.setForeground(Color.GREEN);
            statusLabel.setText("Login successful!");
            dispose();
            SwingUtilities.invokeLater(() -> new MainUI(username).setVisible(true));
        } else {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Invalid credentials.");
        }
    }

    private boolean authenticate(String username, String password) {
        try (Connection conn = MainDBConnection.getConnection()) {
            String query = "SELECT * FROM users WHERE username=? AND password=SHA2(?, 256)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Database connection error.");
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainLoginPage().setVisible(true));
    }
}
