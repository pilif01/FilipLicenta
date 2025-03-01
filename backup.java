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

    private static void log(String message) {
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

    public MainUI() {
        log("Initializing OCR Tool UI");

        setTitle("OCR - TOOL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 800);

        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 2, 20, 20));

        buttonPanel.add(createToolButton("Parse XML file", "Parse the TextWarnings.xml file"));
        buttonPanel.add(createToolButton("Create warning excel", "Create the Excel file for warnings"));
        buttonPanel.add(createToolButton("Create image excel", "Generate the image Excel file"));
        buttonPanel.add(createToolButton("Manual Testing", "Check failed comparisons manually"));
        buttonPanel.add(createToolButton("Manual Icon Testing", "Test icons manually"));
        buttonPanel.add(createToolButton("Name Changing Tool", "Tool for changing names"));

        JPanel paddedPanel = new JPanel(new BorderLayout());
        paddedPanel.add(buttonPanel, BorderLayout.CENTER);
        paddedPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        getContentPane().add(paddedPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setResizable(false);

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
        button.setPreferredSize(new Dimension(250, 80));

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
                    ImgExcelTool.filterExcelFile();
                    break;
                case "Create image excel":
                    TextExcelTool.filterExcelFile();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
            } catch (Exception e) {
                logException("Substance Graphite failed to initialize", e);
            }
            new LoginPage().setVisible(true); // Start with login first
        });
    }
}
