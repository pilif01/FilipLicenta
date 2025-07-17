package org.example;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ibm.icu.text.BreakIterator;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter; // Import for FocusListener
import java.awt.event.FocusEvent;   // Import for FocusEvent
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.Enumeration;

public class MenuFullAuto extends JDialog {

    // UI Components
    private final JTextField excelPathField = new JTextField();
    private final JTextField picturesFolderField = new JTextField();
    private final JTextField croppedImagesFolderField = new JTextField();
    private final JTextField resultsExcelField = new JTextField();
    private final JTextField tessdataPathField = new JTextField();
    private final JButton startButton = new JButton("Start Testing");
    private final JTextArea logTextArea = new JTextArea();

    // Tess4J language mapping
    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();
    static {
        LANGUAGE_MAP.put("de", "deu");      // German
        LANGUAGE_MAP.put("gb", "eng");      // English (GB)
        LANGUAGE_MAP.put("us", "eng");      // English (US)
        LANGUAGE_MAP.put("sa", "ara");      // Arabic
        LANGUAGE_MAP.put("hk", "chi_tra");  // Chinese (Hong Kong)
        LANGUAGE_MAP.put("bg", "bul");      // Bulgarian
        LANGUAGE_MAP.put("cz", "ces");      // Czech
        LANGUAGE_MAP.put("cn", "chi_sim");  // Chinese (PRC)
        LANGUAGE_MAP.put("gr", "ell");      // Greek
        LANGUAGE_MAP.put("tw", "chi_tra");  // Chinese (Taiwan)
        LANGUAGE_MAP.put("dk", "dan");      // Danish
        LANGUAGE_MAP.put("fi", "fin");      // Finnish
        LANGUAGE_MAP.put("fr", "fra");      // French
        LANGUAGE_MAP.put("il", "heb");      // Hebrew
        LANGUAGE_MAP.put("hr", "hrv");      // Croatian
        LANGUAGE_MAP.put("hu", "hun");      // Hungarian
        LANGUAGE_MAP.put("id", "ind");      // Indonesian
        LANGUAGE_MAP.put("it", "ita");      // Italian
        LANGUAGE_MAP.put("jp", "jpn_2");    // Japanese
        LANGUAGE_MAP.put("kr", "kor");      // Korean
        LANGUAGE_MAP.put("my", "msa");      // Malay
        LANGUAGE_MAP.put("nl", "nld");      // Dutch
        LANGUAGE_MAP.put("no", "nor");      // Norwegian
        LANGUAGE_MAP.put("pl", "pol");      // Polish
        LANGUAGE_MAP.put("br", "por");      // Portuguese (Brazil)
        LANGUAGE_MAP.put("pt", "por");      // Portuguese (Standard)
        LANGUAGE_MAP.put("ro", "ron");      // Romanian
        LANGUAGE_MAP.put("ru", "rus");      // Russian
        LANGUAGE_MAP.put("sk", "slk");      // Slovak
        LANGUAGE_MAP.put("si", "slv");      // Slovenian
        LANGUAGE_MAP.put("es", "spa");      // Spanish
        LANGUAGE_MAP.put("rs", "srp");      // Serbian (Latin)
        LANGUAGE_MAP.put("se", "swe");      // Swedish
        LANGUAGE_MAP.put("th", "tha");      // Thai
        LANGUAGE_MAP.put("tr", "tur");      // Turkish
        LANGUAGE_MAP.put("ua", "ukr");      // Ukrainian
        LANGUAGE_MAP.put("vn", "vie");      // Vietnamese
    }

    // Pause/Stop control for the background task
    private volatile boolean pauseRequested = false;
    private volatile boolean stopRequested = false;
    private final Object pauseLock = new Object(); // Object for synchronization during pause

    // Log window instance
    private LogWindow logWindow;

    /**
     * Constructs the main application dialog.
     * Sets up the UI and initializes components.
     */
    public MenuFullAuto() {
        super((java.awt.Frame) null, "Automated Text Testing", true); // Modal dialog on null parent
        initUI();
    }

    /**
     * Initializes and lays out the UI components.
     */
    private void initUI() {
        setSize(800, 700);
        setLayout(null); // Using null layout for absolute positioning
        setLocationRelativeTo(null); // Center the window on screen
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Close operation

        int textFieldWidth = 350;
        int buttonWidth = 100;
        int currentY = 20; // Initial Y position for the first component
        int verticalGap = 40; // Vertical spacing between component groups

        // Helper to add label, text field, and browse button
        addPathChooser("Select Test Excel File:", excelPathField, currentY, e -> selectFile(excelPathField));
        currentY += verticalGap + 30;

        addPathChooser("Select Pictures Folder:", picturesFolderField, currentY, e -> selectFolder(picturesFolderField));
        currentY += verticalGap + 30;

        addPathChooser("Select Cropped Images Folder:", croppedImagesFolderField, currentY, e -> selectFolder(croppedImagesFolderField));
        currentY += verticalGap + 30;

        // Results Excel field with .xlsx automatic append logic
        addPathChooser("Select Results Excel Save Location:", resultsExcelField, currentY, e -> selectFileSave(resultsExcelField));
        // Add FocusListener to resultsExcelField to automatically append .xlsx
        resultsExcelField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                appendXlsxExtension(resultsExcelField);
            }
        });
        currentY += verticalGap + 30;

        addPathChooser("Select Tessdata Folder:", tessdataPathField, currentY, e -> selectFolder(tessdataPathField));
        currentY += verticalGap + 30;

        // Start Testing button
        startButton.setBounds(200, currentY, 150, 40);
        startButton.addActionListener(e -> startTesting());
        add(startButton);

        // Main log area (for overall application messages)
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Aptos Narrow", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setBounds(20, 600, 740, 80); // Position at the bottom of the main window
        add(scrollPane);
    }

    /**
     * Helper method to add a label, text field, and browse button to the UI.
     *
     * @param labelText The text for the label.
     * @param textField The JTextField to associate with the label and browse button.
     * @param y         The Y coordinate for the label and text field.
     * @param selector  The ActionListener for the browse button (e.g., selectFile or selectFolder).
     */
    private void addPathChooser(String labelText, JTextField textField, int y, ActionListener selector) {
        JLabel label = new JLabel(labelText);
        label.setBounds(20, y, 250, 30);
        add(label);

        textField.setBounds(20, y + 30, 350, 30);
        add(textField);

        JButton browseButton = new JButton("Browse");
        browseButton.setBounds(380, y + 30, 100, 30);
        browseButton.addActionListener(selector);
        add(browseButton);
    }

    /**
     * Opens a file chooser dialog for selecting a file.
     *
     * @param textField The JTextField to update with the selected file's absolute path.
     */
    private void selectFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Opens a file chooser dialog for selecting a directory.
     *
     * @param textField The JTextField to update with the selected folder's absolute path.
     */
    private void selectFolder(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Opens a file chooser dialog for selecting a save location for a file.
     * Automatically appends .xlsx extension if missing.
     *
     * @param textField The JTextField to update with the selected save path.
     */
    private void selectFileSave(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();
            // Automatically append .xlsx if not already present
            if (!path.toLowerCase().endsWith(".xlsx")) {
                path += ".xlsx";
            }
            textField.setText(path);
        }
    }

    /**
     * Appends the .xlsx extension to the JTextField's text if it's not already present.
     *
     * @param textField The JTextField to modify.
     */
    private void appendXlsxExtension(JTextField textField) {
        String text = textField.getText().trim();
        if (!text.isEmpty() && !text.toLowerCase().endsWith(".xlsx")) {
            textField.setText(text + ".xlsx");
        }
    }

    /**
     * Initiates the OCR testing process in a background thread.
     */
    private void startTesting() {
        // Initialize or show the log window
        if (logWindow == null) {
            logWindow = new LogWindow();
        }
        logWindow.setVisible(true); // Ensure it's visible before starting

        // Reset control flags
        pauseRequested = false;
        stopRequested = false;
        logWindow.resetButtons(); // Reset pause/stop button states in log window

        // Disable start button during processing
        startButton.setEnabled(false);
        logTextArea.setText(""); // Clear main log area

        // Ensure the results Excel field has the correct extension before starting
        // This is a final check, but the UI updates (selectFileSave, focusLost) handle it proactively.
        appendXlsxExtension(resultsExcelField);


        new SwingWorker<Void, String>() {
            private final String[] RESULT_COLUMNS = {
                    "Photo", "XLeft", "YTop", "XRight", "YBottom",
                    "Reference Text", "OCR Text", "Result"
            };

            @Override
            protected Void doInBackground() {
                String excelPath = excelPathField.getText().trim();
                String picturesFolder = picturesFolderField.getText().trim();
                String croppedImagesFolder = croppedImagesFolderField.getText().trim();
                String resultsExcel = resultsExcelField.getText().trim();
                String tessDataPath = tessdataPathField.getText().trim();

                if (excelPath.isEmpty() || picturesFolder.isEmpty() ||
                        croppedImagesFolder.isEmpty() || resultsExcel.isEmpty() || tessDataPath.isEmpty()) {
                    publishToLog("Please fill all fields.");
                    return null;
                }

                try (FileInputStream fileInputStream = new FileInputStream(excelPath);
                     Workbook inputWorkbook = new XSSFWorkbook(fileInputStream);
                     Workbook resultsWorkbook = new XSSFWorkbook()) {

                    CellStyle wrapTextStyle = resultsWorkbook.createCellStyle();
                    wrapTextStyle.setWrapText(true);

                    // --- CORRECTED LOGIC: Read TControl sheet first ---
                    Map<String, String> controlFlags = new HashMap<>();
                    Sheet tControlSheet = inputWorkbook.getSheet("TControl");
                    if (tControlSheet == null) {
                        publishToLog("Warning: 'TControl' sheet not found. All sheets will be processed by default.");
                    } else {
                        // Iterate rows in TControl sheet, starting from the second row (index 1) if there's a header
                        for (int i = 1; i <= tControlSheet.getLastRowNum(); i++) {
                            Row row = tControlSheet.getRow(i);
                            if (row != null) {
                                // --- SWAPPED INDICES HERE ---
                                String action = getStringValue(row.getCell(0));    // Column A: Action (RUN/SKIP)
                                String sheetName = getStringValue(row.getCell(1)); // Column B: Sheet Name
                                // --- END SWAPPED INDICES ---
                                if (!sheetName.isEmpty() && !action.isEmpty()) {
                                    controlFlags.put(sheetName.trim().toLowerCase(), action.trim().toUpperCase());
                                }
                            }
                        }
                        publishToLog("TControl sheet loaded. Identified " + controlFlags.size() + " control entries.");
                    }
                    // --- END CORRECTED LOGIC ---

                    // The rest of your doInBackground method remains the same from the previous correction
                    // ... (no changes below this point for sheet processing logic)

                    // Iterate over each sheet in the input workbook
                    for (int sheetIndex = 0; sheetIndex < inputWorkbook.getNumberOfSheets(); sheetIndex++) {
                        if (stopRequested) {
                            publishToLog("Stop requested. Exiting processing.");
                            break;
                        }

                        Sheet currentSheet = inputWorkbook.getSheetAt(sheetIndex);
                        String sheetName = currentSheet.getSheetName().trim();

                        if (sheetName.equalsIgnoreCase("TControl")) {
                            publishToLog("Skipping TControl sheet as it's the control panel: " + sheetName);
                            continue;
                        }

                        // Determine if this sheet should be run based on controlFlags
                        String sheetAction = controlFlags.getOrDefault(sheetName.toLowerCase(), "RUN");
                        if ("SKIP".equals(sheetAction)) {
                            publishToLog("Skipping sheet based on TControl: " + sheetName);
                            continue;
                        }

                        publishToLog("Processing sheet: " + sheetName);

                        Sheet resultsSheet = resultsWorkbook.createSheet(sheetName);
                        Row resultsHeader = resultsSheet.createRow(0);

                        for (int colIdx = 0; colIdx < RESULT_COLUMNS.length; colIdx++) {
                            resultsHeader.createCell(colIdx).setCellValue(RESULT_COLUMNS[colIdx]);
                        }

                        CellStyle currentSheetCellStyle = wrapTextStyle;
                        if (sheetName.equalsIgnoreCase("th")) {
                            org.apache.poi.ss.usermodel.Font thaiFont = resultsWorkbook.createFont();
                            thaiFont.setFontName("Tahoma");
                            thaiFont.setFontHeightInPoints((short) 12);
                            CellStyle thaiStyle = resultsWorkbook.createCellStyle();
                            thaiStyle.cloneStyleFrom(wrapTextStyle);
                            thaiStyle.setFont(thaiFont);
                            currentSheetCellStyle = thaiStyle;
                            SwingUtilities.invokeLater(() -> logWindow.setLogFont(new Font("Tahoma", Font.PLAIN, 12)));
                        } else if (sheetName.equalsIgnoreCase("sa") || sheetName.equalsIgnoreCase("il")) {
                            org.apache.poi.ss.usermodel.Font rtlFont = resultsWorkbook.createFont();
                            rtlFont.setFontName(sheetName.equalsIgnoreCase("il") ? "Arial" : "Noto Sans Arabic");
                            rtlFont.setFontHeightInPoints((short) 12);
                            CellStyle rtlStyle = resultsWorkbook.createCellStyle();
                            rtlStyle.cloneStyleFrom(wrapTextStyle);
                            rtlStyle.setFont(rtlFont);
                            currentSheetCellStyle = rtlStyle;
                            SwingUtilities.invokeLater(() -> logWindow.setLogFont(new Font(sheetName.equalsIgnoreCase("il") ? "Arial" : "Noto Sans Arabic", Font.PLAIN, 12)));
                        } else {
                            SwingUtilities.invokeLater(() -> logWindow.setLogFont(new Font("Aptos Narrow", Font.PLAIN, 12)));
                        }

                        for (int rowIndex = 1; rowIndex <= currentSheet.getLastRowNum(); rowIndex++) {
                            if (stopRequested) {
                                publishToLog("Stop requested for sheet " + sheetName + ". Exiting sheet processing.");
                                break;
                            }

                            synchronized (pauseLock) {
                                while (pauseRequested) {
                                    try {
                                        pauseLock.wait();
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        publishToLog("Processing interrupted during pause.");
                                        return null;
                                    }
                                }
                            }

                            Row dataRow = currentSheet.getRow(rowIndex);
                            if (dataRow == null) {
                                publishToLog("Skipping empty row " + rowIndex + " in sheet " + sheetName);
                                continue;
                            }

                            String tControlFlagInRow = getStringValue(dataRow.getCell(0));
                            String photoName = getStringValue(dataRow.getCell(2));
                            int xLeft = getNumericCellValue(dataRow.getCell(3));
                            int yTop = getNumericCellValue(dataRow.getCell(4));
                            int xRight = getNumericCellValue(dataRow.getCell(5));
                            int yBottom = getNumericCellValue(dataRow.getCell(6));
                            String referenceText = getStringValue(dataRow.getCell(7));

                            Row resultDataRow = resultsSheet.createRow(rowIndex);
                            populateResultRowStaticData(resultDataRow, photoName, xLeft, yTop, xRight, yBottom, referenceText, currentSheetCellStyle);

                            if (!"RUN".equalsIgnoreCase(tControlFlagInRow) || photoName.isEmpty()) {
                                publishToLog(String.format("Skipping row %d in sheet %s (Row TControl != RUN or Photo Name empty).", rowIndex, sheetName));
                                resultDataRow.createCell(7).setCellValue("SKIPPED (Row TControl or Photo Missing)");
                                continue;
                            }

                            if (xLeft < 0 || yTop < 0 || xRight <= xLeft || yBottom <= yTop) {
                                publishToLog(String.format("Skipping row %d in sheet %s (Invalid coordinates: %d,%d,%d,%d).", rowIndex, sheetName, xLeft, yTop, xRight, yBottom));
                                resultDataRow.createCell(7).setCellValue("SKIPPED (Invalid Coords)");
                                continue;
                            }

                            int width = xRight - xLeft;
                            int height = yBottom - yTop;

                            if (sheetName.equalsIgnoreCase("th")) {
                                referenceText = segmentThaiText(referenceText);
                                resultDataRow.getCell(5).setCellValue(referenceText);
                            }

                            File imageFile = new File(picturesFolder, photoName + "_" + sheetName + ".png");

                            BufferedImage originalImage = null;
                            try {
                                if (!imageFile.exists()) {
                                    publishToLog("Image not found: " + imageFile.getAbsolutePath());
                                    resultDataRow.createCell(7).setCellValue("IMAGE NOT FOUND");
                                    continue;
                                }
                                originalImage = ImageIO.read(imageFile);
                            } catch (IOException ioEx) {
                                publishToLog("Error reading image " + imageFile.getName() + ": " + ioEx.getMessage());
                                resultDataRow.createCell(7).setCellValue("IMAGE READ ERROR");
                                continue;
                            }

                            BufferedImage enhancedImage = enhanceImage(originalImage);
                            BufferedImage croppedImage = null;
                            try {
                                croppedImage = enhancedImage.getSubimage(xLeft, yTop, width, height);
                            } catch (RasterFormatException rfEx) {
                                publishToLog(String.format("Error cropping image for row %d in sheet %s: %s. Check coordinates.", rowIndex, sheetName, rfEx.getMessage()));
                                resultDataRow.createCell(7).setCellValue("CROP ERROR");
                                continue;
                            }

                            File outputCroppedFile = new File(croppedImagesFolder, "crop_" + photoName + "_" + sheetName + ".png");
                            try {
                                ImageIO.write(croppedImage, "png", outputCroppedFile);
                            } catch (IOException ioEx) {
                                publishToLog("Error writing cropped image " + outputCroppedFile.getName() + ": " + ioEx.getMessage());
                            }

                            ITesseract tesseractInstance = new Tesseract();
                            tesseractInstance.setDatapath(tessDataPath);
                            String tessLanguage = LANGUAGE_MAP.get(sheetName.toLowerCase());
                            if (tessLanguage != null) {
                                tesseractInstance.setLanguage(tessLanguage);
                            } else {
                                publishToLog("Warning: No Tesseract language mapping found for sheet: " + sheetName + ". Using default.");
                            }

                            String ocrResult = "";
                            try {
                                ocrResult = tesseractInstance.doOCR(croppedImage).trim();
                            } catch (TesseractException ex) {
                                publishToLog(String.format("OCR Error for %s in sheet %s: %s", photoName, sheetName, ex.getMessage()));
                                ex.printStackTrace();
                                ocrResult = "OCR_ERROR";
                            }

                            if (sheetName.equalsIgnoreCase("th")) {
                                ocrResult = segmentThaiText(ocrResult);
                            }
                            Cell ocrCell = resultDataRow.createCell(6);
                            ocrCell.setCellValue(ocrResult);
                            ocrCell.setCellStyle(currentSheetCellStyle);

                            boolean match = referenceText.replaceAll("\\r?\\n", "").equalsIgnoreCase(ocrResult.replaceAll("\\r?\\n", ""));
                            resultDataRow.createCell(7).setCellValue(match ? "CORRECT" : "INCORRECT");
                            publishToLog(String.format("Sheet %s, Row %d (%s): %s", sheetName, rowIndex, photoName, (match ? "CORRECT" : "INCORRECT")));
                        }

                        for (int colIdx = 0; colIdx < RESULT_COLUMNS.length; colIdx++) {
                            resultsSheet.autoSizeColumn(colIdx);
                        }
                    }

                    try (FileOutputStream fileOutputStream = new FileOutputStream(resultsExcel)) {
                        resultsWorkbook.write(fileOutputStream);
                    }
                    publishToLog("Results saved to: " + resultsExcel);
                    JOptionPane.showMessageDialog(
                            null,
                            "Testing complete!\nResults saved to:\n" + resultsExcel,
                            "Info",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (IOException e) {
                    publishToLog("File I/O Error: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception ex) {
                    publishToLog("An unexpected error occurred: " + ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            }
            @Override
            protected void process(List<String> chunks) {
                // Update log text areas on the EDT
                chunks.forEach(msg -> {
                    logTextArea.append(msg + "\n"); // Main window log
                    logWindow.appendLog(msg + "\n"); // Floating log window
                });
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                // Re-enable start button when the task is done (finished or cancelled)
                startButton.setEnabled(true);
                if (logWindow != null) { // Check if logWindow was initialized
                    logWindow.disableButtons(); // Disable pause/stop buttons once done
                }
                try {
                    get(); // This will re-throw any exceptions caught by doInBackground
                } catch (InterruptedException | ExecutionException e) {
                    publishToLog("Background task error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            /**
             * Helper to populate the static data for a result row.
             */
            private void populateResultRowStaticData(Row row, String photoName, int xLeft, int yTop, int xRight, int yBottom, String refText, CellStyle style) {
                row.createCell(0).setCellValue(photoName);
                row.createCell(1).setCellValue(xLeft);
                row.createCell(2).setCellValue(yTop);
                row.createCell(3).setCellValue(xRight);
                row.createCell(4).setCellValue(yBottom);
                Cell refTextCell = row.createCell(5);
                refTextCell.setCellValue(refText);
                refTextCell.setCellStyle(style);
            }

            /**
             * Sends a message to be published to the UI log areas.
             * This method is part of the SwingWorker and uses its `publish` method.
             * @param msg The message string.
             */
            private void publishToLog(String msg) {
                publish(msg); // Calls SwingWorker's own publish method
            }
        }.execute(); // Start the SwingWorker
    }

    /**
     * Enhances a given BufferedImage for better OCR readability.
     * Steps include: grayscale conversion, contrast adjustment, and sharpening.
     *
     * @param originalImage The input BufferedImage.
     * @return An enhanced BufferedImage.
     */
    private BufferedImage enhanceImage(BufferedImage originalImage) {
        // Convert to grayscale
        BufferedImage grayImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        // Adjust brightness and contrast
        // ScaleFactor (1.2f): multiplies pixel values by 1.2, making it brighter/higher contrast
        // Offset (15): adds 15 to pixel values, further brightening
        grayImage = new RescaleOp(1.2f, 15, null).filter(grayImage, null);

        // Sharpen the image using a 3x3 kernel
        float[] kernelMatrix = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };
        // This kernel emphasizes central pixel over neighbors, increasing sharpness
        return new ConvolveOp(new Kernel(3, 3, kernelMatrix)).filter(grayImage, null);
    }

    /**
     * Segments Thai text into words using ICU4J's BreakIterator.
     * This is crucial for Thai as it doesn't use spaces between words.
     *
     * @param text The input Thai text.
     * @return The segmented Thai text with spaces between words.
     */
    private static String segmentThaiText(String text) {
        // Get a word instance for Thai locale
        BreakIterator iterator = BreakIterator.getWordInstance(new Locale("th", "TH"));
        iterator.setText(text);
        StringBuilder segmentedText = new StringBuilder();
        int start = iterator.first();
        int end;
        while ((end = iterator.next()) != BreakIterator.DONE) {
            String word = text.substring(start, end).trim();
            if (!word.isEmpty()) {
                segmentedText.append(word).append(" ");
            }
            start = end;
        }
        return segmentedText.toString().trim(); // Remove trailing space
    }

    /**
     * Helper method to safely retrieve String value from an Excel cell,
     * handling various cell types (String, Numeric, Boolean, Formula, Blank).
     *
     * @param cell The Excel cell.
     * @return The cell's value as a String, or an empty string if null/unsupported type.
     */
    private String getStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    publish("Error in getStringValue for formula cell: " + e.getMessage());
                    return "";
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Helper method to safely retrieve an integer value from an Excel cell.
     * Handles numeric cells directly and attempts to parse strings to integers.
     *
     * @param cell The Excel cell.
     * @return The cell's value as an int, or 0 if null/non-numeric/unparsable.
     */
    private int getNumericCellValue(Cell cell) {
        if (cell == null) {
            return 0;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    publish("Warning: Non-numeric value '" + cell.getStringCellValue() + "' found in coordinate cell. Using 0.");
                    return 0;
                }
            default:
                return 0;
        }
    }

    /**
     * Publishes a message to the main application's log text area.
     * This method is thread-safe as it uses SwingUtilities.invokeLater.
     *
     * @param msg The message to append.
     */
    private void publish(String msg) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(msg + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }

    /**
     * Inner class representing a separate floating log window with Pause/Stop controls.
     */
    private class LogWindow extends JDialog {
        private final JTextArea area = new JTextArea();
        private final JButton pauseBtn = new JButton("Pause");
        private final JButton stopBtn = new JButton("Stop");
        private static final int MAX_LOG_LINES = 1000; // Limit for log area

        /**
         * Constructs the LogWindow dialog.
         */
        public LogWindow() {
            super((java.awt.Frame) null, "Processing Log", false); // Non-modal dialog
            setSize(800, 600);
            setLocationRelativeTo(null); // Center relative to main window initially
            setLayout(new BorderLayout());

            area.setEditable(false);
            area.setFont(new Font("Aptos Narrow", Font.PLAIN, 12));
            add(new JScrollPane(area), BorderLayout.CENTER);

            JPanel controlPanel = new JPanel();
            controlPanel.add(pauseBtn);
            controlPanel.add(stopBtn);
            add(controlPanel, BorderLayout.SOUTH);

            pauseBtn.addActionListener(e -> togglePause());
            stopBtn.addActionListener(e -> requestStop());

            // Add window listener to properly handle closing the log window
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    // If the user closes the log window, request stop if testing is active
                    if (!startButton.isEnabled()) { // If testing is active (startButton is disabled)
                        requestStop();
                    }
                }
            });
        }

        /**
         * Appends a message to the log area and manages its size.
         *
         * @param msg The message to append.
         */
        public void appendLog(String msg) {
            SwingUtilities.invokeLater(() -> {
                area.append(msg);
                // Simple line limit management to prevent OOM
                int numLines = area.getLineCount();
                if (numLines > MAX_LOG_LINES + 50) { // Keep some buffer
                    try {
                        // Remove oldest lines to keep the log manageable
                        int endOfFirstLine = area.getLineEndOffset(numLines - MAX_LOG_LINES);
                        area.replaceRange("", 0, endOfFirstLine);
                    } catch (javax.swing.text.BadLocationException e) {
                        // Handle potential error, though unlikely with proper indexing
                    }
                }
                area.setCaretPosition(area.getDocument().getLength()); // Scroll to bottom
            });
        }

        /**
         * Toggles the pause state of the background processing.
         */
        private void togglePause() {
            if (!pauseRequested) {
                pauseRequested = true;
                pauseBtn.setText("Resume");
                appendLog("Processing paused.\n");
            } else {
                synchronized (pauseLock) {
                    pauseRequested = false;
                    pauseLock.notifyAll(); // Notify waiting thread to resume
                }
                pauseBtn.setText("Pause");
                appendLog("Processing resumed.\n");
            }
        }

        /**
         * Requests the background processing to stop.
         */
        private void requestStop() {
            stopRequested = true;
            disableButtons(); // Disable buttons immediately upon stop request
            synchronized (pauseLock) {
                pauseRequested = false; // Ensure it's not paused if stop is requested
                pauseLock.notifyAll(); // Wake up any waiting thread
            }
            appendLog("Stop requested—finishing current operation and skipping rest.\n");
        }

        /**
         * Sets the font for the log area. Useful for language-specific fonts.
         * @param font The Font to set.
         */
        public void setLogFont(Font font) {
            area.setFont(font);
        }

        /**
         * Resets the pause/stop buttons to their initial state.
         */
        public void resetButtons() {
            pauseBtn.setText("Pause");
            pauseBtn.setEnabled(true);
            stopBtn.setEnabled(true);
        }

        /**
         * Disables the pause and stop buttons.
         */
        public void disableButtons() {
            pauseBtn.setEnabled(false);
            stopBtn.setEnabled(false);
        }
    }

    /**
     * Sets the default UI font for all Swing components.
     * @param fontResource The FontUIResource to use.
     */
    public static void setUIFont(FontUIResource fontResource) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontResource);
            }
        }
    }

    public static void main(String[] args) {
        // Set a consistent UI font before creating any Swing components
        setUIFont(new FontUIResource(new Font("Aptos Narrow", Font.PLAIN, 12)));
        SwingUtilities.invokeLater(() -> new MenuFullAuto().setVisible(true));
    }
}