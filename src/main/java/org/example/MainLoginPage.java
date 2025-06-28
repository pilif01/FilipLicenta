package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.imageio.ImageIO;

public class MainLoginPage extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private JLabel statusLabel;

    public MainLoginPage() {
        setTitle("Login Page");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Load Icon
        JLabel iconLabel = new JLabel();
        try {
            InputStream is = getClass().getResourceAsStream("/org/example/pics/OCR_Tool_Icon.png");
            if (is != null) {
                BufferedImage iconImage = ImageIO.read(is);
                Image scaledImage = iconImage.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                System.err.println("Image not found in resources!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10); // Reduced vertical insets for closer spacing
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
        mainPanel.add(iconLabel, gbc); // Adding the logo at the top

        gbc.gridy = 1;
        mainPanel.add(titleLabel, gbc);

        gbc.gridy = 2;
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridy = 3;
        mainPanel.add(usernameField, gbc);

        gbc.gridy = 4;
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridy = 5;
        mainPanel.add(passwordField, gbc);

        gbc.gridy = 6;
        mainPanel.add(loginButton, gbc);

        gbc.gridy = 7;
        mainPanel.add(registerButton, gbc);

        gbc.gridy = 8;
        mainPanel.add(statusLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        loginButton.addActionListener(this::handleLogin);
        registerButton.addActionListener(e -> {
            dispose();
            new MainRegisterPage().setVisible(true);
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
            statusLabel.setText("Database error. Using default fallback.");

            // Fallback user logic
            return username.equals("filip") && password.equals("123");
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainLoginPage().setVisible(true));
    }
}
