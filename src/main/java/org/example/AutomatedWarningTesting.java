package org.example;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.awt.image.BufferedImageOp;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AutomatedWarningTesting extends JDialog {

    private JTextField excelPathField, picturesFolderField, croppedImagesFolderField, resultsExcelField, cropAreaField, tessdataPathField;
    private JButton startButton, selectExcelButton, selectPicturesButton, selectCroppedImagesButton, selectResultsButton, selectCropAreaButton, selectTessdataButton;
    private JButton singleTubeButton, twinTubeButton;
    private Rectangle selectedArea;
    private BufferedImage referenceImage; // for the single/twin tube reference
    private String referenceType;         // "SINGLE" or "TWIN"
    private JTextArea logTextArea;

    // Mapping for tess4j OCR language codes.
    private static final Map<String, String> languageMap = new HashMap<>();
    static {
        languageMap.put("de", "deu");    // german
        languageMap.put("gb", "eng");    // english (gb)
        languageMap.put("us", "eng");    // english (us)
        languageMap.put("sa", "ara");    // arabic
        languageMap.put("hk", "chi_all");    // chinese (hk)
        languageMap.put("bg", "bul");    // bulgarian
        languageMap.put("cz", "ces");    // czech
        languageMap.put("cn", "chi_all");    // chinese (prc)
        languageMap.put("gr", "ell");    // greek
        languageMap.put("tw", "chi_all");    // chinese (taiwan)
        languageMap.put("dk", "dan");    // danish
        languageMap.put("fi", "fin");    // finnish
        languageMap.put("fr", "fra");    // french
        languageMap.put("il", "heb");    // hebrew
        languageMap.put("hr", "hrv");    // croatian
        languageMap.put("hu", "hun");    // hungarian
        languageMap.put("id", "ind");    // indonesian
        languageMap.put("it", "ita");    // italian
        languageMap.put("jp", "jpn_2");    // japanese
        languageMap.put("kr", "kor");    // korean
        languageMap.put("my", "msa");    // malay
        languageMap.put("nl", "nld");    // dutch
        languageMap.put("no", "nor");    // norwegian
        languageMap.put("pl", "pol");    // polish
        languageMap.put("br", "por");    // portuguese (brazil)
        languageMap.put("pt", "por");    // portuguese (standard)
        languageMap.put("ro", "ron");    // romanian
        languageMap.put("ru", "rus");    // russian
        languageMap.put("sk", "slk");    // slovak
        languageMap.put("si", "slv");    // slovenian
        languageMap.put("es", "spa");    // spanish
        languageMap.put("rs", "srp");    // serbian (latin)
        languageMap.put("se", "swe");    // swedish
        languageMap.put("th", "tha");    // thai
        languageMap.put("tr", "tur");    // turkish
        languageMap.put("ua", "ukr");    // ukrainian
        languageMap.put("vn", "vie");    // vietnamese
    }

    // Control variables for pause and stop.
    private volatile boolean pauseRequested = false;
    private volatile boolean stopRequested = false;
    private final Object pauseLock = new Object();

    public AutomatedWarningTesting(JFrame parent) {
        super(parent, "Automated Warning Testing", true);

        // Increase window size to accommodate log area.
        setSize(800, 700);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int labelWidth = 250;
        int textWidth = 350;
        int buttonWidth = 100;
        int y = 20;
        int gap = 40;

        // Excel file selection.
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
        // Pictures folder selection.
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
        // Cropped images folder selection.
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
        // Results Excel file save location.
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
        // Tessdata folder selection.
        JLabel tessdataLabel = new JLabel("Select Tessdata Folder:");
        tessdataLabel.setBounds(20, y, labelWidth, 30);
        add(tessdataLabel);

        tessdataPathField = new JTextField();
        tessdataPathField.setBounds(20, y + 30, textWidth, 30);
        add(tessdataPathField);

        selectTessdataButton = new JButton("Browse");
        selectTessdataButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectTessdataButton);
        selectTessdataButton.addActionListener(e -> selectFolder(tessdataPathField));

        y += gap + 30;
        // Crop area field and selection button.
        JLabel cropAreaLabel = new JLabel("Crop Area (x, y, width, height):");
        cropAreaLabel.setBounds(20, y, labelWidth, 30);
        add(cropAreaLabel);

        cropAreaField = new JTextField();
        cropAreaField.setBounds(20, y + 30, 250, 30);
        add(cropAreaField);

        selectCropAreaButton = new JButton("Select Area");
        selectCropAreaButton.setBounds(280, y + 30, 120, 30);
        add(selectCropAreaButton);

        // Buttons for choosing reference images.
        singleTubeButton = new JButton("Single Tube");
        singleTubeButton.setBounds(20, y + 70, 140, 30);
        add(singleTubeButton);

        twinTubeButton = new JButton("Twin Tube");
        twinTubeButton.setBounds(180, y + 70, 140, 30);
        add(twinTubeButton);

        y += gap + 70;
        // Start testing button.
        startButton = new JButton("Start Testing");
        startButton.setBounds(200, y, 150, 40);
        add(startButton);

        // Add a log area at the bottom.
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        // Force the log text area to always use Aptos Narrow.
        logTextArea.setFont(new Font("Aptos Narrow", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBounds(20, 600, 740, 80);
        add(logScrollPane);

        // Action listeners for file/folder selections.
        selectExcelButton.addActionListener(e -> selectFile(excelPathField));
        selectPicturesButton.addActionListener(e -> selectFolder(picturesFolderField));
        selectCroppedImagesButton.addActionListener(e -> selectFolder(croppedImagesFolderField));
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultsExcelField));

        // When "select area" is pressed, open the crop dialog (if a reference image is loaded).
        selectCropAreaButton.addActionListener(e -> {
            if (referenceImage != null) {
                openCropAreaDialog();
            } else {
                JOptionPane.showMessageDialog(this, "Please select single or twin tube reference first.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Load reference image when one of the tube buttons is clicked.
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

    // Utility method to append messages to the log text area.
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> logTextArea.append(message + "\n"));
    }

    // Opens a file chooser to select a file.
    private void selectFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Opens a file chooser to select a folder.
    private void selectFolder(JTextField textField) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(folderChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Opens a file chooser to select a save location for a file.
    private void selectFileSaveLocation(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Loads the reference image based on the selected type and opens the crop area dialog.
    private void loadReferenceImage(String type) throws IOException {
        String basePath = "C:\\Licenta\\ExcelManager\\src\\main\\java\\org\\example\\pics";
        String imagePath = basePath + "\\Reference_" + (type.equals("SINGLE") ? "SINGLE" : "TWIN") + ".png";
        referenceImage = ImageIO.read(new File(imagePath));
        referenceType = type;
        openCropAreaDialog();
    }

    // Opens a dialog that displays the reference image and allows the user to select a crop area.
    private void openCropAreaDialog() {
        CropAreaDialog dialog = new CropAreaDialog(this, referenceImage);
        dialog.setVisible(true);
        Rectangle area = dialog.getSelectedArea();
        if (area != null) {
            selectedArea = area;
            cropAreaField.setText(area.x + ", " + area.y + ", " + area.width + ", " + area.height);
        }
    }

    // Enhances the image before OCR processing.
    private BufferedImage enhanceImage(BufferedImage original) {
        // Convert the image to grayscale.
        BufferedImage grayscaleImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscaleImage.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Adjust brightness and contrast using RescaleOp.
        RescaleOp rescaleOp = new RescaleOp(1.2f, 15, null);
        grayscaleImage = rescaleOp.filter(grayscaleImage, null);

        // Sharpen the image using a convolution kernel.
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

    // Main testing logic using a SwingWorker for background processing.
    private void startTesting() {
        // Create the log window (which now includes Pause/Resume and Stop buttons).
        LogWindow logWindow = new LogWindow(this);

        // Reset pause/stop flags.
        pauseRequested = false;
        stopRequested = false;

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String excelPath = excelPathField.getText();
                String picturesFolder = picturesFolderField.getText();
                String croppedImagesFolder = croppedImagesFolderField.getText();
                String resultsExcelPath = resultsExcelField.getText().trim();
                String tessdataPath = tessdataPathField.getText();

                if (!resultsExcelPath.toLowerCase().endsWith(".xlsx")) {
                    resultsExcelPath += ".xlsx";
                }

                if (excelPath.isEmpty() || picturesFolder.isEmpty() || croppedImagesFolder.isEmpty() ||
                        resultsExcelPath.isEmpty() || tessdataPath.isEmpty() || selectedArea == null) {
                    publish("Please fill all the fields and select a crop area.");
                    return null;
                }

                // Initialize Tesseract OCR engine.
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessdataPath);

                try (FileInputStream fis = new FileInputStream(excelPath);
                     Workbook workbook = new XSSFWorkbook(fis);
                     Workbook resultsWorkbook = new XSSFWorkbook()) {

                    // Create a wrap text cell style for multi-line texts.
                    CellStyle wrapStyle = resultsWorkbook.createCellStyle();
                    wrapStyle.setWrapText(true);

                    // Read the language selector sheet (assumed to be the first sheet).
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
                        if (stopRequested) {
                            publish("Stop requested. Exiting processing.");
                            break;
                        }
                        Sheet langSheet = workbook.getSheetAt(i);
                        if (langSheet == null) continue;

                        String sheetName = langSheet.getSheetName().trim();
                        publish("Processing sheet: " + sheetName);

                        SwingUtilities.invokeLater(() -> logTextArea.setFont(new Font("Aptos Narrow", Font.PLAIN, 12)));

                        // Determine if this language should be processed.
                        boolean globalRun = languageRunMap.containsKey(sheetName) &&
                                "RUN".equalsIgnoreCase(languageRunMap.get(sheetName));
                        if (!globalRun) {
                            publish("Sheet " + sheetName + " is marked skip or missing in selector. All rows will be marked SKIPPED.");
                        }

                        // Create a results sheet for this language.
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

                        for (int r = 1; r <= langSheet.getLastRowNum(); r++) {
                            // Check for pause/resume.
                            synchronized (pauseLock) {
                                while (pauseRequested) {
                                    pauseLock.wait(100);
                                }
                            }
                            if (stopRequested) {
                                // Mark all remaining rows in this sheet as SKIPPED.
                                for (int r2 = r; r2 <= langSheet.getLastRowNum(); r2++) {
                                    Row rowToSkip = langSheet.getRow(r2);
                                    if (rowToSkip == null) continue;
                                    Row skipResultRow = resultsSheet.createRow(resultsRowIndex++);
                                    String warningName = (rowToSkip.getCell(1) != null ? rowToSkip.getCell(1).toString() : "");
                                    String warningText = (rowToSkip.getCell(2) != null ? rowToSkip.getCell(2).toString() : "");
                                    skipResultRow.createCell(0).setCellValue(warningName);
                                    Cell expCell = skipResultRow.createCell(1);
                                    expCell.setCellValue(warningText);
                                    expCell.setCellStyle(wrapStyle);
                                    skipResultRow.createCell(2).setCellValue("");
                                    skipResultRow.createCell(3).setCellValue("SKIPPED");
                                    skippedCount++;
                                }
                                publish("Sheet " + sheetName + " marked as stopped. Skipped remaining rows.");
                                break;
                            }

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
                            String expectedText = warningTextCell.toString(); // preserve original newlines

                            Row resultRow = resultsSheet.createRow(resultsRowIndex++);
                            resultRow.createCell(0).setCellValue(warningName);
                            // Write expected text with wrap style.
                            Cell expectedCell = resultRow.createCell(1);
                            expectedCell.setCellValue(expectedText);
                            expectedCell.setCellStyle(wrapStyle);

                            if (!globalRun) {
                                resultRow.createCell(2).setCellValue("");
                                resultRow.createCell(3).setCellValue("SKIPPED");
                                skippedCount++;
                                continue;
                            }

                            if (!"run".equalsIgnoreCase(tcontrol)) {
                                resultRow.createCell(2).setCellValue("");
                                resultRow.createCell(3).setCellValue("SKIPPED");
                                skippedCount++;
                                continue;
                            }

                            // Build the expected text line by line.
                            String[] expectedLines = expectedText.split("\\r?\\n");

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
                            BufferedImage enhancedImage = enhanceImage(original);
                            BufferedImage cropped = enhancedImage.getSubimage(
                                    selectedArea.x,
                                    selectedArea.y,
                                    selectedArea.width,
                                    selectedArea.height
                            );

                            String croppedImageName = warningName + "_" + sheetName.toLowerCase() + "_crop.png";
                            File croppedFile = new File(croppedImagesFolder, croppedImageName);
                            ImageIO.write(cropped, "png", croppedFile);

                            String languageCode = languageMap.get(sheetName.toLowerCase());
                            if (languageCode != null) {
                                tesseract.setLanguage(languageCode);
                            }

                            String ocrText = "";
                            try {
                                ocrText = tesseract.doOCR(cropped).trim();
                            } catch (TesseractException te) {
                                te.printStackTrace();
                            }

                            // Compare OCR text to expected text line by line.
                            String[] ocrLines = ocrText.split("\\r?\\n");
                            boolean linesMatch = true;
                            if (expectedLines.length != ocrLines.length) {
                                linesMatch = false;
                            } else {
                                for (int k = 0; k < expectedLines.length; k++) {
                                    if (!expectedLines[k].trim().equalsIgnoreCase(ocrLines[k].trim())) {
                                        linesMatch = false;
                                        break;
                                    }
                                }
                            }
                            String resultText = linesMatch ? "CORRECT" : "INCORRECT";
                            if ("CORRECT".equals(resultText)) {
                                correctCount++;
                            } else {
                                incorrectCount++;
                            }

                            // Write OCR text with wrap style.
                            Cell ocrCell = resultRow.createCell(2);
                            ocrCell.setCellValue(ocrText);
                            ocrCell.setCellStyle(wrapStyle);
                            resultRow.createCell(3).setCellValue(resultText);

                            String logMsg = "Sheet: " + sheetName + "\n" +
                                    "Warning: " + warningName + "\n" +
                                    "Expected (line-by-line):";
                            for (String line : expectedLines) {
                                logMsg += "\n" + line;
                            }
                            logMsg += "\nOCR Result:";
                            for (String line : ocrLines) {
                                logMsg += "\n" + line;
                            }
                            logMsg += "\nTest: " + resultText + "\n-------------------------";
                            publish(logMsg);
                        }
                        String summary = "Sheet " + sheetName + " summary: Correct: " + correctCount +
                                ", Incorrect: " + incorrectCount + ", Skipped: " + skippedCount;
                        publish(summary);
                        publish("-------------------------");
                    }

                    // Auto-size and adjust column widths in all results sheets.
                    for (int sheetIndex = 0; sheetIndex < resultsWorkbook.getNumberOfSheets(); sheetIndex++) {
                        Sheet sheet = resultsWorkbook.getSheetAt(sheetIndex);
                        // Assume 4 columns: Warning Name, Warning Text, OCR Text, and Result.
                        for (int col = 0; col < 4; col++) {
                            sheet.autoSizeColumn(col);
                            // For the multi-line text columns (expected and OCR texts), add extra padding.
                            if (col == 1 || col == 2) {
                                int currentWidth = sheet.getColumnWidth(col);
                                sheet.setColumnWidth(col, (int)(currentWidth * 1.2));
                            }
                        }
                    }

                    try (FileOutputStream fos = new FileOutputStream(resultsExcelPath)) {
                        resultsWorkbook.write(fos);
                    }
                    publish("Results saved to: " + resultsExcelPath);
                    JOptionPane.showMessageDialog(null, "Testing complete! Results saved to: " + resultsExcelPath,
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
                    logWindow.appendLog(logMessage);
                }
            }
        };

        worker.execute();
    }

    // Inner class for the log window that includes Pause/Resume and Stop buttons.
    class LogWindow extends JDialog {
        private JTextArea logTextArea;
        private JButton pauseButton;
        private JButton stopButton;

        public LogWindow(AutomatedWarningTesting parent) {
            super(parent, "Testing Log", false);
            setSize(800, 600);
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());

            // Log text area.
            logTextArea = new JTextArea();
            logTextArea.setEditable(false);
            logTextArea.setFont(new Font("Aptos Narrow", Font.PLAIN, 12));
            JScrollPane logScrollPane = new JScrollPane(logTextArea);
            add(logScrollPane, BorderLayout.CENTER);

            // Panel for control buttons.
            JPanel buttonPanel = new JPanel();
            pauseButton = new JButton("Pause");
            stopButton = new JButton("Stop");
            buttonPanel.add(pauseButton);
            buttonPanel.add(stopButton);
            add(buttonPanel, BorderLayout.SOUTH);

            // Pause/Resume button action.
            pauseButton.addActionListener(e -> {
                if (!pauseRequested) {
                    pauseRequested = true;
                    pauseButton.setText("Resume");
                    appendLog("Processing paused.");
                } else {
                    synchronized (pauseLock) {
                        pauseRequested = false;
                        pauseLock.notifyAll();
                    }
                    pauseButton.setText("Pause");
                    appendLog("Processing resumed.");
                }
            });

            // Stop button action.
            stopButton.addActionListener(e -> {
                stopRequested = true;
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                appendLog("Stop requested. Finishing up current processing and marking remaining warnings as SKIPPED.");
            });

            setVisible(true);
        }

        public void appendLog(String message) {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
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

    // Custom panel for drawing a selection rectangle.
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

    // Helper method to update the default UI font globally.
    public static void setUIFont(FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    // Main method to launch the application; sets the global UI font and shows the GUI.
    public static void main(String[] args) {
        setUIFont(new FontUIResource(new Font("Aptos Narrow", Font.PLAIN, 12)));
        SwingUtilities.invokeLater(() -> new AutomatedWarningTesting(null).setVisible(true));
    }
}
