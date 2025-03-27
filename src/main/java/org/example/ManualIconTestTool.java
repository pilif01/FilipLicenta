package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ManualIconTestTool {
    private static JFrame frame;
    private static JPanel mainPanel;
    private static JComboBox<String> sheetSelector;
    private static JList<String> warningList;
    private static JLabel iconImageLabel; // Changed to JLabel for icon display
    private static JTextArea warningDetailsArea;
    private static JButton selectExcelButton, selectIconFolderButton;
    private static String baseExcelPath = "", baseIconFolderPath = "";
    private static List<WarningData> warningRows;
    private static int currentRow;

    private static String warningNameColumn = "Warning Name";
    private static String iconNameColumn = "Icon Name";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Manual Icon Test Tool");
        mainPanel = new JPanel(new BorderLayout());

        // UI Components
        sheetSelector = new JComboBox<>();
        sheetSelector.addItem("Please select an Excel file");

        warningList = new JList<>();
        iconImageLabel = new JLabel(); // Changed from ImageLabel to JLabel
        warningDetailsArea = new JTextArea(5, 20);
        selectExcelButton = new JButton("Select Excel File");
        selectIconFolderButton = new JButton("Select Icon Folder");

        // Action listener for Excel file selection
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

        // Action listener for icon folder selection
        selectIconFolderButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                baseIconFolderPath = folderChooser.getSelectedFile().getAbsolutePath();
                updateWarningList();
            }
        });

        // Add components to main panel
        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        mainPanel.add(createRightPanel(), BorderLayout.CENTER);

        // Set frame properties
        frame.add(mainPanel);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static void updateSheetSelector(String filePath) {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            sheetSelector.removeAllItems();
            sheetSelector.addItem("Select a sheet");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetSelector.addItem(workbook.getSheetName(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDataFromXLSX(String filePath, String sheetName) {
        warningRows = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheet(sheetName);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Starting from row 2
                Row row = sheet.getRow(i);
                if (row == null) continue;

                WarningData warningData = new WarningData();
                warningData.setWarningName(getStringValueFromCell(row.getCell(getColumnIndex(warningNameColumn))));
                warningData.setIconName(getStringValueFromCell(row.getCell(getColumnIndex(iconNameColumn))));

                // Only add valid rows
                if (warningData.getWarningName() != null && !warningData.getWarningName().isEmpty()) {
                    String iconPath = baseIconFolderPath + File.separator + warningData.getIconName() + ".png";
                    warningData.setIconPath(iconPath);
                    warningRows.add(warningData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateWarningList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (WarningData data : warningRows) {
            String displayText = data.getWarningName();
            listModel.addElement(displayText);
        }
        warningList.setModel(listModel);
    }

    private static void displayRowDetails(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < warningRows.size()) {
            WarningData data = warningRows.get(rowIndex);
            ImageIcon icon = new ImageIcon(data.getIconPath());
            iconImageLabel.setIcon(icon);
            iconImageLabel.setSize(icon.getIconWidth(), icon.getIconHeight());
            warningDetailsArea.setText("Icon Name: " + data.getIconName());
        }
    }

    private static JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(sheetSelector, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(warningList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(selectExcelButton);
        buttonPanel.add(selectIconFolderButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private static JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(iconImageLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(warningDetailsArea), BorderLayout.CENTER);

        return rightPanel;
    }

    private static String getStringValueFromCell(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        return "";
    }

    private static int getColumnIndex(String columnName) {
        switch (columnName) {
            case "Warning Name":
                return 0;
            case "Icon Name":
                return 1;
            default:
                return -1;
        }
    }

    class WarningData {
        private String warningName;
        private String iconName;
        private String iconPath;

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

        public String getIconPath() {
            return iconPath;
        }

        public void setIconPath(String iconPath) {
            this.iconPath = iconPath;
        }
    }
}
