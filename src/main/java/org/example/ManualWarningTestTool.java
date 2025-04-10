package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ibm.icu.text.BreakIterator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManualWarningTestTool {

    private static JFrame frame;
    private static JPanel mainPanel;
    private static JComboBox<String> sheetSelector;
    private static JList<String> warningList;
    private static JLabel mainImageLabel;
    private static JLabel referenceLabel, ocrLabel;
    private static JButton selectExcelButton, selectImageFolderButton, configureColumnsButton, okButton, nokButton, customResultButton;
    private static JTextField customResultTextField;
    private static String baseExcelPath = "", baseImagePath = "";
    private static List<WarningData> warningRows;
    private static int currentRow;

    // Default columns
    private static String warningNameColumn = "Warning Name";
    private static String expectedTextColumn = "Expected Text";
    private static String ocrTextColumn = "OCR Text";
    private static String resultColumn = "Result";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ManualWarningTestTool app = new ManualWarningTestTool();
            app.createAndShowGUI();
        });
    }

    // Create and show GUI components
    private void createAndShowGUI() {
        frame = new JFrame("Automated Warning Testing App");
        mainPanel = new JPanel(new BorderLayout());

        // UI Components
        sheetSelector = new JComboBox<>();
        sheetSelector.addItem("Please select an Excel file and configure columns");
        sheetSelector.setEnabled(false); // Disable until Excel is loaded

        warningList = new JList<>();
        mainImageLabel = new JLabel();

        // Reference and OCR text labels with larger font
        referenceLabel = new JLabel("Expected Text: ");
        ocrLabel = new JLabel("OCR Text: ");

        // Initially use the default font for non-Thai and non-Arab sheets (Aptos Narrow)
        Font defaultFont = new Font("Aptos Narrow", Font.BOLD, 20);
        referenceLabel.setFont(defaultFont);
        ocrLabel.setFont(defaultFont);

        selectExcelButton = new JButton("Select Excel File");
        selectImageFolderButton = new JButton("Select Image Folder");
        configureColumnsButton = new JButton("Configure Columns");

        okButton = new JButton("OK");
        nokButton = new JButton("NOK");
        customResultButton = new JButton("Save Custom Result");
        customResultTextField = new JTextField(20);

        // Set action listeners for buttons
        selectExcelButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                baseExcelPath = selectedFile.getAbsolutePath();
                updateSheetSelector(baseExcelPath);
            }
        });

        selectImageFolderButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                baseImagePath = folderChooser.getSelectedFile().getAbsolutePath();
                updateWarningList();
            }
        });

        configureColumnsButton.addActionListener(e -> openColumnCustomizationWindow());

        okButton.addActionListener(e -> saveResult("OK"));
        nokButton.addActionListener(e -> saveResult("NOK"));
        customResultButton.addActionListener(e -> saveResult(customResultTextField.getText()));

        // Add components to main panel
        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        mainPanel.add(createRightPanel(), BorderLayout.CENTER);

        // Add key binding for 'c' key to save "CORRECT"
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('c'), "saveCorrect");
        frame.getRootPane().getActionMap().put("saveCorrect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveResult("CORRECT");
            }
        });

        // Set frame properties
        frame.add(mainPanel);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateSheetSelector(String filePath) {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            sheetSelector.removeAllItems();
            sheetSelector.addItem("Select a sheet");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetSelector.addItem(workbook.getSheetName(i));
            }
            sheetSelector.setEnabled(true);
            sheetSelector.addActionListener(e -> loadDataFromXLSX(filePath, (String) sheetSelector.getSelectedItem()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDataFromXLSX(String filePath, String sheetName) {
        warningRows = new ArrayList<>(); // Initialize warningRows

        // Log the base image path to ensure it's correct
        System.out.println("Base Image Path: " + baseImagePath);

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheet(sheetName);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Starting from row 2 (index 1)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                WarningData warningData = new WarningData();
                warningData.setWarningName(getStringValueFromCell(row.getCell(getColumnIndex(warningNameColumn))));
                // Read expected text preserving newlines.
                warningData.setExpectedText(getStringValueFromCell(row.getCell(getColumnIndex(expectedTextColumn))));
                warningData.setOcrText(getStringValueFromCell(row.getCell(getColumnIndex(ocrTextColumn))));
                warningData.setResult(getStringValueFromCell(row.getCell(getColumnIndex(resultColumn))));

                // Only add valid rows
                if (warningData.getWarningName() != null && !warningData.getWarningName().isEmpty()) {
                    // Use sheet name as the language identifier for image file naming
                    String language = sheetName.toLowerCase();
                    String imagePath = baseImagePath + File.separator + warningData.getWarningName() + "_" + language + ".png";
                    System.out.println("Image Path: " + imagePath);

                    warningData.setImagePath(imagePath);
                    warningRows.add(warningData);
                }
            }
            updateWarningList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWarningList() {
        if (warningRows == null || warningRows.isEmpty()) {
            System.out.println("Warning rows are empty or not initialized.");
            return;
        }

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (WarningData data : warningRows) {
            String displayText = data.getWarningName() + " - " + data.getResult();
            if ("correct".equalsIgnoreCase(data.getResult())) {
                displayText = "<html><font color='green'>" + displayText + "</font></html>";
            } else if ("incorrect".equalsIgnoreCase(data.getResult())) {
                displayText = "<html><font color='red'>" + displayText + "</font></html>";
            }
            listModel.addElement(displayText);
        }
        warningList.setModel(listModel);

        System.out.println("Updated warning list with " + warningRows.size() + " items.");

        warningList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = warningList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    currentRow = selectedIndex;
                    displayRowDetails(currentRow);
                }
            }
        });
    }

    /**
     * Displays the details for the selected row.
     *
     * For Thai text, segmentation is applied.
     * For Arabic, Saudi (SA), and Israeli (IL) texts, the text is wrapped in HTML with inline CSS that forces
     * right-to-left direction while keeping the text left aligned. This prevents English (LTR) characters
     * from being repositioned unexpectedly.
     *
     * In addition, the code replaces new line characters with HTML <br> so that the text is displayed
     * exactly as in the Excel.
     */
    private void displayRowDetails(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < warningRows.size()) {
            WarningData data = warningRows.get(rowIndex);
            String expectedText = data.getExpectedText();
            String ocrText = data.getOcrText();

            // Replace newline characters with <br> tags for proper HTML rendering.
            String formattedExpected = expectedText.replaceAll("\\r?\\n", "<br>");
            String formattedOCR = ocrText.replaceAll("\\r?\\n", "<br>");

            String selectedSheet = (String) sheetSelector.getSelectedItem();
            String expectedHtml;
            String ocrHtml;

            if (selectedSheet != null) {
                if (selectedSheet.equalsIgnoreCase("th")) {
                    // For Thai: use Tahoma and segment text.
                    Font thaiFont = new Font("Tahoma", Font.PLAIN, 20);
                    referenceLabel.setFont(thaiFont);
                    ocrLabel.setFont(thaiFont);
                    // Optionally perform additional segmentation.
                    formattedExpected = segmentThaiText(formattedExpected);
                    formattedOCR = segmentThaiText(formattedOCR);
                    expectedHtml = "<html><b>Expected Text:</b><br><div style='text-align: left;'>" + formattedExpected + "</div></html>";
                    ocrHtml = "<html><b>OCR Result:</b><br><div style='text-align: left;'>" + formattedOCR + "</div></html>";
                } else if (selectedSheet.equalsIgnoreCase("arab") ||
                        selectedSheet.equalsIgnoreCase("sa") ||
                        selectedSheet.equalsIgnoreCase("il")) {
                    // For RTL languages: set appropriate font and wrap text in an HTML div that enforces RTL.
                    Font rtlFont;
                    if (selectedSheet.equalsIgnoreCase("il")) {
                        rtlFont = new Font("Arial", Font.PLAIN, 20);
                    } else {
                        rtlFont = new Font("Noto Sans Arabic", Font.PLAIN, 20);
                    }
                    referenceLabel.setFont(rtlFont);
                    ocrLabel.setFont(rtlFont);
                    expectedHtml = "<html><b>Expected Text:</b><br><div style='direction: rtl; text-align: left;'>" + formattedExpected + "</div></html>";
                    ocrHtml = "<html><b>OCR Result:</b><br><div style='direction: rtl; text-align: left;'>" + formattedOCR + "</div></html>";
                    referenceLabel.setHorizontalAlignment(SwingConstants.LEFT);
                    ocrLabel.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    // For other languages, use default font and left-to-right styling.
                    Font defaultFont = new Font("Aptos Narrow", Font.BOLD, 20);
                    referenceLabel.setFont(defaultFont);
                    ocrLabel.setFont(defaultFont);
                    expectedHtml = "<html><b>Expected Text:</b><br><div style='text-align: left;'>" + formattedExpected + "</div></html>";
                    ocrHtml = "<html><b>OCR Result:</b><br><div style='text-align: left;'>" + formattedOCR + "</div></html>";
                }
            } else {
                // Fallback styling if no sheet is selected.
                expectedHtml = "<html><b>Expected Text:</b><br>" + formattedExpected + "</html>";
                ocrHtml = "<html><b>OCR Result:</b><br>" + formattedOCR + "</html>";
            }

            // Load and display the corresponding image.
            ImageIcon warningImage = new ImageIcon(data.getImagePath());
            mainImageLabel.setIcon(warningImage);
            mainImageLabel.setPreferredSize(new Dimension(warningImage.getIconWidth(), warningImage.getIconHeight()));
            mainImageLabel.revalidate();
            mainImageLabel.repaint();

            // Set the labels.
            referenceLabel.setText(expectedHtml);
            ocrLabel.setText(ocrHtml);
        }
    }

    private void saveResult(String result) {
        if (currentRow >= 0 && currentRow < warningRows.size()) {
            WarningData data = warningRows.get(currentRow);
            data.setResult(result);

            try (FileInputStream fis = new FileInputStream(new File(baseExcelPath));
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                XSSFSheet sheet = workbook.getSheet(sheetSelector.getSelectedItem().toString());
                Row row = sheet.getRow(currentRow + 1);
                row.getCell(getColumnIndex(resultColumn)).setCellValue(result);

                try (FileOutputStream fos = new FileOutputStream(new File(baseExcelPath))) {
                    workbook.write(fos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateWarningList();
        }
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(sheetSelector, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(warningList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(selectExcelButton);
        buttonPanel.add(selectImageFolderButton);
        buttonPanel.add(configureColumnsButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Add the image in a scroll pane to handle large images.
        JScrollPane scrollPane = new JScrollPane(mainImageLabel);
        scrollPane.setPreferredSize(new Dimension(700, 700));
        rightPanel.add(scrollPane, BorderLayout.NORTH);

        // Container for reference and OCR text labels.
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(referenceLabel);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(ocrLabel);
        rightPanel.add(textPanel, BorderLayout.CENTER);

        // Panel with result buttons and custom result input.
        JPanel resultPanel = new JPanel(new FlowLayout());
        resultPanel.add(okButton);
        resultPanel.add(nokButton);
        resultPanel.add(customResultTextField);
        resultPanel.add(customResultButton);
        rightPanel.add(resultPanel, BorderLayout.SOUTH);

        return rightPanel;
    }

    private String getStringValueFromCell(Cell cell) {
        if (cell == null)
            return "";
        if (cell.getCellType() == CellType.STRING)
            return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC)
            return String.valueOf(cell.getNumericCellValue());
        return "";
    }

    private int getColumnIndex(String columnName) {
        switch (columnName) {
            case "Warning Name":
                return 0;
            case "Expected Text":
                return 1;
            case "OCR Text":
                return 2;
            case "Result":
                return 3;
            default:
                return -1;
        }
    }

    private void openColumnCustomizationWindow() {
        String[] columnNames = {"Warning Name", "Expected Text", "OCR Text", "Result"};
        JPanel panel = new JPanel(new GridLayout(columnNames.length, 2));
        for (String column : columnNames) {
            panel.add(new JLabel(column));
            panel.add(new JTextField(getColumnIndex(column) + ""));
        }
        JOptionPane.showConfirmDialog(frame, panel, "Configure Columns", JOptionPane.OK_CANCEL_OPTION);
    }

    // Helper method for Thai language segmentation using ICU4J's BreakIterator.
    private String segmentThaiText(String text) {
        BreakIterator wordIterator = BreakIterator.getWordInstance(new Locale("th", "TH"));
        wordIterator.setText(text);
        StringBuilder segmented = new StringBuilder();
        int start = wordIterator.first();
        for (int end = wordIterator.next(); end != BreakIterator.DONE; start = end, end = wordIterator.next()) {
            String word = text.substring(start, end).trim();
            if (!word.isEmpty()) {
                segmented.append(word).append(" ");
            }
        }
        return segmented.toString().trim();
    }

    // Inner class to store warning data.
    class WarningData {
        private String warningName;
        private String expectedText;
        private String ocrText;
        private String result;
        private String imagePath;

        public String getWarningName() {
            return warningName;
        }

        public void setWarningName(String warningName) {
            this.warningName = warningName;
        }

        public String getExpectedText() {
            return expectedText;
        }

        public void setExpectedText(String expectedText) {
            this.expectedText = expectedText;
        }

        public String getOcrText() {
            return ocrText;
        }

        public void setOcrText(String ocrText) {
            this.ocrText = ocrText;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }
    }
}
