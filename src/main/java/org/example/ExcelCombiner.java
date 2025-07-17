package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Assumes .xlsx format for simplicity and modern use
import org.apache.poi.hssf.usermodel.HSSFWorkbook; // For .xls files

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator; // For iterating over rows and cells

/**
 * A Java Swing application to merge two Excel files.
 * It copies content from a second Excel file and appends it
 * to the first Excel file, skipping the first row of the second file.
 *
 * This version also adds a new "tcontrol" column as the first column (index 0)
 * to all sheets in the output file. This column will contain "RUN" for data rows
 * and "tcontrol" for the header row.
 */
public class ExcelCombiner extends JFrame {

    private JTextField sourceFile1Field;
    private JTextField sourceFile2Field;
    private JTextField outputFileField;
    private JTextArea logTextArea;

    public ExcelCombiner() {
        super("Excel File Merger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null); // Center the window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        createUI();
    }

    private void createUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Source File 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("First Excel File (Base Content):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        sourceFile1Field = new JTextField(30);
        sourceFile1Field.setEditable(false);
        panel.add(sourceFile1Field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browse1Button = new JButton("Browse...");
        browse1Button.addActionListener(e -> browseFile(sourceFile1Field));
        panel.add(browse1Button, gbc);

        // Source File 2
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Second Excel File (Content to Append):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        sourceFile2Field = new JTextField(30);
        sourceFile2Field.setEditable(false);
        panel.add(sourceFile2Field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browse2Button = new JButton("Browse...");
        browse2Button.addActionListener(e -> browseFile(sourceFile2Field));
        panel.add(browse2Button, gbc);

        // Output File
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Output Excel File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        outputFileField = new JTextField(30);
        outputFileField.setEditable(false); // User selects save location
        panel.add(outputFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton saveAsButton = new JButton("Save As...");
        saveAsButton.addActionListener(e -> selectSaveFile(outputFileField));
        panel.add(saveAsButton, gbc);

        // Merge Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3; // Span across all columns
        gbc.fill = GridBagConstraints.NONE; // Don't stretch button
        gbc.anchor = GridBagConstraints.CENTER; // Center the button
        JButton mergeButton = new JButton("Merge Excel Files");
        mergeButton.addActionListener(e -> mergeFiles());
        panel.add(mergeButton, gbc);

        // Log Area
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0; // Allow log area to expand vertically
        gbc.fill = GridBagConstraints.BOTH;
        logTextArea = new JTextArea(8, 40);
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        panel.add(scrollPane, gbc);

        add(panel);
    }

    /**
     * Opens a file chooser dialog and sets the selected file path to the given text field.
     * @param textField The JTextField to update with the selected file path.
     */
    private void browseFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Excel File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx", "xls"));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            textField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Opens a save file chooser dialog and sets the selected file path to the given text field.
     * @param textField The JTextField to update with the selected save path.
     */
    private void selectSaveFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Merged Excel File As");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (.xlsx)", "xlsx")); // Suggest .xlsx
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            // Ensure .xlsx extension
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath += ".xlsx";
            }
            textField.setText(filePath);
        }
    }

    /**
     * Initiates the merging process in a SwingWorker to keep the UI responsive.
     */
    private void mergeFiles() {
        String file1Path = sourceFile1Field.getText();
        String file2Path = sourceFile2Field.getText();
        String outputPath = outputFileField.getText();

        if (file1Path.isEmpty() || file2Path.isEmpty() || outputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select all input and output files.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File file1 = new File(file1Path);
        File file2 = new File(file2Path);
        File outputFile = new File(outputPath);

        if (!file1.exists()) {
            JOptionPane.showMessageDialog(this, "First Excel file does not exist: " + file1Path,
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!file2.exists()) {
            JOptionPane.showMessageDialog(this, "Second Excel file does not exist: " + file2Path,
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logTextArea.setText(""); // Clear previous logs
        appendToLog("Starting merge process...");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Pass the output file directly to the merge logic
                    mergeExcelContent(file1, file2, outputFile);
                    publish("Merge completed successfully!");
                    JOptionPane.showMessageDialog(ExcelCombiner.this,
                            "Files merged successfully!\nOutput: " + outputPath,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException | IllegalArgumentException e) {
                    publish("Error during merge: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ExcelCombiner.this,
                            "Error merging files: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    appendToLog(message);
                }
            }

            private void appendToLog(String message) {
                SwingUtilities.invokeLater(() -> {
                    logTextArea.append(message + "\n");
                    logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
                });
            }
        }.execute();
    }

    /**
     * Merges content from sourceFile2 into sourceFile1, creating a new output workbook.
     * A new "tcontrol" column with "RUN" is added as the first column (index 0)
     * to all rows in the output workbook. The first row of sourceFile2 is skipped.
     *
     * @param sourceFile1 The first Excel file (content copied first).
     * @param sourceFile2 The second Excel file (content appended, skipping first row).
     * @param outputFile  The file where the merged content will be saved.
     * @throws IOException If there's an error reading/writing Excel files.
     * @throws IllegalArgumentException If file types are not supported.
     */
    private void mergeExcelContent(File sourceFile1, File sourceFile2, File outputFile) throws IOException, IllegalArgumentException {
        // Use try-with-resources for automatic closing of FileInputStream
        try (FileInputStream fis1 = new FileInputStream(sourceFile1);
             FileInputStream fis2 = new FileInputStream(sourceFile2)) {

            Workbook workbook1 = getWorkbook(fis1, sourceFile1.getName());
            Workbook workbook2 = getWorkbook(fis2, sourceFile2.getName());
            Workbook outputWorkbook = new XSSFWorkbook(); // Create a new workbook for output

            // Iterate through each sheet in the first workbook (workbook1)
            for (int i = 0; i < workbook1.getNumberOfSheets(); i++) {
                Sheet sheet1 = workbook1.getSheetAt(i);
                String sheetName = sheet1.getSheetName();
                Sheet outputSheet = outputWorkbook.createSheet(sheetName); // Create corresponding sheet in output

                appendToLog("Copying content from first file, sheet: " + sheetName);

                // Copy rows from workbook1 to outputWorkbook, adding "tcontrol" column
                int outputRowIndex = 0;
                Iterator<Row> rowIterator1 = sheet1.iterator();
                while (rowIterator1.hasNext()) {
                    Row sourceRow = rowIterator1.next();
                    Row newRow = outputSheet.createRow(outputRowIndex++);

                    // Add "tcontrol" column at index 0
                    Cell tcontrolCell = newRow.createCell(0);
                    // If it's the first row (header), set the "tcontrol" cell to "tcontrol" text
                    if (outputRowIndex == 1) { // outputRowIndex is incremented after row creation, so 1 means first row (index 0)
                        tcontrolCell.setCellValue("tcontrol");
                    } else {
                        tcontrolCell.setCellValue("RUN");
                    }

                    // Copy existing cells, shifting their column index by 1
                    Iterator<Cell> cellIterator = sourceRow.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell oldCell = cellIterator.next();
                        Cell newCell = newRow.createCell(oldCell.getColumnIndex() + 1);
                        copyCell(oldCell, newCell);
                    }
                }

                // Now, append content from workbook2 to the same outputSheet
                Sheet sheet2 = workbook2.getSheet(sheetName); // Find corresponding sheet in workbook2
                if (sheet2 == null) {
                    appendToLog("Sheet '" + sheetName + "' not found in the second Excel file for appending. Skipping append for this sheet.");
                    continue;
                }

                appendToLog("Appending content from second file, sheet: " + sheetName);

                // Iterate through rows of sheet2, starting from the second row (index 1)
                // to skip the header/first row
                Iterator<Row> rowIterator2 = sheet2.iterator();
                if (rowIterator2.hasNext()) {
                    rowIterator2.next(); // Skip the first row (header) of sheet2
                } else {
                    appendToLog("Sheet '" + sheetName + "' in second Excel file is empty. No content to append.");
                    continue;
                }

                while (rowIterator2.hasNext()) {
                    Row sourceRow = rowIterator2.next();
                    Row newRow = outputSheet.createRow(outputRowIndex++); // Continue appending

                    // Add "tcontrol" column at index 0
                    Cell tcontrolCell = newRow.createCell(0);
                    tcontrolCell.setCellValue("RUN");

                    // Copy existing cells, shifting their column index by 1
                    Iterator<Cell> cellIterator = sourceRow.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell oldCell = cellIterator.next();
                        Cell newCell = newRow.createCell(oldCell.getColumnIndex() + 1);
                        copyCell(oldCell, newCell);
                    }
                }
            }

            // Write the new outputWorkbook to the output file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                outputWorkbook.write(fos);
            }

            // Close workbooks
            workbook1.close();
            workbook2.close();
            outputWorkbook.close(); // Close the newly created workbook
        } // fis1 and fis2 are closed automatically
    }

    /**
     * Helper method to get the correct Workbook type based on file extension.
     */
    private Workbook getWorkbook(FileInputStream fis, String fileName) throws IOException, IllegalArgumentException {
        if (fileName.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please use .xls or .xlsx files.");
        }
    }

    /**
     * Helper method to copy cell content and style.
     * Note: Copying styles precisely can be complex, this is a basic copy.
     */
    private void copyCell(Cell oldCell, Cell newCell) {
        // Set cell style (basic copy, might not copy all aspects like borders, etc.)
        // This can be complex if you need to copy all style properties.
        // For now, we'll just copy the cell value.
        // newCell.setCellStyle(oldCell.getCellStyle()); // Uncomment if you want to copy styles

        // Set cell value based on type
        switch (oldCell.getCellType()) {
            case STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(oldCell)) {
                    newCell.setCellValue(oldCell.getDateCellValue());
                } else {
                    newCell.setCellValue(oldCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                newCell.setCellValue(oldCell.getBooleanCellValue());
                break;
            case FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            case BLANK:
                // Do nothing for blank cells
                break;
            case ERROR:
                newCell.setCellErrorValue(oldCell.getErrorCellValue());
                break;
            default:
                newCell.setCellValue(oldCell.toString()); // Fallback for other types
                break;
        }
    }

    private void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExcelCombiner().setVisible(true));
    }
}
