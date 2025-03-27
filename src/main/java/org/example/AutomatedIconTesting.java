package org.example;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.awt.Rectangle;

public class AutomatedIconTesting extends JDialog {

    private JTextField excelPathField, iconExcelField, refIconExcelField, cropAreaField, resultExcelField;
    private JButton startButton, selectExcelButton, selectIconExcelButton, selectRefIconExcelButton, selectResultsButton, selectCropAreaButton;
    private JButton singleTubeButton, twinTubeButton;
    private Rectangle selectedArea;
    private BufferedImage referenceImage; // for the icon reference image
    private JTextArea logTextArea;
    private List<WarningIconData> iconData;

    public AutomatedIconTesting(JFrame parent) {
        super(parent, "Automated Icon Testing", true);

        setSize(800, 650);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int labelWidth = 250;
        int textWidth = 350;
        int buttonWidth = 100;
        int y = 20;
        int gap = 40;

        // Excel file selection (Input Icon Excel)
        JLabel selectExcelLabel = new JLabel("Select Icon Excel File:");
        selectExcelLabel.setBounds(20, y, labelWidth, 30);
        add(selectExcelLabel);

        iconExcelField = new JTextField();
        iconExcelField.setBounds(20, y + 30, textWidth, 30);
        add(iconExcelField);

        selectIconExcelButton = new JButton("Browse");
        selectIconExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectIconExcelButton);

        y += gap + 30;

        // Reference Icon Excel file selection
        JLabel selectRefIconExcelLabel = new JLabel("Select Reference Icon Excel File:");
        selectRefIconExcelLabel.setBounds(20, y, labelWidth, 30);
        add(selectRefIconExcelLabel);

        refIconExcelField = new JTextField();
        refIconExcelField.setBounds(20, y + 30, textWidth, 30);
        add(refIconExcelField);

        selectRefIconExcelButton = new JButton("Browse");
        selectRefIconExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectRefIconExcelButton);

        y += gap + 30;

        // Crop area field and selection button
        JLabel cropAreaLabel = new JLabel("Crop Area (x, y, width, height):");
        cropAreaLabel.setBounds(20, y, labelWidth, 30);
        add(cropAreaLabel);

        cropAreaField = new JTextField();
        cropAreaField.setBounds(20, y + 30, 250, 30);
        add(cropAreaField);

        selectCropAreaButton = new JButton("Select Area");
        selectCropAreaButton.setBounds(280, y + 30, 120, 30);
        add(selectCropAreaButton);

        y += gap + 30;

        // Results Excel file save location
        JLabel selectResultsLabel = new JLabel("Select Results Excel Save Location:");
        selectResultsLabel.setBounds(20, y, labelWidth, 30);
        add(selectResultsLabel);

        resultExcelField = new JTextField();
        resultExcelField.setBounds(20, y + 30, textWidth, 30);
        add(resultExcelField);

        selectResultsButton = new JButton("Browse");
        selectResultsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectResultsButton);

        y += gap + 30;

        // Buttons for selecting reference images (Single/Twin)
        singleTubeButton = new JButton("Single Tube");
        singleTubeButton.setBounds(20, y, 140, 30);
        add(singleTubeButton);

        twinTubeButton = new JButton("Twin Tube");
        twinTubeButton.setBounds(180, y, 140, 30);
        add(twinTubeButton);

        y += gap + 70;

        // Start Testing button
        startButton = new JButton("Start Testing");
        startButton.setBounds(200, y, 150, 40);
        add(startButton);

        // Add a log area at the bottom
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBounds(20, 550, 740, 80);
        add(logScrollPane);

        // Action Listeners for file/folder selections
        selectIconExcelButton.addActionListener(e -> selectFile(iconExcelField));
        selectRefIconExcelButton.addActionListener(e -> selectFile(refIconExcelField));
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultExcelField));

        // When "Select Area" is pressed, open the crop dialog (if an icon image is loaded)
        selectCropAreaButton.addActionListener(e -> {
            if (referenceImage != null) {
                openCropAreaDialog();
            } else {
                JOptionPane.showMessageDialog(this, "Please select Single or Twin Tube reference first.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Load reference image when one of the tube buttons is clicked
        singleTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("SINGLE");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        twinTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("TWIN");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        startButton.addActionListener(e -> startTesting());
    }

    // Utility method to append messages to the log text area
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> logTextArea.append(message + "\n"));
    }

    // Opens a file chooser to select a file
    private void selectFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Opens a file chooser to select a save location for a file
    private void selectFileSaveLocation(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Loads the reference image based on the selected type and opens the crop area dialog
    private void loadReferenceImage(String type) throws IOException {
        String basePath = "C:\\Licenta\\ExcelManager\\src\\main\\java\\org\\example\\pics";
        String imagePath = basePath + "\\" + "Reference_" + (type.equals("SINGLE") ? "SINGLE" : "TWIN") + ".png";
        referenceImage = ImageIO.read(new File(imagePath));
        openCropAreaDialog();
    }

    // Opens a dialog that displays the reference image and allows the user to select a crop area
    private void openCropAreaDialog() {
        CropAreaDialog dialog = new CropAreaDialog(this, referenceImage);
        dialog.setVisible(true);
        Rectangle area = dialog.getSelectedArea();
        if (area != null) {
            selectedArea = area;
            cropAreaField.setText(area.x + ", " + area.y + ", " + area.width + ", " + area.height);
        }
    }

    // Main logic to start the testing
    private void startTesting() {
        // Paths to the input and reference icon Excel files
        String iconExcelPath = iconExcelField.getText();
        String refIconExcelPath = refIconExcelField.getText();
        String resultsExcelPath = resultExcelField.getText().trim();

        try {
            // Read the input icon Excel (contains the warning names and associated icon names)
            FileInputStream iconFis = new FileInputStream(new File(iconExcelPath));
            XSSFWorkbook iconWorkbook = new XSSFWorkbook(iconFis);
            Sheet iconSheet = iconWorkbook.getSheetAt(0);

            iconData = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= iconSheet.getLastRowNum(); rowIndex++) {
                Row row = iconSheet.getRow(rowIndex);
                if (row != null) {
                    String warningName = row.getCell(1).toString().trim();  // Warning name
                    String iconName = row.getCell(2).toString().trim();    // Icon name
                    String tControl = row.getCell(0).toString().trim();     // Control (RUN or SKIP)

                    // Add the data to the list
                    iconData.add(new WarningIconData(warningName, iconName, tControl));
                }
            }

            // Read the reference icon Excel (contains the icon names and file paths to the reference icon images)
            FileInputStream refFis = new FileInputStream(new File(refIconExcelPath));
            XSSFWorkbook refIconWorkbook = new XSSFWorkbook(refFis);
            Sheet refIconSheet = refIconWorkbook.getSheetAt(0);

            // Process each warning and compare the icon images
            for (WarningIconData data : iconData) {
                if ("RUN".equalsIgnoreCase(data.getTcontrol())) {
                    // Get the corresponding reference icon
                    BufferedImage referenceIcon = null;
                    for (int rowIndex = 1; rowIndex <= refIconSheet.getLastRowNum(); rowIndex++) {
                        Row row = refIconSheet.getRow(rowIndex);
                        if (row != null && data.getIconName().equals(row.getCell(1).toString().trim())) {
                            // Find the matching reference icon based on the icon name
                            String referenceIconPath = "C:\\PathToReferenceIcons\\" + row.getCell(1).toString().trim() + ".png";
                            referenceIcon = ImageIO.read(new File(referenceIconPath));
                            break;
                        }
                    }

                    // If the reference icon is not found, log the error and skip comparison
                    if (referenceIcon == null) {
                        appendLog("Error: Reference icon for " + data.getIconName() + " not found.");
                        continue;
                    }

                    // Crop the selected area from the reference image
                    BufferedImage croppedImage = referenceImage.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);

                    // If cropped image is null, log the error and skip comparison
                    if (croppedImage == null) {
                        appendLog("Error: Cropped image for warning " + data.getWarningName() + " is null.");
                        continue;
                    }

                    // Compare the cropped warning image with the reference icon image
                    boolean match = compareImages(croppedImage, referenceIcon);

                    // Log the result
                    appendLog("Warning: " + data.getWarningName() + " - Icon Match: " + (match ? "PASSED" : "FAILED"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("Error: " + e.getMessage());
        }
    }

    // Method to compare two images pixel by pixel
    private boolean compareImages(BufferedImage img1, BufferedImage img2) {
        // Check if either image is null
        if (img1 == null || img2 == null) {
            appendLog("Error: One or both images are null.");
            return false;
        }

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            appendLog("Error: Image sizes do not match.");
            return false;
        }

        // Compare pixels one by one
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    appendLog("Mismatch found at (" + x + ", " + y + ")");
                    return false;
                }
            }
        }

        return true;
    }

    // Inner class to hold warning icon data
    class WarningIconData {
        private String warningName;
        private String iconName;
        private String tcontrol;

        public WarningIconData(String warningName, String iconName, String tcontrol) {
            this.warningName = warningName;
            this.iconName = iconName;
            this.tcontrol = tcontrol;
        }

        public String getWarningName() {
            return warningName;
        }

        public String getIconName() {
            return iconName;
        }

        public String getTcontrol() {
            return tcontrol;
        }
    }

    // Crop area dialog for selecting the area to test on the icon
    class CropAreaDialog extends JDialog {
        private BufferedImage image;
        private CropPanel cropPanel;
        private Rectangle selectedArea;

        public CropAreaDialog(AutomatedIconTesting parent, BufferedImage image) {
            super(parent, "Select Crop Area", true);
            this.image = image;
            setSize(image.getWidth() + 50, image.getHeight() + 100);
            setLayout(new BorderLayout());

            cropPanel = new CropPanel(image);
            add(cropPanel, BorderLayout.CENTER);

            JButton saveButton = new JButton("Save");
            add(saveButton, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> {
                selectedArea = cropPanel.getSelection();
                dispose();
            });
        }

        public Rectangle getSelectedArea() {
            return selectedArea;
        }
    }

    // Custom panel to handle crop area selection
    class CropPanel extends JPanel {
        private BufferedImage image;
        private Rectangle selection;
        private Point startDrag, endDrag;

        public CropPanel(BufferedImage image) {
            this.image = image;
            selection = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startDrag = e.getPoint();
                    endDrag = startDrag;
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    endDrag = e.getPoint();
                    updateSelection();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    endDrag = e.getPoint();
                    updateSelection();
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        private void updateSelection() {
            int x = Math.min(startDrag.x, endDrag.x);
            int y = Math.min(startDrag.y, endDrag.y);
            int width = Math.abs(startDrag.x - endDrag.x);
            int height = Math.abs(startDrag.y - endDrag.y);
            selection = new Rectangle(x, y, width, height);
        }

        public Rectangle getSelection() {
            return selection;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
            g.setColor(Color.RED);
            ((Graphics2D) g).setStroke(new BasicStroke(2));
            g.drawRect(selection.x, selection.y, selection.width, selection.height);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutomatedIconTesting(null).setVisible(true));
    }
}
