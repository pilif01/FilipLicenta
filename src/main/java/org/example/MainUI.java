package org.example;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.example.Logger.logException;

public class MainUI extends JFrame {
    private static MainUI mainUIInstance;
    private static final String LOG_FILE = "OCR-log.txt";
    private JLabel greetingLabel;

    public MainUI(String username) {
        log("Initializing OCR Tool UI");

        setTitle("OCR - TOOL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700); // Adjusted window size
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setResizable(false);

        // greeting message
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        greetingLabel = new JLabel("Hi, " + username, SwingConstants.CENTER);
        greetingLabel.setFont(new Font("Arial", Font.BOLD, 28));
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
                {"Parse XML file", "Parse the TextWarnings.xml file"},
                {"Create warning excel", "Create the Excel file for warnings"},
                {"Create image excel", "Generate the image Excel file"},
                {"Manual Testing", "Check failed comparisons manually"},
                {"Manual Icon Testing", "Test icons manually"},
                {"Name Changing Tool", "Tool for changing names"}
        };

        int row = 0;
        for (String[] data : buttonData) {
            gbc.gridx = row % 2;
            gbc.gridy = row / 2;
            gbc.gridwidth = 1;
            gbc.weightx = 0.5;
            mainPanel.add(createToolButton(data[0], data[1]), gbc);
            row++;
        }

        // Right panel for large buttons
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(300, 0));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        rightPanel.add(createLargeToolButton("Full auto icon testing", "..."), gbc);

        gbc.gridy = 1;
        rightPanel.add(createLargeToolButton("Full auto warning testing", "..."), gbc);

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

    private JButton createToolButton(String text, String description) {
        JButton button = new JButton("<html><center>" + text + "<br><i>" + description + "</i></center></html>");
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        button.setPreferredSize(new Dimension(250, 100));

        Color brighterOrangeYellow = new Color(255, 200, 0);
        button.setBackground(brighterOrangeYellow);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(100, 149, 237));
                button.setForeground(Color.BLACK);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(brighterOrangeYellow);
                button.setForeground(Color.WHITE);
            }
        });

        button.addActionListener(e -> {
            log("Clicked tool button: " + text);
            switch (text) {
                case "Parse XML file":
                    ParseXMLFile.convertXMLToExcel();
                    break;
                case "Create warning excel":
                    TextExcelTool.filterExcelFile();
                    break;
                case "Create image excel":
                    ImgExcelTool.filterExcelFile();
                    break;
                case "Manual Testing":
                    ImageExplorerApp.main(new String[]{});
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

    private JButton createLargeToolButton(String text, String description) {
        JButton button = new JButton("<html><center>" + text + "<br><i>" + description + "</i></center></html>");
        button.setFont(new Font("Arial", Font.PLAIN, 20));
        button.setPreferredSize(new Dimension(250, 150));

        Color brighterOrangeYellow = new Color(255, 200, 0);
        button.setBackground(brighterOrangeYellow);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(100, 149, 237));
                button.setForeground(Color.BLACK);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(brighterOrangeYellow);
                button.setForeground(Color.WHITE);
            }
        });

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
            new LoginPage().setVisible(true);  // Show the LoginPage first
        });
    }

}