package org.example;

import org.apache.poi.ss.usermodel.*;
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

public class ManualIconTestTool {

    private static JFrame frame;
    private static JPanel mainPanel;
    private static JList<String> warningList;
    private static JLabel mainImageLabel;
    private static JLabel iconLabel;
    private static JButton selectExcelButton, selectImageFolderButton, selectIconFolderButton, configureColumnsButton, okButton, nokButton, customResultButton;
    private static JTextField customResultTextField;
    private static String baseExcelPath = "", baseImagePath = "", baseIconPath = "";
    private static List<WarningData> warningRows;
    private static int currentRow;

    // Default columns
    private static String warningNameColumn = "Warning Name";
    private static String iconNameColumn = "Icon Name";
    private static String resultColumn = "Result";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ManualIconTestTool app = new ManualIconTestTool();
            app.createAndShowGUI();
        });
    }

    // Create and show GUI components
    private void createAndShowGUI() {
        frame = new JFrame("Automated Icon Testing App");
        mainPanel = new JPanel(new BorderLayout());

        // UI Components
        warningList = new JList<>();
        mainImageLabel = new JLabel();
        iconLabel = new JLabel();

        // Reference and OCR text labels with larger font
        Font largeFont = new Font("Arial", Font.BOLD, 20); // Bold, larger size

        selectExcelButton = new JButton("Select Excel File");
        selectImageFolderButton = new JButton("Select Image Folder");
        selectIconFolderButton = new JButton("Select Icons Folder");
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
                updateWarningList(baseExcelPath);
            }
        });

        selectImageFolderButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                baseImagePath = folderChooser.getSelectedFile().getAbsolutePath();
            }
        });

        selectIconFolderButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                baseIconPath = folderChooser.getSelectedFile().getAbsolutePath();
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

    private void updateWarningList(String filePath) {
        warningRows = new ArrayList<>(); // Initialize warningRows here if it's null

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Only one sheet
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Starting from row 2
                Row row = sheet.getRow(i);
                if (row == null) continue;

                WarningData warningData = new WarningData();
                warningData.setWarningName(getStringValueFromCell(row.getCell(getColumnIndex(warningNameColumn)))); // Column for warning name
                warningData.setIconName(getStringValueFromCell(row.getCell(getColumnIndex(iconNameColumn)))); // Column for icon name
                warningData.setResult(getStringValueFromCell(row.getCell(getColumnIndex(resultColumn)))); // Column for result

                // Only add valid rows
                if (warningData.getWarningName() != null && !warningData.getWarningName().isEmpty()) {
                    warningRows.add(warningData);
                }
            }
            updateWarningListDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWarningListDisplay() {
        if (warningRows == null || warningRows.isEmpty()) {
            System.out.println("Warning rows are empty or not initialized.");
            return;
        }

        DefaultListModel<String> listModel = new DefaultListModel<>();

        for (WarningData data : warningRows) {
            String displayText = data.getWarningName() + " - " + data.getResult();
            listModel.addElement(displayText);
        }

        warningList.setModel(listModel);

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

    private void displayRowDetails(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < warningRows.size()) {
            WarningData data = warningRows.get(rowIndex);

            // Load the corresponding warning image and display it
            ImageIcon warningImage = new ImageIcon(baseImagePath + File.separator + data.getWarningName() + ".png");
            mainImageLabel.setIcon(warningImage);
            mainImageLabel.setPreferredSize(new Dimension(warningImage.getIconWidth(), warningImage.getIconHeight()));
            mainImageLabel.revalidate();
            mainImageLabel.repaint();

            // Load the corresponding icon and display it
            ImageIcon iconImage = new ImageIcon(baseIconPath + File.separator + data.getIconName() + ".png");
            iconLabel.setIcon(iconImage);
            iconLabel.setPreferredSize(new Dimension(iconImage.getIconWidth(), iconImage.getIconHeight()));
            iconLabel.revalidate();
            iconLabel.repaint();
        }
    }

    private void saveResult(String result) {
        if (currentRow >= 0 && currentRow < warningRows.size()) {
            WarningData data = warningRows.get(currentRow);
            data.setResult(result);

            try (FileInputStream fis = new FileInputStream(new File(baseExcelPath));
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);
                Row row = sheet.getRow(currentRow + 1);
                row.getCell(getColumnIndex(resultColumn)).setCellValue(result);

                try (FileOutputStream fos = new FileOutputStream(new File(baseExcelPath))) {
                    workbook.write(fos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateWarningListDisplay();
        }
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(warningList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(selectExcelButton);
        buttonPanel.add(selectImageFolderButton);
        buttonPanel.add(selectIconFolderButton);
        buttonPanel.add(configureColumnsButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Add the image in a scroll pane to handle large images
        JScrollPane scrollPane = new JScrollPane(mainImageLabel);
        scrollPane.setPreferredSize(new Dimension(700, 350));
        rightPanel.add(scrollPane, BorderLayout.NORTH);

        // Add the icon in a scroll pane
        JScrollPane iconScrollPane = new JScrollPane(iconLabel);
        iconScrollPane.setPreferredSize(new Dimension(700, 350));
        rightPanel.add(iconScrollPane, BorderLayout.CENTER);

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
            case "Icon Name":
                return 1;
            case "Result":
                return 2;
            default:
                return -1;
        }
    }

    // Column customization window
    private void openColumnCustomizationWindow() {
        String[] columnNames = {"Warning Name", "Icon Name", "Result"};
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
        private String iconName;
        private String result;

        public String getWarningName() {
            return warningName;
        }

        public void setWarningName(String warningName) {
            this.warningName = warningName;
        }

        public String getIconName() {
            return iconName;
        }

        public void setIconName(String iconName) {
            this.iconName = iconName;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
