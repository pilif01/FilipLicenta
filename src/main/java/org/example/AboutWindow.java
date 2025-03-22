package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.example.Logger.logException;


public class AboutWindow {

    private JFrame aboutFrame;

    public AboutWindow(JFrame parentFrame) {
        aboutFrame = new JFrame("About");
        aboutFrame.setSize(800, 600);
        aboutFrame.setResizable(false);
        aboutFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        aboutFrame.setLocationRelativeTo(parentFrame);

        // Create a panel for the content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());

        // Title Label
        JLabel titleLabel = new JLabel("OCR - Application");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Description Label
        JLabel aboutLabel = new JLabel("Made with <3 by Bulc Filip");
        aboutLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(aboutLabel, BorderLayout.CENTER);

        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(500, 200));

        // Placeholder for Picture
        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("conti4.png");
            if (imageStream != null) {
                Image iconImage = ImageIO.read(imageStream);
                imageLabel.setIcon(new ImageIcon(iconImage));
            } else {
                // Log image not found using class loader
                //logException("Image file not found using class loader: conti4.png");

                // Try loading using a relative path
                File imageFile = new File("conti4.png"); // Replace with the actual relative path
                if (imageFile.exists()) {
                    Image iconImage = ImageIO.read(imageFile);
                    imageLabel.setIcon(new ImageIcon(iconImage));
                } else {
                    // Log image not found using relative path
                    //logException("Image file not found using relative path: conti4.png");
                }
            }
        } catch (IOException e) {
            // Log exception if there's an error loading the image
            logException("Error loading image", e);
        }
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(imageLabel, BorderLayout.SOUTH);

        aboutFrame.add(contentPanel);
    }

    public void showAboutWindow() {
        aboutFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame testFrame = new JFrame("About Section");
            testFrame.setSize(800, 600);
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JButton aboutButton = new JButton("Show About");
            aboutButton.addActionListener(e -> {
                AboutWindow aboutWindow = new AboutWindow(testFrame);
                aboutWindow.showAboutWindow();
            });

            testFrame.add(aboutButton);
            testFrame.setVisible(true);
        });
    }
}
