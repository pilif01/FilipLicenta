package org.example;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

public class MenusTestUI extends JFrame {
    private static MenusTestUI mainUIInstance;
    private static final String LOG_FILE = "OCR-log.txt";
    private JLabel greetingLabel;
    private Font promptFont;

    public MenusTestUI(String username) {
        // Load custom font
        loadFont();
        log("Initializing Menus Test UI");

        // Frame settings
        setTitle("OCR - MENU TEST");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setResizable(false);

        // Header with Back button and greeting
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton backButton = new JButton("← Back");
        backButton.setFont(promptFont.deriveFont(Font.PLAIN, 16));
        backButton.setFocusPainted(false);
        backButton.setBackground(new Color(240, 240, 240));
        backButton.addActionListener(evt -> {
            log("Back button clicked – returning to initial menu");
            this.dispose();
            new InitialMenuUI(username).setVisible(true);
        });
        headerPanel.add(backButton, BorderLayout.WEST);

        greetingLabel = new JLabel("Hi, " + username, SwingConstants.CENTER);
        greetingLabel.setFont(promptFont.deriveFont(Font.BOLD, 28));
        greetingLabel.setForeground(new Color(50, 50, 50));
        headerPanel.add(greetingLabel, BorderLayout.CENTER);

        // Spacer to keep greeting centered
        headerPanel.add(Box.createRigidArea(backButton.getPreferredSize()), BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.BOTH;

        // Define buttons and their descriptions
        String[][] buttonData = {
            {"Parse Screens XML file",    "Parse the TextScreens.xml file",     "buttonParseXML.png"},
            {"Parse Units XML file",      "Parse the TextUnits.xml file",       "buttonParseXML.png"},
            {"Combine Excels",            "Generate the Menus Excel file",      "buttonCreateImageExcel.png"},
            {"Manual Testing",            "Check failed comparisons manually",  "ManualTesting.png"},
            {"Generate coordinates",      "Generate coordinates for testing",   "ManualIconTesting.png"},
            {"Name Changing Tool",        "Tool for changing names",            "NameChangingTool.png"}
        };

        int row = 0;
        for (String[] data : buttonData) {
            gbc.gridx = row % 2;
            gbc.gridy = row / 2;
            gbc.weightx = 0.5;
            mainPanel.add(createToolButton(data[0], data[1], data[2]), gbc);
            row++;
        }

        // Right panel for large buttons
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(300, 0));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        rightPanel.add(createLargeToolButton(
            "Create final excel for testing",
            "Combines texts excel with coordinates excel",
            "FullAutoIconTesting.png"
        ), gbc);
        gbc.gridy = 1;
        rightPanel.add(createLargeToolButton(
            "Full auto menu testing",
            "Check all texts using Tesseract",
            "FullAutoWarningTesting.png"
        ), gbc);

        add(mainPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        mainUIInstance = this;

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log("Closing Menus Test UI");
                System.exit(0);
            }
        });
    }

    private JButton createToolButton(String text, String description, String iconName) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setPreferredSize(new Dimension(250, 100));
        Color base = new Color(251, 67, 8);
        button.setBackground(base);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);

        JLabel iconLabel = new JLabel();
        try {
            Image iconImage = ImageIO.read(new File(
                "/org/example/pics/" + iconName
            ));
            Image scaledImage = iconImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledImage));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel labelText = new JLabel(String.format(
            "<html><b>%s</b><br><i>%s</i></html>", text, description
        ));
        labelText.setFont(promptFont.deriveFont(Font.PLAIN, 16));
        labelText.setForeground(Color.WHITE);
        textPanel.add(labelText, BorderLayout.CENTER);

        button.add(iconLabel, BorderLayout.WEST);
        button.add(textPanel, BorderLayout.CENTER);

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(93, 0, 179));
                labelText.setForeground(Color.BLACK);
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(base);
                labelText.setForeground(Color.WHITE);
            }
        });

        button.addActionListener(e -> {
            log("Clicked tool button: " + text);
            switch (text) {
                case "Parse Screens XML file":    MenuTextScreens.convertXMLToExcel();       break;
                case "Parse Units XML file":      MenuTextUnits.convertXMLToExcel();       break;
                case "Combine Excels":            ExcelCombiner.main(new String[]{});     break;
                case "Manual Testing":            ManualMenuTestTool.main(new String[]{}); break;
                case "Generate coordinates":      CoordinateMaker.main(new String[]{});       break;
                case "Name Changing Tool":        NameChangingTool.main(new String[]{});     break;
            }
        });
        return button;
    }

    private JButton createLargeToolButton(String text, String description, String iconName) {
        JButton button = createToolButton(text, description, iconName);
        button.addActionListener(e -> {
            log("Clicked tool button: " + text);
            switch (text) {
                case "Create final excel for testing": ExcelProcessor.main(new String[]{}); break;
                case "Full auto menu testing":        MenuFullAuto.main(new String[]{}); break;
            }
        });
        return button;
    }

    private void loadFont() {
        try {
            promptFont = Font.createFont(
                Font.TRUETYPE_FONT,
                new File("/org/example/pics/Prompt-Medium.ttf")
            );
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(promptFont);
        } catch (Exception e) {
            e.printStackTrace();
            promptFont = new Font("Arial", Font.PLAIN, 16);
        }
    }

    public static void log(String message) {
        System.out.println(message);
        writeToFile(getCurrentTimestamp() + " - " + message);
    }

    private static void writeToFile(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
