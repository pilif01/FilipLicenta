package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class TextExcelTool {

    public static void filterExcelFile() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Select the original Excel file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select the Original Excel File");

                int result = fileChooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) return; // User cancelled
                File inputFile = fileChooser.getSelectedFile();

                // Select save location for the filtered Excel file
                fileChooser.setDialogTitle("Save Filtered Excel File with Languages");
                fileChooser.setSelectedFile(new File("Filtered_Warnings_Languages.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) return; // User cancelled
                File outputFile = fileChooser.getSelectedFile();

                // Process Excel file and create the correctly formatted output
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
        Sheet langsSheet = newWorkbook.createSheet("LANGS");  // Create LANGS sheet

        // Read header row and find column indexes
        Row headerRow = sheet.getRow(0);
        int tControlIndex = -1, hilIdIndex = -1;
        List<Integer> languageIndexes = new ArrayList<>();
        List<String> languageCodes = new ArrayList<>();
        List<String> fullLanguageNames = new ArrayList<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = headerRow.getCell(i).getStringCellValue().trim();
            if (header.equalsIgnoreCase("TControl")) {
                tControlIndex = i;
            } else if (header.equalsIgnoreCase("HilID")) {
                hilIdIndex = i;
            } else {
                // Assume all other columns are language texts
                languageIndexes.add(i);
                languageCodes.add(header);  // Short language codes (EN, DE, FR)
                fullLanguageNames.add(getFullLanguageName(header));  // Get full names
            }
        }

        if (tControlIndex == -1 || hilIdIndex == -1 || languageIndexes.isEmpty()) {
            throw new Exception("Required columns not found in the original Excel file.");
        }

        // ======== CREATE THE "LANGS" SHEET AS A SELECTION MENU ========
        Row langsHeaderRow = langsSheet.createRow(0);
        langsHeaderRow.createCell(0).setCellValue("RUN/SKIP");  // Selection column
        langsHeaderRow.createCell(1).setCellValue("Language Code");
        langsHeaderRow.createCell(2).setCellValue("Full Language Name");

        for (int i = 0; i < languageCodes.size(); i++) {
            Row langRow = langsSheet.createRow(i + 1);
            langRow.createCell(0).setCellValue("RUN");  // Default is RUN, user can change to SKIP
            langRow.createCell(1).setCellValue(languageCodes.get(i));
            langRow.createCell(2).setCellValue(fullLanguageNames.get(i));
        }

        // ======== CREATE SHEETS ONLY FOR LANGUAGES MARKED AS "RUN" ========
        Map<String, Sheet> languageSheets = new HashMap<>();
        for (int i = 1; i <= languageCodes.size(); i++) {
            String status = langsSheet.getRow(i).getCell(0).getStringCellValue().trim();
            if (status.equalsIgnoreCase("RUN")) {
                String langCode = langsSheet.getRow(i).getCell(1).getStringCellValue();
                Sheet langSheet = newWorkbook.createSheet(langCode);
                Row header = langSheet.createRow(0);
                header.createCell(0).setCellValue("TControl");
                header.createCell(1).setCellValue("Warning Name");
                header.createCell(2).setCellValue("Warning Text");
                languageSheets.put(langCode, langSheet);
            }
        }

        int newRowNum = 1;

        // Process each row and apply filters
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String tControl = row.getCell(tControlIndex).getStringCellValue().trim().toUpperCase();
            if (tControl.equals("SKIP")) continue; // Skip "SKIP" rows
            if (!tControl.equals("RUN")) continue; // Only keep "RUN" rows

            // Extract HilID (Warning Name)
            String hilID = row.getCell(hilIdIndex).getStringCellValue().trim();

            // Add row to each language sheet that is marked "RUN"
            for (int j = 0; j < languageIndexes.size(); j++) {
                String langCode = languageCodes.get(j);
                if (languageSheets.containsKey(langCode)) {
                    Sheet langSheet = languageSheets.get(langCode);
                    Row langRow = langSheet.createRow(newRowNum);
                    langRow.createCell(0).setCellValue(tControl);
                    langRow.createCell(1).setCellValue(hilID);
                    Cell cell = row.getCell(languageIndexes.get(j));
                    langRow.createCell(2).setCellValue(cell != null ? cell.getStringCellValue().trim() : "");
                }
            }
            newRowNum++;
        }

        // Auto-size columns
        for (Sheet sheetToResize : newWorkbook) {
            for (int i = 0; i < 3; i++) {
                sheetToResize.autoSizeColumn(i);
            }
        }

        // Save new Excel file
        FileOutputStream fos = new FileOutputStream(outputFile);
        newWorkbook.write(fos);
        fos.close();
        workbook.close();
    }

    private static String getFullLanguageName(String code) {
        Map<String, String> langMap = new HashMap<>();
        langMap.put("EN", "English");
        langMap.put("DE", "German");
        langMap.put("FR", "French");
        langMap.put("ES", "Spanish");
        langMap.put("IT", "Italian");
        langMap.put("NL", "Dutch");
        langMap.put("RU", "Russian");
        langMap.put("JA", "Japanese");
        langMap.put("ZH", "Chinese");
        return langMap.getOrDefault(code.toUpperCase(), "Unknown Language");
    }
}
