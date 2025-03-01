package org.example;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.example.Logger.logException;

public class MainUI extends JFrame {
    private static MainUI mainUIInstance;

    // Logging
    private static final String LOG_FILE = "OCR-log.txt";

    // Logging method to write to the log file
    private static void log(String message) {
        System.out.println(message);
        writeToFile(getCurrentTimestamp() + " - " + message);
    }

    private static void writeToFile(String message) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true));
            writer.println(message);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    private JPanel mainPanel;

    public MainUI() {
        // Log initialization
        log("Initializing OCR Tool UI");

        setTitle("OCR - TOOL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 1200);

        // Set custom window decoration
        setUndecorated(true);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        // Set custom icon for the application
        try {
            InputStream iconStream = getClass().getClassLoader().getResourceAsStream("logo3.png");
            if (iconStream != null) {
                Image iconImage = ImageIO.read(iconStream);
                setIconImage(iconImage);
            } else {
                log("Icon file not found using class loader: logo3.png");
                File iconFile = new File("logo3.png");
                if (iconFile.exists()) {
                    Image iconImage = ImageIO.read(iconFile);
                    setIconImage(iconImage);
                } else {
                    log("Icon file not found using relative path: logo3.png");
                }
            }
        } catch (IOException e) {
            logException("Error loading icon", e);
        }

        // Log creating a menu bar
        log("Creating menu bar");
        JMenuBar menuBar = new JMenuBar();
        createMenuBar(menuBar);
        setJMenuBar(menuBar);

        // Set layout manager for the main frame
        setLayout(new BorderLayout());

        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create and add buttons
        JButton parseXmlButton = createToolButton("Parse XML file", "Used to parse the TextWarnings.xml file");
        JButton createWrnExcelButton = createToolButton("Create warning excel", "Used to create the excel file for manual warning testing");
        JButton createImgExcelButton = createToolButton("Create image excel", "Used to create the excel file for manual warning testing");
        JButton manualTestingButton = createToolButton("Manual Testing", "Used to check manually the failed comparisons");
        JButton manualIconTestingButton = createToolButton("Manual Icon Testing", "Test icons manually");
        JButton nameChangingToolButton = createToolButton("Name Changing Tool", "Tool for changing names");

        buttonPanel.add(Box.createVerticalGlue()); // Add spacing at the top
        buttonPanel.add(parseXmlButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add spacing
        buttonPanel.add(createWrnExcelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add spacing
        buttonPanel.add(createImgExcelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add spacing
        buttonPanel.add(manualTestingButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add spacing
        buttonPanel.add(manualIconTestingButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add spacing
        buttonPanel.add(nameChangingToolButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add spacing
        buttonPanel.add(Box.createVerticalGlue()); // Add spacing at the bottom

        // Add the button panel to the center
        getContentPane().add(buttonPanel, BorderLayout.CENTER);

        // Add an image to the bottom of the UI
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("conti4.png");
            if (imageStream != null) {
                Image image = ImageIO.read(imageStream);
                imageLabel.setIcon(new ImageIcon(image));
            } else {
                log("Image file not found using class loader: conti4.png");
                File imageFile = new File("conti4.png");
                if (imageFile.exists()) {
                    Image image = ImageIO.read(imageFile);
                    imageLabel.setIcon(new ImageIcon(image));
                } else {
                    log("Image file not found using relative path: conti4.png");
                }
            }
        } catch (IOException e) {
            logException("Error loading image", e);
        }

        //scroll
        JScrollPane scrollPane = new JScrollPane(buttonPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Set faster scrolling speed
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(30); // Increase this value for faster scrolling
        verticalScrollBar.setBlockIncrement(60);

        getContentPane().add(scrollPane, BorderLayout.CENTER);


        getContentPane().add(imageLabel, BorderLayout.SOUTH);

        setResizable(false);
        mainUIInstance = this;

        // Add a listener to handle the close button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log("Closing OCR Tool UI");
                System.exit(0);
            }
        });
    }

    private void createMenuBar(JMenuBar menuBar) {
        createFileMenu(menuBar);
        createHelpMenu(menuBar);
    }

    private void createFileMenu(JMenuBar menuBar) {
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitMenuItem = createMenuItem("Exit", this::handleExitAction);
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
    }

    private void createHelpMenu(JMenuBar menuBar) {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = createMenuItem("About", this::handleAboutAction);
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
    }

    private JMenuItem createMenuItem(String label, ActionListener actionListener) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.addActionListener(actionListener);
        return menuItem;
    }

    private void handleExitAction(ActionEvent event) {
        log("Exit action clicked");
        System.exit(0);
    }

    private void handleAboutAction(ActionEvent event) {
        log("About action clicked");
        JOptionPane.showMessageDialog(this, "OCR Tool Version 1.0", "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private JButton createToolButton(String text, String description) {
        JButton button = new JButton("<html><center>" + text + "<br><i>" + description + "</i></center></html>");
        button.setFont(new Font("Arial", Font.PLAIN, 18));
        button.setPreferredSize(new Dimension(450, 100));

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

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log("Clicked tool button: " + text);
                switch (text) {
                    case "Parse XML file":
                        launchParseXMLTool();
                        break;

                    case "Create warning excel":
                        launchWrnExcelTool();
                        break;

                    case "Create image excel":
                        launchImgExcelTool();
                        break;

                    case "Manual Testing":
                        launchManualTestingTool();
                        break;

                    case "Manual Icon Testing":
                        launchManualIconTestingTool();
                        break;

                    case "Name Changing Tool":
                        launchNameChangingTool();
                        break;
                }
            }
        });
        return button;
    }

    private void launchParseXMLTool() {
        log("Parse XML Tool");
        SwingUtilities.invokeLater(() -> {
            mainUIInstance.setVisible(false);
            log("Opening XML Parsing Tool");
            ParseXMLFile.main(new String[]{});
            mainUIInstance.setVisible(true);
        });
    }

    private void launchWrnExcelTool() {
        log("Warning Excel Tool");
        SwingUtilities.invokeLater(() -> {
            mainUIInstance.setVisible(false);
            log("Opening Warning Excel Tool");
            //include logic here
            mainUIInstance.setVisible(true);
        });
    }

    private void launchImgExcelTool() {
        log("Image Excel Tool");
        SwingUtilities.invokeLater(() -> {
            mainUIInstance.setVisible(false);
            log("Opening Image Excel Tool");
            //include logic here
            mainUIInstance.setVisible(true);
        });
    }

    private void launchManualTestingTool() {
        log("Manual Testing Tool");
        SwingUtilities.invokeLater(() -> {
            mainUIInstance.setVisible(false);
            log("Opening ImageExplorer for Manual Testing");
            ImageExplorerApp.main(new String[]{});
            mainUIInstance.setVisible(true);
        });
    }

    private void launchManualIconTestingTool() {
        log("Manual Icon Testing Tool");
        SwingUtilities.invokeLater(() -> {
            mainUIInstance.setVisible(false);
            log("Opening Manual Icon Testing Tool");
            ManualIconTestTool.main(new String[]{});
            mainUIInstance.setVisible(true);
        });
    }

    private void launchNameChangingTool() {
        log("Name Changing Tool");
        SwingUtilities.invokeLater(() -> {
            mainUIInstance.setVisible(false);
            log("Opening Name Changing Tool");
            NameChangingTool.main(new String[]{});
            mainUIInstance.setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
            } catch (Exception e) {
                logException("Substance Graphite failed to initialize", e);
            }
            new MainUI().setVisible(true);
        });
    }
}
