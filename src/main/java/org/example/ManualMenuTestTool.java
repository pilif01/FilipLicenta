package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ibm.icu.text.BreakIterator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManualMenuTestTool {

    private static JFrame frame;
    private static JPanel mainPanel;
    private static JComboBox<String> sheetSelector;
    private static JList<MenuData> menuList; // Changed to JList<MenuData>
    private static ImagePanel mainImagePanel; // Changed to custom ImagePanel for drawing
    private static JLabel referenceLabel, ocrLabel;
    private static JButton selectExcelButton, selectImageFolderButton, configureColumnsButton, okButton, nokButton, customResultButton;
    private static JTextField customResultTextField;
    private static String baseExcelPath = "", baseImagePath = "";
    private static List<MenuData> menuRows; // Changed to List<MenuData>
    private static int currentRow;

    // Configurable column indices with default values based on common Excel structure
    // Column A (index 0) is reserved for TControl flag, but will be ignored for processing.
    private static int photoNameColIdx = 0;       // Default: Column C
    private static int xLeftColIdx = 1;           // Default: Column D
    private static int yTopColIdx = 2;            // Default: Column E
    private static int xRightColIdx = 3;          // Default: Column F
    private static int yBottomColIdx = 4;         // Default: Column G
    private static int referenceTextColIdx = 5;   // Default: Column H
    private static int ocrTextColIdx = 6;         // Default: Column I
    private static int resultColIdx = 7;          // Default: Column J


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ManualMenuTestTool app = new ManualMenuTestTool();
            app.createAndShowGUI();
        });
    }

    // Create and show the GUI components
    private void createAndShowGUI() {
        frame = new JFrame("Manual Menu Testing App");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose only this window
        mainPanel = new JPanel(new BorderLayout());

        // UI Components
        sheetSelector = new JComboBox<>();
        sheetSelector.addItem("Please select an Excel file");
        sheetSelector.setEnabled(false); // Disabled until an Excel is loaded

        // JList for menu data
        menuList = new JList<>();
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setFont(new Font("Arial", Font.PLAIN, 16));
        // Custom renderer for JList to show status
        menuList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MenuData) {
                    MenuData data = (MenuData) value;
                    String displayText = data.getPhotoName() + " - " + data.getResult();
                    if ("correct".equalsIgnoreCase(data.getResult())) {
                        label.setForeground(new Color(0, 128, 0)); // Dark Green
                    } else if ("incorrect".equalsIgnoreCase(data.getResult()) || "nok".equalsIgnoreCase(data.getResult())) {
                        label.setForeground(new Color(200, 0, 0)); // Dark Red
                    } else if (data.getResult() != null && !data.getResult().isEmpty()) {
                        label.setForeground(Color.BLUE); // Custom result
                    } else {
                        label.setForeground(Color.BLACK); // Default
                    }
                    label.setText(displayText);
                }
                return label;
            }
        });

        mainImagePanel = new ImagePanel(); // Instantiate custom ImagePanel
        mainImagePanel.setPreferredSize(new Dimension(800, 600)); // Default size, will adjust with image

        // Labels for expected & OCR text
        referenceLabel = new JLabel("Reference Text: ");
        ocrLabel = new JLabel("OCR Text: ");

        // Set default font (used for non-Thai/non-Arab sheets)
        Font defaultFont = new Font("Aptos Narrow", Font.BOLD, 20);
        referenceLabel.setFont(defaultFont);
        ocrLabel.setFont(defaultFont);

        selectExcelButton = new JButton("Select Excel File");
        selectImageFolderButton = new JButton("Select Image Folder");
        configureColumnsButton = new JButton("Configure Columns"); // Re-enabled

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
                // If Excel data is already loaded, re-load to attempt image display
                if (sheetSelector.getSelectedItem() != null && !sheetSelector.getSelectedItem().toString().startsWith("Please")) {
                    loadDataFromXLSX(baseExcelPath, (String) sheetSelector.getSelectedItem());
                }
            }
        });

        configureColumnsButton.addActionListener(e -> openColumnCustomizationWindow()); // Action listener for configure columns
        okButton.addActionListener(e -> saveResult("CORRECT")); // Changed to CORRECT for consistency
        nokButton.addActionListener(e -> saveResult("INCORRECT")); // Changed to INCORRECT for consistency
        customResultButton.addActionListener(e -> saveResult(customResultTextField.getText().trim()));

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
        // Add key binding for 'n' key to save "INCORRECT"
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('n'), "saveIncorrect");
        frame.getRootPane().getActionMap().put("saveIncorrect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveResult("INCORRECT");
            }
        });

        // Finalize frame
        frame.add(mainPanel);
        frame.setSize(1200, 800); // Larger size for better layout
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateSheetSelector(String filePath) {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            sheetSelector.removeAllItems();
            sheetSelector.addItem("Select a sheet");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String sheetName = workbook.getSheetName(i);
                // Exclude the "TControl" sheet from the selector as it's not a test data sheet
                if (!sheetName.equalsIgnoreCase("TControl")) {
                    sheetSelector.addItem(sheetName);
                }
            }
            sheetSelector.setEnabled(true);
            // Remove previous action listeners to prevent duplicates
            for (ActionListener al : sheetSelector.getActionListeners()) {
                sheetSelector.removeActionListener(al);
            }
            sheetSelector.addActionListener(e -> {
                String selectedSheet = (String) sheetSelector.getSelectedItem();
                if (selectedSheet != null && !selectedSheet.equals("Select a sheet")) {
                    loadDataFromXLSX(filePath, selectedSheet);
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error loading Excel sheets: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadDataFromXLSX(String filePath, String sheetName) {
        menuRows = new ArrayList<>(); // Initialize menuRows
        DefaultListModel<MenuData> listModel = new DefaultListModel<>(); // New list model for MenuData

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                JOptionPane.showMessageDialog(frame, "Sheet '" + sheetName + "' not found in the Excel file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DataFormatter formatter = new DataFormatter();

            // Loop from row 1 (index 1) as row 0 is header.
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Removed the TControl flag check as per user's request
                // String tControlFlag = getStringValueFromCell(row.getCell(0));
                // if (!"RUN".equalsIgnoreCase(tControlFlag)) {
                //     System.out.println("Skipping row " + (i + 1) + " in sheet " + sheetName + " due to TControl flag: " + tControlFlag);
                //     continue;
                // }

                // Get cells directly using the configured indices
                Cell photoNameCell = row.getCell(photoNameColIdx);
                Cell xLeftCell = row.getCell(xLeftColIdx);
                Cell yTopCell = row.getCell(yTopColIdx);
                Cell xRightCell = row.getCell(xRightColIdx);
                Cell yBottomCell = row.getCell(yBottomColIdx);
                Cell referenceTextCell = row.getCell(referenceTextColIdx);
                Cell ocrTextCell = row.getCell(ocrTextColIdx);
                Cell resultCell = row.getCell(resultColIdx);

                // Validate essential cells - photoName and coordinates are crucial for display
                if (photoNameCell == null || getStringValueFromCell(photoNameCell).trim().isEmpty() ||
                        xLeftCell == null || yTopCell == null || xRightCell == null || yBottomCell == null) {
                    System.out.println("Skipping row " + (i + 1) + " in sheet " + sheetName + " due to missing or empty essential data (Photo Name or Coordinates) in configured columns.");
                    continue;
                }

                String photoName = getStringValueFromCell(photoNameCell);
                int x1 = getNumericCellValue(xLeftCell);
                int y1 = getNumericCellValue(yTopCell);
                int x2 = getNumericCellValue(xRightCell);
                int y2 = getNumericCellValue(yBottomCell);
                String referenceText = getStringValueFromCell(referenceTextCell); // Can be empty
                String ocrText = getStringValueFromCell(ocrTextCell);         // Can be empty
                String result = getStringValueFromCell(resultCell);           // Can be empty

                String language = sheetName.toLowerCase();
                String imagePath = baseImagePath + File.separator + photoName + "_" + language + ".png";

                MenuData menuData = new MenuData(photoName, x1, y1, x2, y2, referenceText, ocrText, result, imagePath, i); // Pass actual row index
                menuRows.add(menuData);
                listModel.addElement(menuData);
            }
            menuList.setModel(listModel);

            if (!menuRows.isEmpty()) {
                menuList.setSelectedIndex(0); // Select the first item
            } else {
                displayRowDetails(-1); // Clear display if no data
            }

            System.out.println("Loaded " + menuRows.size() + " menu items for sheet: " + sheetName);
            updateMenuList(); // Initial update of the list display
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error reading Excel file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "An unexpected error occurred while loading Excel data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void openColumnCustomizationWindow() {
        // Create text fields to display/edit current column letters
        JTextField photoNameField = new JTextField(columnIndexToLetter(photoNameColIdx));
        JTextField xLeftField = new JTextField(columnIndexToLetter(xLeftColIdx));
        JTextField yTopField = new JTextField(columnIndexToLetter(yTopColIdx));
        JTextField xRightField = new JTextField(columnIndexToLetter(xRightColIdx));
        JTextField yBottomField = new JTextField(columnIndexToLetter(yBottomColIdx));
        JTextField referenceTextField = new JTextField(columnIndexToLetter(referenceTextColIdx));
        JTextField ocrTextField = new JTextField(columnIndexToLetter(ocrTextColIdx));
        JTextField resultField = new JTextField(columnIndexToLetter(resultColIdx));

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5)); // 0 rows, 2 columns, with gaps
        panel.add(new JLabel("Photo Name Column (e.g., C):"));
        panel.add(photoNameField);
        panel.add(new JLabel("XLeft Column (e.g., D):"));
        panel.add(xLeftField);
        panel.add(new JLabel("YTop Column (e.g., E):"));
        panel.add(yTopField);
        panel.add(new JLabel("XRight Column (e.g., F):"));
        panel.add(xRightField);
        panel.add(new JLabel("YBottom Column (e.g., G):"));
        panel.add(yBottomField);
        panel.add(new JLabel("Reference Text Column (e.g., H):"));
        panel.add(referenceTextField);
        panel.add(new JLabel("OCR Text Column (e.g., I):"));
        panel.add(ocrTextField);
        panel.add(new JLabel("Result Column (e.g., J):"));
        panel.add(resultField);

        int option = JOptionPane.showConfirmDialog(frame, panel, "Configure Excel Columns by Letter", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            try {
                int newPhotoNameIdx = columnLetterToIndex(photoNameField.getText());
                int newXLeftIdx = columnLetterToIndex(xLeftField.getText());
                int newYTopIdx = columnLetterToIndex(yTopField.getText());
                int newXRightIdx = columnLetterToIndex(xRightField.getText());
                int newYBottomIdx = columnLetterToIndex(yBottomField.getText());
                int newReferenceTextIdx = columnLetterToIndex(referenceTextField.getText());
                int newOcrTextIdx = columnLetterToIndex(ocrTextField.getText());
                int newResultIdx = columnLetterToIndex(resultField.getText());

                // Basic validation: ensure indices are non-negative
                if (newPhotoNameIdx < 0 || newXLeftIdx < 0 || newYTopIdx < 0 || newXRightIdx < 0 || newYBottomIdx < 0 ||
                        newReferenceTextIdx < 0 || newOcrTextIdx < 0 || newResultIdx < 0) {
                    throw new IllegalArgumentException("Invalid column letter entered. Please use A-Z or AA-ZZ format.");
                }

                photoNameColIdx = newPhotoNameIdx;
                xLeftColIdx = newXLeftIdx;
                yTopColIdx = newYTopIdx;
                xRightColIdx = newXRightIdx;
                yBottomColIdx = newYBottomIdx;
                referenceTextColIdx = newReferenceTextIdx;
                ocrTextColIdx = newOcrTextIdx;
                resultColIdx = newResultIdx;

                JOptionPane.showMessageDialog(frame, "Column configuration updated. Please re-select Excel file to apply changes.", "Configuration Saved", JOptionPane.INFORMATION_MESSAGE);

                // Re-load data if an Excel file is already selected
                if (baseExcelPath != null && !baseExcelPath.isEmpty() && sheetSelector.getSelectedItem() != null && !sheetSelector.getSelectedItem().toString().startsWith("Please")) {
                    loadDataFromXLSX(baseExcelPath, (String) sheetSelector.getSelectedItem());
                }

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, "Error in column configuration: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "An unexpected error occurred during column configuration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private static int columnLetterToIndex(String letter) {
        if (letter == null || letter.isEmpty()) {
            return -1; // Invalid input
        }
        letter = letter.toUpperCase();
        int index = 0;
        for (int i = 0; i < letter.length(); i++) {
            index = index * 26 + (letter.charAt(i) - 'A' + 1);
        }
        return index - 1; // Convert 1-based index (A=1) to 0-based
    }

    private static String columnIndexToLetter(int index) {
        if (index < 0) {
            return "";
        }
        StringBuilder letter = new StringBuilder();
        int current = index + 1; // Convert to 1-based index
        while (current > 0) {
            int remainder = (current - 1) % 26;
            letter.insert(0, (char) ('A' + remainder));
            current = (current - 1) / 26;
        }
        return letter.toString();
    }


    private void updateMenuList() {
        if (menuRows == null || menuRows.isEmpty()) {
            System.out.println("Menu rows are empty or not initialized.");
            menuList.setModel(new DefaultListModel<>()); // Clear the list
            displayRowDetails(-1); // Clear current display
            return;
        }

        DefaultListModel<MenuData> listModel = new DefaultListModel<>();
        for (MenuData data : menuRows) {
            listModel.addElement(data); // Add MenuData objects directly, renderer handles display
        }
        menuList.setModel(listModel);

        System.out.println("Updated menu list with " + menuRows.size() + " items.");

        // Ensure listener is only added once
        if (menuList.getListSelectionListeners().length == 0) {
            menuList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = menuList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        currentRow = selectedIndex;
                        displayRowDetails(currentRow);
                    }
                }
            });
        }
        // Re-select current row to trigger display update in case of re-load
        if (currentRow >= 0 && currentRow < menuRows.size()) {
            menuList.setSelectedIndex(currentRow);
        } else if (!menuRows.isEmpty()) {
            menuList.setSelectedIndex(0);
        }
    }

    /**
     * Displays details for the selected row, including image and highlighted text.
     * For Thai, the text is segmented using ICU4J before diff highlighting is applied.
     * New line characters are replaced with HTML <br> tags.
     */
    private void displayRowDetails(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < menuRows.size()) {
            MenuData data = menuRows.get(rowIndex);
            String expectedText = data.getReferenceText();
            String ocrText = data.getOcrText();

            String selectedSheet = (String) sheetSelector.getSelectedItem();
            if (selectedSheet == null || selectedSheet.equals("Select a sheet")) {
                // This state should ideally not be reached if data is loaded correctly
                System.err.println("No sheet selected or invalid sheet.");
                return;
            }

            String formattedExpected;
            String formattedOCR;
            String expectedHtml;
            String ocrHtml;

            // Load and display the image.
            File imageFile = new File(data.getImagePath());
            if (imageFile.exists()) {
                try {
                    BufferedImage image = javax.imageio.ImageIO.read(imageFile);
                    mainImagePanel.setImage(image);
                    mainImagePanel.setCoordinates(data.getX1(), data.getY1(), data.getX2(), data.getY2());
                    mainImagePanel.revalidate();
                    mainImagePanel.repaint();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error loading image: " + ex.getMessage(), "Image Error", JOptionPane.ERROR_MESSAGE);
                    mainImagePanel.setImage(null);
                    mainImagePanel.setCoordinates(0,0,0,0);
                    mainImagePanel.repaint();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Image not found: " + imageFile.getName(), "Image Not Found", JOptionPane.WARNING_MESSAGE);
                mainImagePanel.setImage(null);
                mainImagePanel.setCoordinates(0,0,0,0);
                mainImagePanel.repaint();
            }


            // For Thai, we first segment the text before applying diff highlighting
            if (selectedSheet.equalsIgnoreCase("th")) {
                // Segment Thai text
                String segExpected = segmentThaiText(expectedText);
                String segOCR = segmentThaiText(ocrText);

                // Apply diff highlighting if the result is marked wrong and texts differ
                if (data.getResult() != null &&
                        (data.getResult().equalsIgnoreCase("NOK") || data.getResult().equalsIgnoreCase("incorrect"))
                        && !expectedText.equals(ocrText)) { // Compare original texts for overall diff
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

                expectedHtml = "<html><b>Reference Text:</b><br><div style='text-align: left;'>" + formattedExpected + "</div></html>";
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

                if (selectedSheet.equalsIgnoreCase("sa") || selectedSheet.equalsIgnoreCase("il")) {
                    // Arabic/Hebrew fonts (Right-to-Left languages)
                    Font rtlFont = selectedSheet.equalsIgnoreCase("il")
                            ? new Font("Arial", Font.PLAIN, 20)
                            : new Font("Noto Sans Arabic", Font.PLAIN, 20);
                    referenceLabel.setFont(rtlFont);
                    ocrLabel.setFont(rtlFont);
                    expectedHtml = "<html><b>Reference Text:</b><br><div style='direction: rtl; text-align: left;'>" + formattedExpected + "</div></html>";
                    ocrHtml = "<html><b>OCR Result:</b><br><div style='direction: rtl; text-align: left;'>" + formattedOCR + "</div></html>";
                    referenceLabel.setHorizontalAlignment(SwingConstants.LEFT);
                    ocrLabel.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    // Reset to default font for other languages
                    Font defaultFont = new Font("Aptos Narrow", Font.BOLD, 20);
                    referenceLabel.setFont(defaultFont);
                    ocrLabel.setFont(defaultFont);
                    expectedHtml = "<html><b>Reference Text:</b><br><div style='text-align: left;'>" + formattedExpected + "</div></html>";
                    ocrHtml = "<html><b>OCR Result:</b><br><div style='text-align: left;'>" + formattedOCR + "</div></html>";
                    referenceLabel.setHorizontalAlignment(SwingConstants.LEFT);
                    ocrLabel.setHorizontalAlignment(SwingConstants.LEFT);
                }
            }

            // Update text labels.
            referenceLabel.setText(expectedHtml);
            ocrLabel.setText(ocrHtml);
        } else {
            // Clear display if no valid row is selected or available
            mainImagePanel.setImage(null);
            mainImagePanel.setCoordinates(0,0,0,0);
            mainImagePanel.repaint();
            referenceLabel.setText("Reference Text: ");
            ocrLabel.setText("OCR Text: ");
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
        // Use a regex to split by one or more whitespace characters, preserving empty strings if needed.
        String[] expTokens = expected.split("\\s+");
        String[] ocrTokens = ocr.split("\\s+");

        // Handle cases where split might result in a single empty string for empty input
        if (expected.isEmpty()) expTokens = new String[]{};
        if (ocr.isEmpty()) ocrTokens = new String[]{};

        // If no whitespace, tokenize by character
        if (expTokens.length == 0 || (expTokens.length == 1 && expTokens[0].isEmpty() && !expected.isEmpty())) {
            expTokens = new String[expected.length()];
            for (int i = 0; i < expected.length(); i++) {
                expTokens[i] = String.valueOf(expected.charAt(i));
            }
        }
        if (ocrTokens.length == 0 || (ocrTokens.length == 1 && ocrTokens[0].isEmpty() && !ocr.isEmpty())) {
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
                } else if (expTokens[i - 1].equalsIgnoreCase(ocrTokens[j - 1])) { // Case-insensitive comparison
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
            if (expTokens[i - 1].equalsIgnoreCase(ocrTokens[j - 1])) { // Case-insensitive comparison
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
            if (lcsIndex < lcs.size() && token.equalsIgnoreCase(lcs.get(lcsIndex))) { // Case-insensitive comparison
                highlightedExpected.append(token);
                lcsIndex++;
            } else {
                // Wrap non-matching tokens in a highlighted span.
                highlightedExpected.append("<span style='background-color: yellow;'>")
                        .append(token)
                        .append("</span>");
            }
            highlightedExpected.append(" "); // Always add space between tokens for display
        }

        // Build the highlighted OCR text.
        StringBuilder highlightedOCR = new StringBuilder();
        lcsIndex = 0;
        for (String token : ocrTokens) {
            if (lcsIndex < lcs.size() && token.equalsIgnoreCase(lcs.get(lcsIndex))) { // Case-insensitive comparison
                highlightedOCR.append(token);
                lcsIndex++;
            } else {
                highlightedOCR.append("<span style='background-color: yellow;'>")
                        .append(token)
                        .append("</span>");
            }
            highlightedOCR.append(" "); // Always add space between tokens for display
        }

        return new String[]{highlightedExpected.toString().trim(), highlightedOCR.toString().trim()};
    }

    private void saveResult(String result) {
        if (currentRow >= 0 && currentRow < menuRows.size()) {
            MenuData data = menuRows.get(currentRow);
            data.setResult(result); // Update in-memory data

            try (FileInputStream fis = new FileInputStream(new File(baseExcelPath));
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                XSSFSheet sheet = workbook.getSheet(sheetSelector.getSelectedItem().toString());
                if (sheet == null) {
                    JOptionPane.showMessageDialog(frame, "Error: Sheet '" + sheetSelector.getSelectedItem().toString() + "' not found in Excel.", "Save Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Row row = sheet.getRow(data.getExcelRowIndex()); // Use the actual Excel row index
                if (row == null) {
                    row = sheet.createRow(data.getExcelRowIndex()); // Create if it doesn't exist (shouldn't happen for existing data)
                }

                // Directly use the static resultColIdx
                Cell resultCell = row.getCell(resultColIdx);
                if (resultCell == null) {
                    resultCell = row.createCell(resultColIdx);
                }
                resultCell.setCellValue(result);

                try (FileOutputStream fos = new FileOutputStream(new File(baseExcelPath))) {
                    workbook.write(fos);
                }
                JOptionPane.showMessageDialog(frame, "Result saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error saving result to Excel: " + e.getMessage() + "\nEnsure the Excel file is not open in another program.", "File Access Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "An unexpected error occurred while saving result: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            updateMenuList(); // Refresh the list to show updated status
            displayRowDetails(currentRow); // Re-display current row to update highlighting if needed
        } else {
            JOptionPane.showMessageDialog(frame, "No item selected to save result for.", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(250, frame.getHeight())); // Fixed width for left panel

        JPanel topButtons = new JPanel(new GridLayout(3, 1, 5, 5)); // Increased grid rows for configure button
        topButtons.add(selectExcelButton);
        topButtons.add(selectImageFolderButton);
        topButtons.add(configureColumnsButton); // Add configure columns button
        leftPanel.add(topButtons, BorderLayout.NORTH);

        leftPanel.add(new JScrollPane(menuList), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(sheetSelector, BorderLayout.NORTH); // Sheet selector at the bottom of left panel

        leftPanel.add(bottomPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10)); // Add spacing

        // Place the image in a scroll pane to handle large images.
        JScrollPane imageScrollPane = new JScrollPane(mainImagePanel);
        imageScrollPane.setPreferredSize(new Dimension(800, 600)); // Set preferred size for the scroll pane
        rightPanel.add(imageScrollPane, BorderLayout.CENTER); // Image panel in the center

        // Container for expected and OCR text labels.
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // Align content to left
        textPanel.add(referenceLabel);
        textPanel.add(Box.createVerticalStrut(10)); // Spacer
        textPanel.add(ocrLabel);
        rightPanel.add(textPanel, BorderLayout.NORTH); // Text labels at the top of right panel

        // Panel with result buttons and custom result input.
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
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
            return String.valueOf((int) cell.getNumericCellValue()); // For integers
        if (cell.getCellType() == CellType.BOOLEAN)
            return String.valueOf(cell.getBooleanCellValue());
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return new DataFormatter().formatCellValue(cell); // Evaluate formula and get formatted string
            } catch (Exception e) {
                System.err.println("Error evaluating formula cell: " + e.getMessage());
                return "";
            }
        }
        return "";
    }

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
                    System.err.println("Warning: Non-numeric string in coordinate cell: '" + cell.getStringCellValue() + "'. Using 0.");
                    return 0;
                }
            default:
                return 0;
        }
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

    // Inner class to store menu data.
    class MenuData {
        private String photoName;
        private int x1, y1, x2, y2;
        private String referenceText;
        private String ocrText;
        private String result;
        private String imagePath;
        private int excelRowIndex; // To save back to the correct row

        public MenuData(String photoName, int x1, int y1, int x2, int y2, String referenceText, String ocrText, String result, String imagePath, int excelRowIndex) {
            this.photoName = photoName;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.referenceText = referenceText;
            this.ocrText = ocrText;
            this.result = result;
            this.imagePath = imagePath;
            this.excelRowIndex = excelRowIndex;
        }

        public String getPhotoName() { return photoName; }
        public int getX1() { return x1; }
        public int getY1() { return y1; }
        public int getX2() { return x2; }
        public int getY2() { return y2; }
        public String getReferenceText() { return referenceText; }
        public String getOcrText() { return ocrText; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getImagePath() { return imagePath; }
        public int getExcelRowIndex() { return excelRowIndex; }

        @Override
        public String toString() {
            // This is what the JList will display by default before the custom renderer
            return photoName;
        }
    }

    // Custom JPanel to display image and draw coordinates
    class ImagePanel extends JPanel {
        private BufferedImage image;
        private Rectangle coordinatesRect; // Rectangle to draw

        public ImagePanel() {
            // Ensure the panel can be scrolled if the image is larger than the viewport
            setLayout(new BorderLayout()); // Use a layout manager
            setBackground(Color.LIGHT_GRAY); // Background when no image
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            // Set preferred size to image size to allow JScrollPane to work correctly
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            } else {
                setPreferredSize(new Dimension(800, 600)); // Default size if no image
            }
            revalidate(); // Recalculate layout
            repaint(); // Redraw
        }

        public void setCoordinates(int x1, int y1, int x2, int y2) {
            // Only set if coordinates are valid (non-zero area)
            if (x1 < x2 && y1 < y2) {
                this.coordinatesRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
            } else {
                this.coordinatesRect = null; // No valid rectangle to draw
            }
            repaint(); // Redraw
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                // Draw image at (0,0)
                g.drawImage(image, 0, 0, this);
            }
            if (coordinatesRect != null) {
                Graphics2D g2d = (Graphics2D) g.create(); // Create a copy of Graphics context
                g2d.setColor(Color.RED); // Draw the rectangle in red
                g2d.setStroke(new BasicStroke(3)); // Thicker line
                g2d.drawRect(coordinatesRect.x, coordinatesRect.y, coordinatesRect.width, coordinatesRect.height);
                g2d.dispose(); // Release resources
            }
        }
    }
}