package org.example;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * AutomatedIconTesting extends JDialog and serves as the main GUI for automated icon testing.
 *
 * Excel file column mapping:
 * - Column 0: tcontrol
 * - Column 1: warning name
 * - Column 2: icon name
 * - Column 3: result
 */
public class AutomatedIconTesting extends JDialog {

    // Text fields for selecting various files and folders needed by the application
    private JTextField iconExcelField, downPicsField, refPicsField, croppedImagesFolderField, resultsExcelField, cropAreaField;
    // Buttons for browsing files/folders and performing actions (select crop area, start test, etc.)
    private JButton selectIconExcelButton, selectDownPicsButton, selectRefPicsButton, selectCroppedImagesButton, selectResultsButton, selectCropAreaButton;
    private JButton singleTubeButton, twinTubeButton, startButton;
    // Rectangle representing the user-selected crop area (applied to both down and reference images)
    private Rectangle selectedArea;
    // Buffered image for the reference image loaded via the single/twin tube button (used in cropping)
    private BufferedImage referenceImage;
    // Log text area to display messages in the main dialog and log window
    private JTextArea logTextArea;
    // List to store warning data read from the Excel file (each object contains the warning name and row index)
    private List<WarningIconData> iconData;

    // Constructor that initializes the GUI components and layouts
    public AutomatedIconTesting(JFrame parent) {
        super(parent, "Automated Icon Testing", true);
        setSize(800, 750);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int labelWidth = 250;
        int textWidth = 350;
        int buttonWidth = 100;
        int y = 20;
        int gap = 40;

        // --- Icon Excel File Selection ---
        JLabel iconExcelLabel = new JLabel("Select Icon Excel File:");
        iconExcelLabel.setBounds(20, y, labelWidth, 30);
        add(iconExcelLabel);

        iconExcelField = new JTextField();
        iconExcelField.setBounds(20, y + 30, textWidth, 30);
        add(iconExcelField);

        selectIconExcelButton = new JButton("Browse");
        selectIconExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectIconExcelButton);

        y += gap + 30;

        // --- Down Pics Folder Selection ---
        JLabel downPicsLabel = new JLabel("Select Down Pics Folder:");
        downPicsLabel.setBounds(20, y, labelWidth, 30);
        add(downPicsLabel);

        downPicsField = new JTextField();
        downPicsField.setBounds(20, y + 30, textWidth, 30);
        add(downPicsField);

        selectDownPicsButton = new JButton("Browse");
        selectDownPicsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectDownPicsButton);

        y += gap + 30;

        // --- Ref Pics Folder Selection ---
        JLabel refPicsLabel = new JLabel("Select Ref Pics Folder:");
        refPicsLabel.setBounds(20, y, labelWidth, 30);
        add(refPicsLabel);

        refPicsField = new JTextField();
        refPicsField.setBounds(20, y + 30, textWidth, 30);
        add(refPicsField);

        selectRefPicsButton = new JButton("Browse");
        selectRefPicsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectRefPicsButton);

        y += gap + 30;

        // --- Cropped Images Folder Selection ---
        JLabel croppedImagesLabel = new JLabel("Select Cropped Images Folder:");
        croppedImagesLabel.setBounds(20, y, labelWidth, 30);
        add(croppedImagesLabel);

        croppedImagesFolderField = new JTextField();
        croppedImagesFolderField.setBounds(20, y + 30, textWidth, 30);
        add(croppedImagesFolderField);

        selectCroppedImagesButton = new JButton("Browse");
        selectCroppedImagesButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectCroppedImagesButton);

        y += gap + 30;

        // --- Results Excel File Save Location ---
        JLabel resultsExcelLabel = new JLabel("Select Results Excel Save Location:");
        resultsExcelLabel.setBounds(20, y, labelWidth, 30);
        add(resultsExcelLabel);

        resultsExcelField = new JTextField();
        resultsExcelField.setBounds(20, y + 30, textWidth, 30);
        add(resultsExcelField);

        selectResultsButton = new JButton("Browse");
        selectResultsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectResultsButton);

        y += gap + 30;

        // --- Crop Area Selection ---
        JLabel cropAreaLabel = new JLabel("Crop Area (x y width height):");
        cropAreaLabel.setBounds(20, y, labelWidth, 30);
        add(cropAreaLabel);

        cropAreaField = new JTextField();
        cropAreaField.setBounds(20, y + 30, 250, 30);
        add(cropAreaField);

        selectCropAreaButton = new JButton("Select Area");
        selectCropAreaButton.setBounds(280, y + 30, 120, 30);
        add(selectCropAreaButton);

        y += gap + 30;

        // --- Reference Image Mode Buttons (Single/Twin) ---
        singleTubeButton = new JButton("Single Tube");
        singleTubeButton.setBounds(20, y, 140, 30);
        add(singleTubeButton);

        twinTubeButton = new JButton("Twin Tube");
        twinTubeButton.setBounds(180, y, 140, 30);
        add(twinTubeButton);

        y += gap + 30;

        // --- Start Testing Button ---
        startButton = new JButton("Start Testing");
        startButton.setBounds(150, y, 150, 40);
        add(startButton);

        y += gap + 30;

        // --- Log Text Area ---
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBounds(20, y, 740, 150);
        add(logScrollPane);

        // --- Action Listeners ---
        selectIconExcelButton.addActionListener(e -> selectFile(iconExcelField));
        selectDownPicsButton.addActionListener(e -> selectDirectory(downPicsField));
        selectRefPicsButton.addActionListener(e -> selectDirectory(refPicsField));
        selectCroppedImagesButton.addActionListener(e -> selectDirectory(croppedImagesFolderField));
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultsExcelField));

        selectCropAreaButton.addActionListener(e -> {
            if (referenceImage != null) {
                openCropAreaDialog();
            } else {
                JOptionPane.showMessageDialog(this, "Please select single or twin tube reference first.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        singleTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("SINGLE");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading single tube reference image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        twinTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("TWIN");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading twin tube reference image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        startButton.addActionListener(e -> startTesting());
    }

    // --- Utility Methods for File/Folder Selection ---
    private void selectFile(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void selectDirectory(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void selectFileSaveLocation(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // --- Load Reference Image Method ---
    // Loads the reference image from a fixed path with filenames "Reference_SINGLE.png" or "Reference_TWIN.png".
    private void loadReferenceImage(String type) throws IOException {
        String basePath = "c:\\licenta\\excelmanager\\src\\main\\java\\org\\example\\pics";
        String imageFile = "Reference_" + (type.equals("SINGLE") ? "SINGLE" : "TWIN") + ".png";
        File file = new File(basePath + System.getProperty("file.separator") + imageFile);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "Reference image " + imageFile + " not found in " + basePath, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        referenceImage = ImageIO.read(file);
        openCropAreaDialog();
    }

    // --- Open Crop Area Dialog Method ---
    private void openCropAreaDialog() {
        CropAreaDialog dialog = new CropAreaDialog(this, referenceImage);
        dialog.setVisible(true);
        Rectangle area = dialog.getSelectedArea();
        if (area != null) {
            selectedArea = area;
            cropAreaField.setText(area.x + " " + area.y + " " + area.width + " " + area.height);
        }
    }

    // --- Start Testing Method ---
    private void startTesting() {
        LogWindow logWindow = new LogWindow(this);
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                String iconExcelPath = iconExcelField.getText().trim();
                String downPicsFolder = downPicsField.getText().trim();
                String refPicsFolder = refPicsField.getText().trim();
                String resultsExcelPath = resultsExcelField.getText().trim();
                String croppedFolder = croppedImagesFolderField.getText().trim();

                if (iconExcelPath.isEmpty() || downPicsFolder.isEmpty() || refPicsFolder.isEmpty() ||
                        resultsExcelPath.isEmpty() || croppedFolder.isEmpty() || selectedArea == null) {
                    publish("Please ensure all selections are made and crop area is set.");
                    return null;
                }

                try (FileInputStream fis = new FileInputStream(new File(iconExcelPath));
                     XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    iconData = new ArrayList<>();

                    // Read warning name from column 1 (mapping: col0 = tcontrol, col1 = warning name, col2 = icon name, col3 = result)
                    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null && row.getCell(1) != null) {
                            String warningName = row.getCell(1).toString().trim();
                            iconData.add(new WarningIconData(warningName, rowIndex));
                        }
                    }

                    // Process each warning: compare the down pic with the reference pic (both expected to have the same name)
                    for (WarningIconData data : iconData) {
                        String warningName = data.getWarningName();
                        String downPicPath = downPicsFolder + System.getProperty("file.separator") + warningName + ".png";
                        String refPicPath = refPicsFolder + System.getProperty("file.separator") + warningName + ".png";
                        File downFile = new File(downPicPath);
                        File refFile = new File(refPicPath);

                        if (!downFile.exists() || !refFile.exists()) {
                            data.setResult("pic not found");
                            publish("Warning: " + warningName + " -> pic not found");
                            continue;
                        }

                        BufferedImage downImage = ImageIO.read(downFile);
                        BufferedImage refImage = ImageIO.read(refFile);

                        // Crop both images using the selected area
                        BufferedImage croppedDown = downImage.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
                        BufferedImage croppedRef = refImage.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);

                        // Save the cropped down image automatically
                        String croppedImageName = warningName + "_crop.png";
                        File output = new File(croppedFolder + System.getProperty("file.separator") + croppedImageName);
                        ImageIO.write(croppedDown, "png", output);
                        publish("Cropped image saved for warning: " + warningName);

                        // Pixel-by-pixel comparison of the cropped images
                        boolean match = compareImages(croppedDown, croppedRef);
                        String resultStr = match ? "correct" : "incorrect";
                        data.setResult(resultStr);
                        publish("Warning: " + warningName + " -> " + resultStr);
                    }

                    // Write the results into the Excel file in column 3 (cell index 3)
                    Row headerRow = sheet.getRow(0);
                    Cell resultHeaderCell = headerRow.createCell(3);
                    resultHeaderCell.setCellValue("result");
                    for (WarningIconData data : iconData) {
                        Row row = sheet.getRow(data.getRowIndex());
                        if (row != null) {
                            Cell cell = row.createCell(3);
                            cell.setCellValue(data.getResult());
                        }
                    }

                    // Ensure the results Excel file path ends with ".xlsx"
                    if (!resultsExcelPath.toLowerCase().endsWith(".xlsx")) {
                        resultsExcelPath += ".xlsx";
                    }

                    try (FileOutputStream fos = new FileOutputStream(new File(resultsExcelPath))) {
                        workbook.write(fos);
                    }
                    publish("Results written to " + resultsExcelPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("Error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    logWindow.appendLog(msg);
                }
            }
        };
        worker.execute();
    }

    // --- Compare Images Method ---
    private boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) return false;
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) return false;
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- WarningIconData Inner Class ---
    class WarningIconData {
        private String warningName;
        private String result;
        private int rowIndex;

        public WarningIconData(String warningName, int rowIndex) {
            this.warningName = warningName;
            this.rowIndex = rowIndex;
        }

        public String getWarningName() {
            return warningName;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    // --- LogWindow Inner Class ---
    class LogWindow extends JDialog {
        private JTextArea logTextArea;

        public LogWindow(AutomatedIconTesting parent) {
            super(parent, "Testing Log", false);
            setSize(600, 400);
            setLocationRelativeTo(parent);
            logTextArea = new JTextArea();
            logTextArea.setEditable(false);
            logTextArea.setFont(new Font("aptos narrow", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(logTextArea);
            add(scrollPane);
            setVisible(true);
        }

        public void appendLog(String message) {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
    }

    // --- CropAreaDialog Inner Class ---
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

    // --- CropPanel Inner Class ---
    class CropPanel extends JPanel {
        private BufferedImage image;
        private Rectangle selection;
        private Point startDrag, endDrag;

        public CropPanel(BufferedImage image) {
            this.image = image;
            // Initially, the selection covers the whole image
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

        // Update the selection rectangle based on the current mouse drag positions
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

    // Main method to launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutomatedIconTesting(null).setVisible(true));
    }
}
