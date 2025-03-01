package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ManualIconTestTool extends JFrame {
    private JLabel warningImageLabel;
    private JLabel iconImageLabel;
    private JList<String> warningList;
    private File selectedIconFolder;
    private File selectedImageFolder;
    private File selectedExcelFile;

    private List<RowData> rows;
    private int currentIndex;

    public ManualIconTestTool() {
        setTitle("Manual Icon Test");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout());

        // Image panel
        JPanel imagePanel = new JPanel(new GridLayout(1, 2));
        warningImageLabel = new JLabel("No Warning Image", SwingConstants.CENTER);
        iconImageLabel = new JLabel("No Icon Image", SwingConstants.CENTER);
        imagePanel.add(warningImageLabel);
        imagePanel.add(iconImageLabel);
        add(imagePanel, BorderLayout.CENTER);

        // Warning list
        warningList = new JList<>(new DefaultListModel<>());
        warningList.addListSelectionListener(e -> {
            currentIndex = warningList.getSelectedIndex();
            if (currentIndex >= 0 && currentIndex < rows.size()) {
                displayRow(rows.get(currentIndex));
            }
        });
        add(new JScrollPane(warningList), BorderLayout.SOUTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton nokButton = new JButton("NOK");
        JButton squashButton = new JButton("Squash");
        buttonPanel.add(okButton);
        buttonPanel.add(nokButton);
        buttonPanel.add(squashButton);

        JButton selectExcelButton = new JButton("Select Excel");
        JButton selectImageFolderButton = new JButton("Select Image Folder");
        JButton selectIconFolderButton = new JButton("Select Icon Folder");
        buttonPanel.add(selectExcelButton);
        buttonPanel.add(selectImageFolderButton);
        buttonPanel.add(selectIconFolderButton);

        add(buttonPanel, BorderLayout.NORTH);

        // Event listeners
        selectExcelButton.addActionListener(this::handleSelectExcel);
        selectImageFolderButton.addActionListener(this::handleSelectImageFolder);
        selectIconFolderButton.addActionListener(this::handleSelectIconFolder);

        okButton.addActionListener(e -> handleResult("OK"));
        nokButton.addActionListener(e -> handleResult("NOK"));
        squashButton.addActionListener(e -> handleResult("Squash"));

        rows = new ArrayList<>();
        currentIndex = -1;
    }

    private void handleSelectExcel(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedExcelFile = fileChooser.getSelectedFile();
            loadExcelData();
        }
    }

    private void handleSelectImageFolder(ActionEvent e) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFolder = folderChooser.getSelectedFile();
        }
    }

    private void handleSelectIconFolder(ActionEvent e) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedIconFolder = folderChooser.getSelectedFile();
        }
    }

    private void loadExcelData() {
        if (selectedExcelFile == null) return;

        try (FileInputStream fis = new FileInputStream(selectedExcelFile)) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheetAt(0); // Assume data is in the first sheet
            rows.clear();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);
                if (row != null) {
                    RowData rowData = new RowData();
                    rowData.setReference(getCellValue(row.getCell(0)));
                    rowData.setMenuName(getCellValue(row.getCell(1)));
                    rowData.setImgPath(selectedImageFolder + "/" + getCellValue(row.getCell(1)) + ".png");
                    rows.add(rowData);
                }
            }
            updateWarningList();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading Excel data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        return "";
    }

    private void updateWarningList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (RowData rowData : rows) {
            listModel.addElement(rowData.getMenuName());
        }
        warningList.setModel(listModel);
    }

    private void displayRow(RowData rowData) {
        warningImageLabel.setIcon(new ImageIcon(rowData.getImgPath()));
        if (selectedIconFolder != null) {
            File iconFile = new File(selectedIconFolder, rowData.getMenuName() + ".png");
            if (iconFile.exists()) {
                iconImageLabel.setIcon(new ImageIcon(iconFile.getAbsolutePath()));
            } else {
                iconImageLabel.setIcon(null);
            }
        }
    }

    private void handleResult(String result) {
        if (currentIndex < 0 || currentIndex >= rows.size() || selectedExcelFile == null) return;

        RowData rowData = rows.get(currentIndex);
        try (FileInputStream fis = new FileInputStream(selectedExcelFile);
             FileOutputStream fos = new FileOutputStream(selectedExcelFile)) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFRow row = sheet.getRow(currentIndex + 1);
            if (row == null) row = sheet.createRow(currentIndex + 1);
            Cell resultCell = row.createCell(2); // Assume result goes in column C
            resultCell.setCellValue(result);
            workbook.write(fos);
            JOptionPane.showMessageDialog(this, "Result saved: " + result, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving result: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ManualIconTestTool tool = new ManualIconTestTool();
            tool.setVisible(true);
        });
    }
}
