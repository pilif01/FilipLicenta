package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
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

    // Default column indices (zero-based)
    private static int warningNameColumnIndex = 0;
    private static int iconNameColumnIndex = 1;
    private static int resultColumnIndex = 2;

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

        // Set font for any labels if needed
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

        // Add key binding for 'c' key to save "CORRECT" only if a warning is selected
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('c'), "saveCorrect");
        frame.getRootPane().getActionMap().put("saveCorrect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (warningList.getSelectedIndex() != -1) {
                    saveResult("CORRECT");
                } else {
                    System.out.println("No warning selected. Please select a warning before saving.");
                }
            }
        });

        // Set frame properties
        frame.add(mainPanel);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateWarningList(String filePath) {
        warningRows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Only one sheet
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Starting from row 2
                Row row = sheet.getRow(i);
                if (row == null) continue;

                WarningData warningData = new WarningData();
                warningData.setWarningName(getStringValueFromCell(row.getCell(warningNameColumnIndex)));
                warningData.setIconName(getStringValueFromCell(row.getCell(iconNameColumnIndex)));
                warningData.setResult(getStringValueFromCell(row.getCell(resultColumnIndex)));

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
                // Update the result cell based on the configured result column index
                Cell cell = row.getCell(resultColumnIndex);
                if (cell == null) {
                    cell = row.createCell(resultColumnIndex);
                }
                cell.setCellValue(result);

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

        // Add the warning image in a scroll pane to handle large images
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

    // Column customization window: update the global column indices and then reload the Excel data.
    private void openColumnCustomizationWindow() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField warningField = new JTextField(String.valueOf(warningNameColumnIndex), 5);
        JTextField iconField = new JTextField(String.valueOf(iconNameColumnIndex), 5);
        JTextField resultField = new JTextField(String.valueOf(resultColumnIndex), 5);

        panel.add(new JLabel("Warning Name Column (zero-based):"));
        panel.add(warningField);
        panel.add(new JLabel("Icon Name Column (zero-based):"));
        panel.add(iconField);
        panel.add(new JLabel("Result Column (zero-based):"));
        panel.add(resultField);

        int option = JOptionPane.showConfirmDialog(frame, panel, "Configure Columns", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                // Parse new column indices
                warningNameColumnIndex = Integer.parseInt(warningField.getText().trim());
                iconNameColumnIndex = Integer.parseInt(iconField.getText().trim());
                resultColumnIndex = Integer.parseInt(resultField.getText().trim());
                // Reload the Excel file with the new configuration if an Excel file is already selected
                if (!baseExcelPath.isEmpty()) {
                    updateWarningList(baseExcelPath);
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(frame, "Please enter valid integer values for column indices.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
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
