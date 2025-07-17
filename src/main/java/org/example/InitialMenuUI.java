// InitialMenuUI.java
package org.example;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import javax.swing.*;
import java.awt.*;

import static org.example.Logger.logException;

public class InitialMenuUI extends JFrame {
    public InitialMenuUI(String username) {
        // Apply Substance Graphite Look & Feel
        try {
            UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
            // After changing L&F, update this frame’s UI
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            Logger.logException("Substance Graphite failed to initialize in InitialMenuUI", e);
        }

        setTitle("OCR - Select Test Mode");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton btnWarnings = new JButton("Test Warnings");
        JButton btnMenus    = new JButton("Test Menus");

        btnWarnings.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new MainUI(username).setVisible(true));
        });

        btnMenus.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new MenusTestUI(username).setVisible(true));
        });

        panel.add(btnWarnings);
        panel.add(btnMenus);
        add(panel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
            } catch (Exception e) {
                logException("Substance Graphite failed to initialize", e);
            }
            new InitialMenuUI("Default User").setVisible(true);  // Directly show MainUI
        });
    }
    
}
