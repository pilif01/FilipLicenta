package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellReference;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ExcelProcessor extends JFrame {

    private static final String TCONTROL_VALUE = "RUN";
    private static final int EMPTY_COLUMNS_COUNT = 4; // These are columns 4, 5, 6, 7 (indices 3, 4, 5, 6)

    // Fixed column indices based on your new requirement
    private static final int TCONTROL_COL_INDEX = 0; // Column A (index 0)
    private static final int TEXT_RESOURCE_ID_COL_INDEX = 1; // Column B (index 1)
    private static final int PHOTO_NAME_COL_INDEX = 2; // Column C (index 2)
    private static final int FIRST_LANGUAGE_COL_INDEX = 3; // Column E (index 4)

    // Defined order of languages
    private static final List<String> LANGUAGE_ORDER = Arrays.asList(
            "DE", "GB", "US", "SA", "HK", "BG", "CZ", "CN", "GR", "TW", "DK", "FI", "FR", "IL", "HR", "HU", "ID",
            "IT", "JP", "KR", "MY", "NL", "NO", "PL", "BR", "PT", "RO", "RU", "SK", "SI", "ES", "RS", "SE", "TH",
            "TR", "UA", "VN"
    );

    // UI Components
    private JTextField inputFilePathField;
    private JTextField outputFilePathField;
    private JButton selectInputFileButton;
    private JButton selectOutputFileButton;
    private JButton processButton;
    private JTextArea statusArea;
    private JScrollPane scrollPane;

    private String inputFilePath;
    private String outputFilePath;

    public ExcelProcessor() {
        super("Excel Processor"); // Window title
        setupUI();
        setupListeners();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close only this window
        setSize(600, 350); // Window size
        setLocationRelativeTo(null); // Center the window on the screen
        setVisible(true); // Make the window visible
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10)); // Layout with spacing

        // Panel for file path selection
        JPanel fileSelectionPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        fileSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        fileSelectionPanel.add(new JLabel("Input Excel File:"));
        inputFilePathField = new JTextField(30);
        inputFilePathField.setEditable(false); // User cannot type directly
        fileSelectionPanel.add(inputFilePathField);
        selectInputFileButton = new JButton("Select File...");
        fileSelectionPanel.add(selectInputFileButton);

        fileSelectionPanel.add(new JLabel("Output Excel File:"));
        outputFilePathField = new JTextField(30);
        outputFilePathField.setEditable(false);
        fileSelectionPanel.add(outputFilePathField);
        selectOutputFileButton = new JButton("Select Location...");
        fileSelectionPanel.add(selectOutputFileButton);

        add(fileSelectionPanel, BorderLayout.NORTH);

        // Panel for the process button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        processButton = new JButton("Process Excel");
        processButton.setEnabled(false); // Disabled initially
        buttonPanel.add(processButton);
        add(buttonPanel, BorderLayout.CENTER);

        // Status/log area
        statusArea = new JTextArea(8, 40);
        statusArea.setEditable(false);
        scrollPane = new JScrollPane(statusArea);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        selectInputFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select input Excel file");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
            int userSelection = fileChooser.showOpenDialog(ExcelProcessor.this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                inputFilePath = selectedFile.getAbsolutePath();
                inputFilePathField.setText(inputFilePath);
                checkProcessButtonStatus();
            }
        });

        selectOutputFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select location for output Excel file");
            fileChooser.setSelectedFile(new File("output_excel.xlsx"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
            int userSelection = fileChooser.showSaveDialog(ExcelProcessor.this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                outputFilePath = selectedFile.getAbsolutePath();
                if (!outputFilePath.toLowerCase().endsWith(".xlsx")) {
                    outputFilePath += ".xlsx";
                }
                outputFilePathField.setText(outputFilePath);
                checkProcessButtonStatus();
            }
        });

        processButton.addActionListener(e -> {
            setUIEnabled(false);
            statusArea.setText("");
            appendStatus("Starting Excel file processing...");

            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        processExcelLogic(inputFilePath, outputFilePath);
                        publish("Excel file successfully generated at: " + outputFilePath);
                    } catch (IOException ex) {
                        publish("Error processing Excel file: " + ex.getMessage() +
                                "\nPossible reasons: The file might be open, or you don't have write permissions to the selected location.");
                        ex.printStackTrace();
                    } catch (IllegalStateException ex) {
                        publish("Error: " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        publish("An unexpected error occurred: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String message : chunks) {
                        appendStatus(message);
                    }
                }

                @Override
                protected void done() {
                    setUIEnabled(true);
                    try {
                        get();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        appendStatus("Processing interrupted: " + ex.getMessage());
                    } catch (ExecutionException ex) {
                        appendStatus("An error occurred during processing: " + ex.getCause().getMessage());
                        ex.getCause().printStackTrace();
                    }
                    appendStatus("Process finished.");
                }

                private void processExcelLogic(String input, String output) throws IOException, IllegalStateException {
                    Map<String, List<Map<String, String>>> dataByLanguage = new LinkedHashMap<>();
                    for (String lang : LANGUAGE_ORDER) {
                        dataByLanguage.put(lang, new ArrayList<>());
                    }

                    Map<String, Integer> languageColumnMap = new HashMap<>(); // To store column index for each language
                    int totalRows = 0;

                    publish("Reading data from input file...");
                    try (FileInputStream fis = new FileInputStream(input);
                         Workbook workbook = new XSSFWorkbook(fis)) {

                        Sheet sheet = workbook.getSheetAt(0);
                        totalRows = sheet.getLastRowNum();

                        Row headerRow = sheet.getRow(0);
                        if (headerRow == null) {
                            throw new IllegalStateException("Input Excel file is empty or missing header row.");
                        }

                        int lastCol = headerRow.getLastCellNum();
                        for (int colIndex = FIRST_LANGUAGE_COL_INDEX; colIndex < lastCol; colIndex++) {
                            Cell headerCell = headerRow.getCell(colIndex);
                            if (headerCell != null && headerCell.getCellType() == CellType.STRING) {
                                String headerValue = headerCell.getStringCellValue();
                                if (LANGUAGE_ORDER.contains(headerValue)) {
                                    languageColumnMap.put(headerValue, colIndex);
                                }
                            }
                        }

                        List<String> missingLanguagesInInput = new ArrayList<>();
                        for (String lang : LANGUAGE_ORDER) {
                            if (!languageColumnMap.containsKey(lang)) {
                                missingLanguagesInInput.add(lang);
                            }
                        }
                        if (!missingLanguagesInInput.isEmpty()) {
                            publish("Warning: The following languages from the defined order were not found in the input file's header: " + String.join(", ", missingLanguagesInInput));
                        }


                        for (int r = 1; r <= totalRows; r++) {
                            Row row = sheet.getRow(r);
                            if (row == null) continue;

                            String tControl = getCellValue(row.getCell(TCONTROL_COL_INDEX));
                            String textResourceId = getCellValue(row.getCell(TEXT_RESOURCE_ID_COL_INDEX));
                            String photoName = getCellValue(row.getCell(PHOTO_NAME_COL_INDEX));

                            if (tControl.equalsIgnoreCase(TCONTROL_VALUE)) {
                                for (String lang : LANGUAGE_ORDER) {
                                    Integer langColIndex = languageColumnMap.get(lang);

                                    if (langColIndex != null) {
                                        String text = getCellValue(row.getCell(langColIndex));

                                        Map<String, String> rowData = new HashMap<>();
                                        rowData.put("TextResource ID", textResourceId);
                                        rowData.put("Photo name", photoName);
                                        rowData.put("Text", text);

                                        dataByLanguage.get(lang).add(rowData);
                                    }
                                }
                            }
                            publishProgress("Reading", r, totalRows);
                        }
                        publish("\nData reading finished.");
                    }

                    publish("Creating output file...");
                    try (Workbook newWorkbook = new XSSFWorkbook()) {

                        // --- Step 1: Create and populate the TControl sheet ---
                        Sheet tControlSheet = newWorkbook.createSheet("TControl");
                        Row tControlHeader = tControlSheet.createRow(0);
                        tControlHeader.createCell(0).setCellValue("TControl");
                        tControlHeader.createCell(1).setCellValue("Languages");

                        int rowNum = 1;
                        for (String lang : LANGUAGE_ORDER) {
                            if (languageColumnMap.containsKey(lang)) { // Only add if present in input header
                                Row row = tControlSheet.createRow(rowNum++);
                                row.createCell(0).setCellValue(TCONTROL_VALUE);
                                row.createCell(1).setCellValue(lang);
                            }
                        }

                        // --- Step 2: Create and populate individual language sheets ---
                        // Keep track of which sheets were actually created to order them later
                        List<String> sheetsToOrder = new ArrayList<>();
                        sheetsToOrder.add("TControl"); // TControl is always created

                        int currentSheetCreationCount = 0;
                        int totalSheetsToConsider = LANGUAGE_ORDER.size();

                        for (String lang : LANGUAGE_ORDER) {
                            if (languageColumnMap.containsKey(lang)) {
                                Sheet langSheet = newWorkbook.createSheet(lang); // Create sheet here
                                sheetsToOrder.add(lang); // Add to the list for ordering

                                Row langHeader = langSheet.createRow(0);
                                langHeader.createCell(0).setCellValue("TControl");
                                langHeader.createCell(1).setCellValue("TextResource ID");
                                langHeader.createCell(2).setCellValue("Photo name");
                                langHeader.createCell(3 + EMPTY_COLUMNS_COUNT).setCellValue("Text");

                                int langRowNum = 1;
                                for (Map<String, String> rowData : dataByLanguage.get(lang)) {
                                    Row row = langSheet.createRow(langRowNum++);
                                    row.createCell(0).setCellValue(TCONTROL_VALUE);
                                    row.createCell(1).setCellValue(rowData.get("TextResource ID"));
                                    row.createCell(2).setCellValue(rowData.get("Photo name"));
                                    row.createCell(3 + EMPTY_COLUMNS_COUNT).setCellValue(rowData.get("Text"));
                                }
                                currentSheetCreationCount++;
                            } else {
                                publish("Skipping creation of sheet for language '" + lang + "' as it was not found in the input file's header.");
                            }
                            publishProgress("Writing sheets", currentSheetCreationCount, totalSheetsToConsider);
                        }
                        publish("\nSheets created.");

                        // --- Step 3: Explicitly set the sheet order ---
                        // Ensure TControl is first
                        int tControlIndex = newWorkbook.getSheetIndex("TControl");
                        if (tControlIndex != 0) { // Only move if it's not already first
                            newWorkbook.setSheetOrder("TControl", 0);
                        }

                        // Set order for language sheets
                        int currentOrderPosition = 1; // Start after TControl sheet
                        for (String lang : LANGUAGE_ORDER) {
                            if (languageColumnMap.containsKey(lang)) { // Only order sheets that were actually created
                                int currentSheetIndex = newWorkbook.getSheetIndex(lang);
                                if (currentSheetIndex != -1 && currentSheetIndex != currentOrderPosition) {
                                    newWorkbook.setSheetOrder(lang, currentOrderPosition);
                                }
                                currentOrderPosition++;
                            }
                        }
                        publish("Sheet order adjusted.");


                        try (FileOutputStream fos = new FileOutputStream(output)) {
                            newWorkbook.write(fos);
                        }
                    }
                }

                private void publishProgress(String task, int current, int total) {
                    int percentage = (int) (((double) current / total) * 100);
                    int barLength = 20;
                    int filledLength = (int) (((double) percentage / 100) * barLength);

                    StringBuilder progressBar = new StringBuilder();
                    progressBar.append(task).append(": [");
                    for (int i = 0; i < barLength; i++) {
                        if (i < filledLength) {
                            progressBar.append("#");
                        } else {
                            progressBar.append(" ");
                        }
                    }
                    progressBar.append("] ").append(percentage).append("% (").append(current).append("/").append(total).append(")");
                    publish(progressBar.toString());
                }

            }.execute();
        });
    }

    private void appendStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    private void checkProcessButtonStatus() {
        processButton.setEnabled(inputFilePath != null && !inputFilePath.isEmpty() &&
                outputFilePath != null && !outputFilePathField.getText().isEmpty());
    }

    private void setUIEnabled(boolean enabled) {
        selectInputFileButton.setEnabled(enabled);
        selectOutputFileButton.setEnabled(enabled);
        processButton.setEnabled(enabled);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return cell.getCachedFormulaResultType() == CellType.STRING ? cell.getStringCellValue() : "";
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ExcelProcessor();
        });
    }
}