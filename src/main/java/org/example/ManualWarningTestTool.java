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

    // Create and show the GUI components
    private void createAndShowGUI() {
        frame = new JFrame("Automated Warning Testing App");
        mainPanel = new JPanel(new BorderLayout());

        // UI Components
        sheetSelector = new JComboBox<>();
        sheetSelector.addItem("Please select an Excel file and configure columns");
        sheetSelector.setEnabled(false); // Disabled until an Excel is loaded

        warningList = new JList<>();
        mainImageLabel = new JLabel();

        // Labels for expected & OCR text
        referenceLabel = new JLabel("Expected Text: ");
        ocrLabel = new JLabel("OCR Text: ");

        // Set default font (used for non-Thai/non-Arab sheets)
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

        // Set up button action listeners
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

        // Assemble main panel
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

        // Finalize frame
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

        System.out.println("Base Image Path: " + baseImagePath);

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheet(sheetName);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // starting from row 2 (index 1)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                WarningData warningData = new WarningData();
                warningData.setWarningName(getStringValueFromCell(row.getCell(getColumnIndex(warningNameColumn))));
                // Read expected text preserving newlines
                warningData.setExpectedText(getStringValueFromCell(row.getCell(getColumnIndex(expectedTextColumn))));
                warningData.setOcrText(getStringValueFromCell(row.getCell(getColumnIndex(ocrTextColumn))));
                warningData.setResult(getStringValueFromCell(row.getCell(getColumnIndex(resultColumn))));

                // Only add valid rows
                if (warningData.getWarningName() != null && !warningData.getWarningName().isEmpty()) {
                    // Use sheet name as language identifier for image file naming
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
     * Displays details for the selected row.
     * For Thai, the text is segmented using ICU4J before diff highlighting is applied.
     * New line characters are replaced with HTML <br> tags.
     */
    private void displayRowDetails(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < warningRows.size()) {
            WarningData data = warningRows.get(rowIndex);
            String expectedText = data.getExpectedText();
            String ocrText = data.getOcrText();

            String selectedSheet = (String) sheetSelector.getSelectedItem();

            String formattedExpected;
            String formattedOCR;
            String expectedHtml;
            String ocrHtml;

            // For Thai, we first segment the text before applying diff highlighting
            if (selectedSheet != null && selectedSheet.equalsIgnoreCase("th")) {
                // Segment Thai text
                String segExpected = segmentThaiText(expectedText);
                String segOCR = segmentThaiText(ocrText);

                // Apply diff highlighting if the result is marked wrong and texts differ
                if (data.getResult() != null &&
                        (data.getResult().equalsIgnoreCase("NOK") || data.getResult().equalsIgnoreCase("incorrect"))
                        && !expectedText.equals(ocrText)) {
                    String[] diffHTML = computeDiffHighlight(segExpected, segOCR);
                    formattedExpected = diffHTML[0];
                    formattedOCR = diffHTML[1];
                } else {
                    formattedExpected = segExpected;
                    formattedOCR = segOCR;
                }
                // Replace newline characters with <br> for proper rendering
                formattedExpected = formattedExpected.replaceAll("\\r?\\n", "<br>");
                formattedOCR = formattedOCR.replaceAll("\\r?\\n", "<br>");

                // Use Thai font
                Font thaiFont = new Font("Tahoma", Font.PLAIN, 20);
                referenceLabel.setFont(thaiFont);
                ocrLabel.setFont(thaiFont);

                expectedHtml = "<html><b>Expected Text:</b><br><div style='text-align: left;'>" + formattedExpected + "</div></html>";
                ocrHtml = "<html><b>OCR Result:</b><br><div style='text-align: left;'>" + formattedOCR + "</div></html>";
            } else {
                // For non-Thai languages, work with the original text.
                formattedExpected = expectedText.replaceAll("\\r?\\n", "<br>");
                formattedOCR = ocrText.replaceAll("\\r?\\n", "<br>");
                if (data.getResult() != null &&
                        (data.getResult().equalsIgnoreCase("NOK") || data.getResult().equalsIgnoreCase("incorrect"))
                        && !expectedText.equals(ocrText)) {
                    String[] diffHTML = computeDiffHighlight(expectedText, ocrText);
                    formattedExpected = diffHTML[0].replaceAll("\\r?\\n", "<br>");
                    formattedOCR = diffHTML[1].replaceAll("\\r?\\n", "<br>");
                }

                if (selectedSheet != null) {
                    if (selectedSheet.equalsIgnoreCase("arab") ||
                            selectedSheet.equalsIgnoreCase("sa") ||
                            selectedSheet.equalsIgnoreCase("il")) {
                        Font rtlFont = selectedSheet.equalsIgnoreCase("il")
                                ? new Font("Arial", Font.PLAIN, 20)
                                : new Font("Noto Sans Arabic", Font.PLAIN, 20);
                        referenceLabel.setFont(rtlFont);
                        ocrLabel.setFont(rtlFont);
                        expectedHtml = "<html><b>Expected Text:</b><br><div style='direction: rtl; text-align: left;'>" + formattedExpected + "</div></html>";
                        ocrHtml = "<html><b>OCR Result:</b><br><div style='direction: rtl; text-align: left;'>" + formattedOCR + "</div></html>";
                        referenceLabel.setHorizontalAlignment(SwingConstants.LEFT);
                        ocrLabel.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        Font defaultFont = new Font("Aptos Narrow", Font.BOLD, 20);
                        referenceLabel.setFont(defaultFont);
                        ocrLabel.setFont(defaultFont);
                        expectedHtml = "<html><b>Expected Text:</b><br><div style='text-align: left;'>" + formattedExpected + "</div></html>";
                        ocrHtml = "<html><b>OCR Result:</b><br><div style='text-align: left;'>" + formattedOCR + "</div></html>";
                    }
                } else {
                    expectedHtml = "<html><b>Expected Text:</b><br>" + formattedExpected + "</html>";
                    ocrHtml = "<html><b>OCR Result:</b><br>" + formattedOCR + "</html>";
                }
            }

            // Load and display the image.
            ImageIcon warningImage = new ImageIcon(data.getImagePath());
            mainImageLabel.setIcon(warningImage);
            mainImageLabel.setPreferredSize(new Dimension(warningImage.getIconWidth(), warningImage.getIconHeight()));
            mainImageLabel.revalidate();
            mainImageLabel.repaint();

            // Update text labels.
            referenceLabel.setText(expectedHtml);
            ocrLabel.setText(ocrHtml);
        }
    }

    /**
     * Computes and highlights the differences between the expected and OCR texts.
     * It tokenizes by whitespace and uses a dynamic programming approach (LCS) to build the diff,
     * wrapping non-matching tokens in a highlighted span.
     *
     * @param expected the expected text (can be segmented already)
     * @param ocr the OCR text (can be segmented already)
     * @return a String array where index 0 is the highlighted expected text and index 1 is the highlighted OCR text.
     */
    private String[] computeDiffHighlight(String expected, String ocr) {
        // Determine tokenization method: split by whitespace if present; otherwise, split into individual characters.
        String[] expTokens;
        String[] ocrTokens;
        if (expected.contains(" ")) {
            expTokens = expected.split(" ");
        } else {
            expTokens = new String[expected.length()];
            for (int i = 0; i < expected.length(); i++) {
                expTokens[i] = String.valueOf(expected.charAt(i));
            }
        }
        if (ocr.contains(" ")) {
            ocrTokens = ocr.split(" ");
        } else {
            ocrTokens = new String[ocr.length()];
            for (int i = 0; i < ocr.length(); i++) {
                ocrTokens[i] = String.valueOf(ocr.charAt(i));
            }
        }

        int m = expTokens.length, n = ocrTokens.length;

        // Build the Longest Common Subsequence (LCS) matrix.
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    dp[i][j] = 0;
                } else if (expTokens[i - 1].equals(ocrTokens[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Reconstruct the LCS from the matrix.
        List<String> lcs = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (expTokens[i - 1].equals(ocrTokens[j - 1])) {
                lcs.add(0, expTokens[i - 1]);
                i--;
                j--;
            } else {
                if (dp[i - 1][j] >= dp[i][j - 1]) {
                    i--;
                } else {
                    j--;
                }
            }
        }

        // Build the highlighted expected text.
        StringBuilder highlightedExpected = new StringBuilder();
        int lcsIndex = 0;
        for (String token : expTokens) {
            if (lcsIndex < lcs.size() && token.equals(lcs.get(lcsIndex))) {
                highlightedExpected.append(token);
                lcsIndex++;
            } else {
                // Wrap non-matching tokens in a highlighted span.
                highlightedExpected.append("<span style='background-color: yellow;'>")
                        .append(token)
                        .append("</span>");
            }
            // Add a space only if the original texts were whitespace tokenized.
            highlightedExpected.append(expected.contains(" ") ? " " : "");
        }

        // Build the highlighted OCR text.
        StringBuilder highlightedOCR = new StringBuilder();
        lcsIndex = 0;
        for (String token : ocrTokens) {
            if (lcsIndex < lcs.size() && token.equals(lcs.get(lcsIndex))) {
                highlightedOCR.append(token);
                lcsIndex++;
            } else {
                highlightedOCR.append("<span style='background-color: yellow;'>")
                        .append(token)
                        .append("</span>");
            }
            highlightedOCR.append(ocr.contains(" ") ? " " : "");
        }

        return new String[]{highlightedExpected.toString().trim(), highlightedOCR.toString().trim()};
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

        // Place the image in a scroll pane to handle large images.
        JScrollPane scrollPane = new JScrollPane(mainImageLabel);
        scrollPane.setPreferredSize(new Dimension(700, 700));
        rightPanel.add(scrollPane, BorderLayout.NORTH);

        // Container for expected and OCR text labels.
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

    // Helper method for Thai segmentation using ICU4J's BreakIterator.
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
