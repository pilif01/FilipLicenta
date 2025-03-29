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

import static org.example.Logger.logException;

public class MainUI extends JFrame {
    private static MainUI mainUIInstance;
    private static final String LOG_FILE = "OCR-log.txt";
    private JLabel greetingLabel;
    private Font promptFont;

    public MainUI(String username) {
        loadFont(); // Load the custom font

        log("Initializing OCR Tool UI");

        setTitle("OCR - TOOL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setResizable(false);

        // Greeting message
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        greetingLabel = new JLabel("Hi, " + username, SwingConstants.CENTER);
        greetingLabel.setFont(promptFont.deriveFont(Font.BOLD, 28));
        greetingLabel.setForeground(new Color(50, 50, 50));
        headerPanel.add(greetingLabel);

        // Main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.BOTH;

        // Define buttons and their descriptions
        String[][] buttonData = {
                {"Parse XML file", "Parse the TextWarnings.xml file", "buttonParseXML.png"},
                {"Create warning excel", "Create the Excel file for warnings", "buttonCreateWarningExcel.png"},
                {"Create image excel", "Generate the image Excel file", "buttonCreateImageExcel.png"},
                {"Manual Testing", "Check failed comparisons manually", "ManualTesting.png"},
                {"Manual Icon Testing", "Test icons manually", "ManualIconTesting.png"},
                {"Name Changing Tool", "Tool for changing names", "NameChangingTool.png"}
        };

        int row = 0;
        for (String[] data : buttonData) {
            gbc.gridx = row % 2;
            gbc.gridy = row / 2;
            gbc.gridwidth = 1;
            gbc.weightx = 0.5;
            mainPanel.add(createToolButton(data[0], data[1], data[2]), gbc);
            row++;
        }

        // Right panel for large buttons
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(300, 0));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        rightPanel.add(createLargeToolButton("Full auto icon testing", "Check all the Icons using pixel to pixel comparison", "FullAutoIconTesting.png"), gbc);

        gbc.gridy = 1;
        rightPanel.add(createLargeToolButton("Full auto warning testing", "Check all the texts using Tessaract", "FullAutoWarningTesting.png"), gbc);

        // Add panels to frame
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        mainUIInstance = this;

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log("Closing OCR Tool UI");
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

        // Load icon
        JLabel iconLabel = new JLabel();
        try {
            Image iconImage = ImageIO.read(new File("C:\\Licenta\\ExcelManager\\src\\main\\java\\org\\example\\pics\\" + iconName));
            Image scaledImage = iconImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledImage));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel labelText = new JLabel("<html><b>" + text + "</b><br><i>" + description + "</i></html>");
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

        // ADD ACTION LISTENER TO MAKE BUTTON WORK
        button.addActionListener(e -> {
            log("Clicked tool button: " + text);
            switch (text) {
                case "Parse XML file":
                    WrnzParseXMLFile.convertXMLToExcel();
                    break;
                case "Create warning excel":
                    WrnTextExcelTool.filterExcelFile();
                    break;
                case "Create image excel":
                    WrnImgExcelTool.filterExcelFile();
                    break;
                case "Manual Testing":
                    ManualWarningTestTool.main(new String[]{});
                    break;
                case "Manual Icon Testing":
                    ManualIconTestTool.main(new String[]{});
                    break;
                case "Name Changing Tool":
                    NameChangingTool.main(new String[]{});
                    break;
            }
        });

        return button;
    }

    private JButton createLargeToolButton(String text, String description, String iconName) {
        JButton button = createToolButton(text, description, iconName);
        // Remove the action listener added by createToolButton and add one for large buttons if needed,
        // or modify the switch cases to handle both cases.
        button.addActionListener(e -> {
            log("Clicked tool button: " + text);
            switch (text) {
                case "Full auto icon testing":
                    AutomatedIconTesting.main(new String[]{});
                    break;
                case "Full auto warning testing":
                    AutomatedWarningTesting.main(new String[]{});
                    break;
            }
        });
        return button;
    }

    private void loadFont() {
        try {
            promptFont = Font.createFont(Font.TRUETYPE_FONT, new File("C:\\Licenta\\ExcelManager\\src\\main\\java\\org\\example\\pics\\Prompt-Medium.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(promptFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            promptFont = new Font("Arial", Font.PLAIN, 16);  // Fallback to Arial if loading fails
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
            } catch (Exception e) {
                logException("Substance Graphite failed to initialize", e);
            }
            new MainUI("User").setVisible(true);
        });
    }
}
