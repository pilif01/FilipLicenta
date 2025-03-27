package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageExplorerApp {

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
            ImageExplorerApp app = new ImageExplorerApp();
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

        // Set the font for the labels (larger size and bold)
        Font largeFont = new Font("Arial", Font.BOLD, 20); // Bold, larger size
        referenceLabel.setFont(largeFont);
        ocrLabel.setFont(largeFont);

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
        warningRows = new ArrayList<>(); // Initialize warningRows here if it's null

        // Log the base image path to ensure it's correct
        System.out.println("Base Image Path: " + baseImagePath); // Log to console to verify

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheet(sheetName);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Starting from row 2
                Row row = sheet.getRow(i);
                if (row == null) continue;

                WarningData warningData = new WarningData();
                warningData.setWarningName(getStringValueFromCell(row.getCell(getColumnIndex(warningNameColumn)))); // Column for warning name
                warningData.setExpectedText(getStringValueFromCell(row.getCell(getColumnIndex(expectedTextColumn)))); // Column for expected text
                warningData.setOcrText(getStringValueFromCell(row.getCell(getColumnIndex(ocrTextColumn)))); // Column for OCR text
                warningData.setResult(getStringValueFromCell(row.getCell(getColumnIndex(resultColumn)))); // Column for result

                // Only add valid rows
                if (warningData.getWarningName() != null && !warningData.getWarningName().isEmpty()) {
                    // Use sheet name as the language identifier
                    String language = sheetName.toLowerCase(); // Convert the sheet name to lowercase to match image file naming convention
                    String imagePath = baseImagePath + File.separator + warningData.getWarningName() + "_" + language + ".png";
                    System.out.println("Image Path: " + imagePath); // Log the image path for verification

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
        // Check if warningRows is not null and contains data
        if (warningRows == null || warningRows.isEmpty()) {
            System.out.println("Warning rows are empty or not initialized.");
            return;
        }

        DefaultListModel<String> listModel = new DefaultListModel<>();

        // Iterate over the warning rows and add them to the list model
        for (WarningData data : warningRows) {
            String displayText = data.getWarningName() + " - " + data.getResult();

            // Check the result and update the color accordingly
            if ("correct".equalsIgnoreCase(data.getResult())) {
                displayText = "<html><font color='green'>" + displayText + "</font></html>";
            } else if ("incorrect".equalsIgnoreCase(data.getResult())) {
                displayText = "<html><font color='red'>" + displayText + "</font></html>";
            }

            // Add the formatted display text to the list model
            listModel.addElement(displayText);
        }

        // Set the list model to the warning list
        warningList.setModel(listModel);

        // Log the update process
        System.out.println("Updated warning list with " + warningRows.size() + " items.");

        // Add a listener to handle clicking on a warning name
        warningList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = warningList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    currentRow = selectedIndex;  // Update current row index
                    displayRowDetails(currentRow); // Ensure displayRowDetails is called when a warning is selected
                }
            }
        });
    }


    private void displayRowDetails(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < warningRows.size()) {
            WarningData data = warningRows.get(rowIndex);

            // Load the corresponding image and display it in original size
            ImageIcon warningImage = new ImageIcon(data.getImagePath());

            // Set the icon to the mainImageLabel
            mainImageLabel.setIcon(warningImage);

            // Resize the label to fit the image size (to avoid compression)
            mainImageLabel.setPreferredSize(new Dimension(warningImage.getIconWidth(), warningImage.getIconHeight()));

            // Make sure the label's size is adjusted to the image size
            mainImageLabel.revalidate();
            mainImageLabel.repaint();

            // Set the reference (Expected) text and OCR text labels
            referenceLabel.setText("<html><b>Expected Text:</b><br>" + data.getExpectedText() + "</html>");
            ocrLabel.setText("<html><b>OCR Result:</b><br>" + data.getOcrText() + "</html>");
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

        // Add the image in a scroll pane to handle large images
        JScrollPane scrollPane = new JScrollPane(mainImageLabel);
        scrollPane.setPreferredSize(new Dimension(700, 700)); // Adjust as needed
        rightPanel.add(scrollPane, BorderLayout.NORTH);

        // Container to hold both the labels for Reference Text and OCR Text
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS)); // Stack the labels vertically
        textPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the text panel horizontally

        // Add the reference and OCR text labels to the panel
        textPanel.add(referenceLabel);
        textPanel.add(Box.createVerticalStrut(10)); // Adds some space between the texts
        textPanel.add(ocrLabel);

        // Center the text panel vertically inside the rightPanel
        rightPanel.add(textPanel, BorderLayout.CENTER);

        // Panel with result buttons and custom result input
        JPanel resultPanel = new JPanel(new FlowLayout());
        resultPanel.add(okButton);
        resultPanel.add(nokButton);
        resultPanel.add(customResultTextField);
        resultPanel.add(customResultButton);
        rightPanel.add(resultPanel, BorderLayout.SOUTH);

        return rightPanel;
    }



    private String getStringValueFromCell(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
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

    // Class to store warning data
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