package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.*;

public class ImgExcelTool {

    public static void filterExcelFile() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Select the original Excel file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select the Original Excel File");

                int result = fileChooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return; // User cancelled
                }
                File inputFile = fileChooser.getSelectedFile();

                // Select save location for the filtered Excel file
                fileChooser.setDialogTitle("Save Filtered Excel File");
                fileChooser.setSelectedFile(new File("Filtered_Warnings.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return; // User cancelled
                }
                File outputFile = fileChooser.getSelectedFile();

                // Process Excel file and create a new one
                processExcel(inputFile, outputFile);

                JOptionPane.showMessageDialog(null, "Filtered Excel file created successfully at: " + outputFile.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void processExcel(File inputFile, File outputFile) throws Exception {
        FileInputStream fis = new FileInputStream(inputFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        Workbook newWorkbook = new XSSFWorkbook();
        Sheet newSheet = newWorkbook.createSheet("Filtered Warnings");

        // Read header row and find column indexes
        Row headerRow = sheet.getRow(0);
        int tControlIndex = -1, hilIdIndex = -1, iconIndex = -1;

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = headerRow.getCell(i).getStringCellValue().trim();
            if (header.equalsIgnoreCase("TControl")) tControlIndex = i;
            else if (header.equalsIgnoreCase("HilID")) hilIdIndex = i;
            else if (header.equalsIgnoreCase("Icon")) iconIndex = i;
        }

        if (tControlIndex == -1 || hilIdIndex == -1 || iconIndex == -1) {
            throw new Exception("Required columns not found in the original Excel file.");
        }

        // Create new header row in filtered Excel file
        Row newHeaderRow = newSheet.createRow(0);
        newHeaderRow.createCell(0).setCellValue("TControl");
        newHeaderRow.createCell(1).setCellValue("HilID");
        newHeaderRow.createCell(2).setCellValue("Warning Image");

        int newRowNum = 1;

        // Process each row and apply filters
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String tControl = row.getCell(tControlIndex).getStringCellValue().trim().toUpperCase();
            if (tControl.equals("SKIP")) continue; // Skip "SKIP" rows
            if (!tControl.equals("RUN")) continue; // Only keep "RUN" rows

            // Extract HilID and Icon
            String hilID = row.getCell(hilIdIndex).getStringCellValue().trim();
            String warningImage = row.getCell(iconIndex).getStringCellValue().trim();

            // Add row to new Excel file
            Row newRow = newSheet.createRow(newRowNum++);
            newRow.createCell(0).setCellValue(tControl);
            newRow.createCell(1).setCellValue(hilID);
            newRow.createCell(2).setCellValue(warningImage);
        }

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            newSheet.autoSizeColumn(i);
        }

        // Save new Excel file
        FileOutputStream fos = new FileOutputStream(outputFile);
        newWorkbook.write(fos);
        fos.close();
        workbook.close();
    }
}
