package org.example;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.awt.image.ConvolveOp;
import java.awt.image.BufferedImageOp;

public class AutomatedWarningTesting extends JDialog {

    private JTextField excelPathField, picturesFolderField, croppedImagesFolderField, resultsExcelField, cropAreaField;
    private JButton startButton, selectExcelButton, selectPicturesButton, selectCroppedImagesButton, selectResultsButton, selectCropAreaButton;
    private JButton singleTubeButton, twinTubeButton;
    private Rectangle selectedArea;
    private BufferedImage referenceImage; // for the Single/Twin Tube reference
    private String referenceType;         // "SINGLE" or "TWIN"

    // Log text area to display processing details
    private JTextArea logTextArea;

    public AutomatedWarningTesting(JFrame parent) {
        super(parent, "Automated Warning Testing", true);

        // Increase window size to accommodate log area.
        setSize(800, 650);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int labelWidth = 250;
        int textWidth = 350;
        int buttonWidth = 100;
        int y = 20;
        int gap = 40;

        // Excel file selection
        JLabel selectExcelLabel = new JLabel("Select Filtered Excel File:");
        selectExcelLabel.setBounds(20, y, labelWidth, 30);
        add(selectExcelLabel);

        excelPathField = new JTextField();
        excelPathField.setBounds(20, y + 30, textWidth, 30);
        add(excelPathField);

        selectExcelButton = new JButton("Browse");
        selectExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectExcelButton);

        y += gap + 30;
        // Pictures folder selection
        JLabel selectPicturesLabel = new JLabel("Select Pictures Folder:");
        selectPicturesLabel.setBounds(20, y, labelWidth, 30);
        add(selectPicturesLabel);

        picturesFolderField = new JTextField();
        picturesFolderField.setBounds(20, y + 30, textWidth, 30);
        add(picturesFolderField);

        selectPicturesButton = new JButton("Browse");
        selectPicturesButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectPicturesButton);

        y += gap + 30;
        // Cropped images folder selection
        JLabel selectCroppedImagesLabel = new JLabel("Select Cropped Images Folder:");
        selectCroppedImagesLabel.setBounds(20, y, labelWidth, 30);
        add(selectCroppedImagesLabel);

        croppedImagesFolderField = new JTextField();
        croppedImagesFolderField.setBounds(20, y + 30, textWidth, 30);
        add(croppedImagesFolderField);

        selectCroppedImagesButton = new JButton("Browse");
        selectCroppedImagesButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectCroppedImagesButton);

        y += gap + 30;
        // Results Excel file save location
        JLabel selectResultsLabel = new JLabel("Select Results Excel Save Location:");
        selectResultsLabel.setBounds(20, y, labelWidth, 30);
        add(selectResultsLabel);

        resultsExcelField = new JTextField();
        resultsExcelField.setBounds(20, y + 30, textWidth, 30);
        add(resultsExcelField);

        selectResultsButton = new JButton("Browse");
        selectResultsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectResultsButton);

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

        // Buttons for choosing reference images
        singleTubeButton = new JButton("Single Tube");
        singleTubeButton.setBounds(20, y + 70, 140, 30);
        add(singleTubeButton);

        twinTubeButton = new JButton("Twin Tube");
        twinTubeButton.setBounds(180, y + 70, 140, 30);
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
        // Set bounds for the log area near the bottom of the window
        logScrollPane.setBounds(20, 550, 740, 80);
        add(logScrollPane);

        // Action Listeners for file/folder selections
        selectExcelButton.addActionListener(e -> selectFile(excelPathField));
        selectPicturesButton.addActionListener(e -> selectFolder(picturesFolderField));
        selectCroppedImagesButton.addActionListener(e -> selectFolder(croppedImagesFolderField));
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultsExcelField));

        // When "Select Area" is pressed, open the crop dialog (if a reference image is loaded)
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

    // Opens a file chooser to select a folder
    private void selectFolder(JTextField textField) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(folderChooser.getSelectedFile().getAbsolutePath());
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
        String imagePath = basePath + "\\Reference_" + (type.equals("SINGLE") ? "SINGLE" : "TWIN") + ".png";
        referenceImage = ImageIO.read(new File(imagePath));
        referenceType = type;
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



    class LogWindow extends JDialog {
        private JTextArea logTextArea;

        public LogWindow(AutomatedWarningTesting parent) {
            super(parent, "Testing Log", false);  // The second parameter makes the dialog non-modal
            setSize(800, 600);
            setLocationRelativeTo(parent); // Center it relative to the main window
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            logTextArea = new JTextArea();
            logTextArea.setEditable(false);
            JScrollPane logScrollPane = new JScrollPane(logTextArea);
            add(logScrollPane);

            setVisible(true);  // Make sure the dialog is visible
        }

        public void appendLog(String message) {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength()); // Auto-scroll
        }
    }


    private BufferedImage enhanceImage(BufferedImage original) {
        // Convert the image to grayscale
        BufferedImage grayscaleImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscaleImage.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Adjust brightness and contrast using RescaleOp
        RescaleOp rescaleOp = new RescaleOp(1.2f, 15, null); // Increase contrast and brightness
        grayscaleImage = rescaleOp.filter(grayscaleImage, null);

        // Sharpen the image using a convolution kernel
        float[] sharpenMatrix = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };
        Kernel kernel = new Kernel(3, 3, sharpenMatrix);
        BufferedImageOp sharpen = new ConvolveOp(kernel);
        grayscaleImage = sharpen.filter(grayscaleImage, null);

        return grayscaleImage;
    }

    // Language code map (mapping the language code to Tesseract language codes)
    private static final Map<String, String> languageMap = new HashMap<>();
    static {
        languageMap.put("de", "deu");    // German
        languageMap.put("gb", "eng");    // English (GB)
        languageMap.put("us", "eng");    // English (US)
        languageMap.put("sa", "ara");    // Arabic
        languageMap.put("hk", "zho");    // Chinese (PRC)
        languageMap.put("bg", "bul");    // Bulgarian
        languageMap.put("cz", "ces");    // Czech
        languageMap.put("cn", "zho");    // Chinese (PRC)
        languageMap.put("gr", "ell");    // Greek
        languageMap.put("tw", "zho");    // Chinese (Taiwan)
        languageMap.put("dk", "dan");    // Danish
        languageMap.put("fi", "fin");    // Finnish
        languageMap.put("fr", "fra");    // French
        languageMap.put("il", "heb");    // Hebrew
        languageMap.put("hr", "hrv");    // Croatian
        languageMap.put("hu", "hun");    // Hungarian
        languageMap.put("id", "ind");    // Indonesian
        languageMap.put("it", "ita");    // Italian
        languageMap.put("jp", "jpn");    // Japanese
        languageMap.put("kr", "kor");    // Korean
        languageMap.put("my", "msa");    // Malay
        languageMap.put("nl", "nld");    // Dutch
        languageMap.put("no", "nor");    // Norwegian
        languageMap.put("pl", "pol");    // Polish
        languageMap.put("br", "por");    // Portuguese (Brazil)
        languageMap.put("pt", "por");    // Portuguese (Standard)
        languageMap.put("ro", "ron");    // Romanian
        languageMap.put("ru", "rus");    // Russian
        languageMap.put("sk", "slk");    // Slovak
        languageMap.put("si", "slv");    // Slovenian
        languageMap.put("es", "spa");    // Spanish
        languageMap.put("rs", "srp");    // Serbian (Latin)
        languageMap.put("se", "swe");    // Swedish
        languageMap.put("th", "tha");    // Thai
        languageMap.put("tr", "tur");    // Turkish
        languageMap.put("ua", "ukr");    // Ukrainian
        languageMap.put("vn", "vie");    // Vietnamese
    }

    /**
     * Main testing logic:
     * 1. Read the first (selector) sheet to build a map for each language (Column 0 holds RUN/SKIP, Column 1 holds language code).
     * 2. For each subsequent language sheet, create a results sheet (even if the language is marked SKIP).
     *    - If the selector marks the language as RUN, process each row:
     *         - If the row's tcontrol is "run", perform OCR.
     *         - Otherwise, mark the row as "SKIPPED".
     *    - If the selector marks the language as SKIP or is missing, mark all rows as "SKIPPED".
     * 3. At the end of each sheet, append a summary count of CORRECT, INCORRECT, and SKIPPED.
     */

    // In the main testing class, add the following to the startTesting method:

    private void startTesting() {
        // Create the log window
        LogWindow logWindow = new LogWindow(this);  // Pass the parent frame

        // Use SwingWorker to run the OCR and testing in the background
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                String excelPath = excelPathField.getText();
                String picturesFolder = picturesFolderField.getText();
                String croppedImagesFolder = croppedImagesFolderField.getText();
                String resultsExcelPath = resultsExcelField.getText().trim();

                if (!resultsExcelPath.toLowerCase().endsWith(".xlsx")) {
                    resultsExcelPath += ".xlsx";
                }

                if (excelPath.isEmpty() || picturesFolder.isEmpty() || croppedImagesFolder.isEmpty() ||
                        resultsExcelPath.isEmpty() || selectedArea == null) {
                    publish("Please fill all the fields and select a crop area.");
                    return null;
                }

                // Initialize Tesseract OCR engine (using Tess4J) with fixed tessdata path
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath("C:\\Licenta\\ExcelManager\\tessdata"); // Set the path to the tessdata folder

                try (FileInputStream fis = new FileInputStream(excelPath);
                     Workbook workbook = new XSSFWorkbook(fis);
                     Workbook resultsWorkbook = new XSSFWorkbook()) {

                    // 1) Read the selector sheet (first sheet) to build a mapping: language -> RUN/SKIP.
                    Sheet selectorSheet = workbook.getSheetAt(0);
                    Map<String, String> languageRunMap = new HashMap<>();

                    publish("Reading language selector sheet...");
                    for (int r = 1; r <= selectorSheet.getLastRowNum(); r++) {
                        Row row = selectorSheet.getRow(r);
                        if (row == null) continue;
                        // Column 0: RUN/SKIP, Column 1: language code.
                        Cell runCell = row.getCell(0);
                        Cell langCell = row.getCell(1);
                        if (runCell == null || langCell == null) continue;
                        String runOrSkip = runCell.toString().trim().toUpperCase();
                        String languageCode = langCell.toString().trim();
                        languageRunMap.put(languageCode, runOrSkip);
                        publish("Language: " + languageCode + " - " + runOrSkip);
                    }

                    // Process each language sheet (skip the first selector sheet).
                    for (int i = 1; i < workbook.getNumberOfSheets(); i++) {
                        Sheet langSheet = workbook.getSheetAt(i);
                        if (langSheet == null) continue;

                        String sheetName = langSheet.getSheetName().trim();
                        publish("Processing sheet: " + sheetName);

                        // Determine if this language should be processed (global RUN) or not.
                        boolean globalRun = languageRunMap.containsKey(sheetName) && "RUN".equalsIgnoreCase(languageRunMap.get(sheetName));
                        if (!globalRun) {
                            publish("Sheet " + sheetName + " is marked SKIP or missing in selector. All rows will be marked SKIPPED.");
                        }

                        // Create a results sheet for this language regardless.
                        Sheet resultsSheet = resultsWorkbook.createSheet(sheetName);
                        Row headerRow = resultsSheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Warning Name");
                        headerRow.createCell(1).setCellValue("Warning Text");
                        headerRow.createCell(2).setCellValue("OCR Text");
                        headerRow.createCell(3).setCellValue("Result");

                        int resultsRowIndex = 1;
                        int correctCount = 0;
                        int incorrectCount = 0;
                        int skippedCount = 0;

                        // Process each row in the language sheet (assuming row 0 is header).
                        for (int r = 1; r <= langSheet.getLastRowNum(); r++) {
                            Row row = langSheet.getRow(r);
                            if (row == null) continue;

                            Cell tcontrolCell = row.getCell(0);
                            Cell warningNameCell = row.getCell(1);
                            Cell warningTextCell = row.getCell(2);

                            if (tcontrolCell == null || warningNameCell == null || warningTextCell == null) {
                                publish("Row " + r + " in sheet " + sheetName + " is incomplete, skipping.");
                                skippedCount++;
                                continue;
                            }

                            String tcontrol = tcontrolCell.toString().trim().toLowerCase();
                            String warningName = warningNameCell.toString().trim();
                            String expectedText = warningTextCell.toString().trim();

                            Row resultRow = resultsSheet.createRow(resultsRowIndex++);
                            resultRow.createCell(0).setCellValue(warningName);
                            resultRow.createCell(1).setCellValue(expectedText);

                            // If global RUN is false, mark the row as SKIPPED.
                            if (!globalRun) {
                                resultRow.createCell(2).setCellValue("");
                                resultRow.createCell(3).setCellValue("SKIPPED");
                                skippedCount++;
                                continue;
                            }

                            // If global RUN is true, then check the row's own tcontrol.
                            if (!"run".equalsIgnoreCase(tcontrol)) {
                                resultRow.createCell(2).setCellValue("");
                                resultRow.createCell(3).setCellValue("SKIPPED");
                                skippedCount++;
                                continue;
                            }

                            // Process image "warningName_sheetName.png"
                            String imageFileName = warningName + "_" + sheetName.toLowerCase() + ".png";
                            File imageFile = new File(picturesFolder, imageFileName);
                            if (!imageFile.exists()) {
                                publish("Image file not found: " + imageFile.getAbsolutePath());
                                resultRow.createCell(2).setCellValue("");
                                resultRow.createCell(3).setCellValue("IMAGE NOT FOUND");
                                skippedCount++;
                                continue;
                            }

                            BufferedImage original = ImageIO.read(imageFile);

                            // Enhance the image before OCR
                            BufferedImage enhancedImage = enhanceImage(original);

                            BufferedImage cropped = enhancedImage.getSubimage(
                                    selectedArea.x,
                                    selectedArea.y,
                                    selectedArea.width,
                                    selectedArea.height
                            );

                            // Save the cropped image (naming: warningName_sheetName_crop.png)
                            String croppedImageName = warningName + "_" + sheetName.toLowerCase() + "_crop.png";
                            File croppedFile = new File(croppedImagesFolder, croppedImageName);
                            ImageIO.write(cropped, "png", croppedFile);

                            // Set language for OCR based on the language map
                            String languageCode = languageMap.get(sheetName.toLowerCase());
                            if (languageCode != null) {
                                tesseract.setLanguage(languageCode); // Set the language for OCR
                            }

                            // Run OCR on the enhanced image
                            String ocrText = "";
                            try {
                                ocrText = tesseract.doOCR(cropped).trim();
                            } catch (TesseractException te) {
                                te.printStackTrace();
                            }

                            String result = ocrText.equalsIgnoreCase(expectedText) ? "CORRECT" : "INCORRECT";
                            if ("CORRECT".equals(result)) {
                                correctCount++;
                            } else {
                                incorrectCount++;
                            }

                            resultRow.createCell(2).setCellValue(ocrText);
                            resultRow.createCell(3).setCellValue(result);

                            String logMsg = "Language Sheet: " + sheetName + "\n" +
                                    "Warning: " + warningName + "\n" +
                                    "Expected: " + expectedText + "\n" +
                                    "OCR Result: " + ocrText + "\n" +
                                    "Test: " + result + "\n-------------------------";
                            publish(logMsg);
                        }
                        // Append sheet summary to log.
                        String summary = "Sheet " + sheetName + " Summary: CORRECT: " + correctCount +
                                ", INCORRECT: " + incorrectCount + ", SKIPPED: " + skippedCount;
                        publish(summary);
                        publish("-------------------------");
                    }

                    // Save the results workbook to the specified file.
                    try (FileOutputStream fos = new FileOutputStream(resultsExcelPath)) {
                        resultsWorkbook.write(fos);
                    }
                    publish("Results saved to: " + resultsExcelPath);
                    JOptionPane.showMessageDialog(null, "Testing Complete! Results saved to: " + resultsExcelPath,
                            "Info", JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    publish("Error during testing: " + ex.getMessage());
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String logMessage : chunks) {
                    logWindow.appendLog(logMessage); // Update log window
                }
            }
        };

        // Start the worker in the background
        worker.execute();
    }

    // Inner dialog for selecting a crop area over the reference image.
    class CropAreaDialog extends JDialog {
        private BufferedImage image;
        private CropPanel cropPanel;
        private Rectangle selectedArea;

        public CropAreaDialog(AutomatedWarningTesting parent, BufferedImage image) {
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

    // Custom panel that displays the image and lets the user draw a rectangle to select the crop area.
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
        SwingUtilities.invokeLater(() -> new AutomatedWarningTesting(null).setVisible(true));
    }
}
