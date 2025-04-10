package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class WrnTextExcelTool {

    public static void filterExcelFile() {
        SwingUtilities.invokeLater(() -> {
            try {
                // File chooser for original Excel file.
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select the Original Excel File");

                int result = fileChooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) return;
                File inputFile = fileChooser.getSelectedFile();

                // File chooser for saving filtered Excel file.
                fileChooser.setDialogTitle("Save Filtered Excel File with Languages");
                fileChooser.setSelectedFile(new File("Filtered_Warnings_Languages.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) return;
                File outputFile = fileChooser.getSelectedFile();

                // Setup progress dialog.
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Processing...");
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressDialog.add(progressBar);
                progressDialog.setVisible(true);

                // SwingWorker to handle processing.
                SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        FileInputStream fis = new FileInputStream(inputFile);
                        Workbook workbook = new XSSFWorkbook(fis);
                        Sheet sheet = workbook.getSheetAt(0);

                        Workbook newWorkbook = new XSSFWorkbook();
                        Sheet langsSheet = newWorkbook.createSheet("LANGS");

                        Row headerRow = sheet.getRow(0);
                        int totalRows = sheet.getLastRowNum();

                        // Create a cell style with wrapped text.
                        CellStyle wrapStyle = newWorkbook.createCellStyle();
                        wrapStyle.setWrapText(true);

                        int languageStartIndex = 3; // First 3 columns are TControl, HilID, Icon.

                        // Create header row for the LANGS sheet.
                        Row langsHeaderRow = langsSheet.createRow(0);
                        langsHeaderRow.createCell(0).setCellValue("RUN/SKIP");
                        langsHeaderRow.createCell(1).setCellValue("Language Code");

                        List<String> languageCodes = new ArrayList<>();
                        Map<String, String> languageMap = new HashMap<>();

                        // Extract language codes from header row.
                        for (int i = languageStartIndex; i < headerRow.getLastCellNum(); i++) {
                            String originalLangCode = headerRow.getCell(i).getStringCellValue();
                            String shortenedCode = getShortLanguageCode(originalLangCode);
                            languageCodes.add(shortenedCode);
                            languageMap.put(shortenedCode, originalLangCode);

                            Row langRow = langsSheet.createRow(i - languageStartIndex + 1);
                            langRow.createCell(0).setCellValue("RUN");
                            langRow.createCell(1).setCellValue(shortenedCode);
                        }

                        // Create a sheet for each language.
                        Map<String, Sheet> languageSheets = new HashMap<>();
                        for (String langCode : languageCodes) {
                            Sheet langSheet = newWorkbook.createSheet(langCode);
                            Row header = langSheet.createRow(0);
                            header.createCell(0).setCellValue("TControl");
                            header.createCell(1).setCellValue("HilID");
                            header.createCell(2).setCellValue("Warning Text");
                            languageSheets.put(langCode, langSheet);
                        }

                        // Process each row of the original sheet.
                        for (int i = 1; i <= totalRows; i++) {
                            Row row = sheet.getRow(i);
                            if (row == null) continue;

                            String tControl = row.getCell(0).getStringCellValue().trim();
                            if (!"RUN".equalsIgnoreCase(tControl)) continue;

                            String hilID = row.getCell(1).getStringCellValue().trim();

                            for (int j = languageStartIndex; j < row.getLastCellNum(); j++) {
                                String originalLangCode = headerRow.getCell(j).getStringCellValue();
                                String shortenedCode = getShortLanguageCode(originalLangCode);
                                Sheet langSheet = languageSheets.get(shortenedCode);
                                if (langSheet == null) continue;

                                String warningText = row.getCell(j) != null ? row.getCell(j).getStringCellValue().trim() : "";
                                int rowNum = langSheet.getLastRowNum() + 1;
                                Row langRow = langSheet.createRow(rowNum);
                                langRow.createCell(0).setCellValue(tControl);
                                langRow.createCell(1).setCellValue(hilID);
                                Cell cell = langRow.createCell(2);
                                cell.setCellValue(warningText);
                                cell.setCellStyle(wrapStyle);
                            }

                            int progress = (int) (((double) i / totalRows) * 100);
                            publish(progress);
                        }

                        // Auto-size the columns for each language sheet.
                        for (Sheet langSheet : languageSheets.values()) {
                            // Auto-size columns 0 (TControl), 1 (HilID), and 2 (Warning Text).
                            for (int col = 0; col < 3; col++) {
                                langSheet.autoSizeColumn(col);
                                // If this is the text column, add extra padding.
                                if (col == 2) {
                                    int currentWidth = langSheet.getColumnWidth(col);
                                    // Increase width by 20% (or adjust the value as needed).
                                    langSheet.setColumnWidth(col, (int) (currentWidth * 1.2));
                                }
                            }
                        }

                        // Optionally auto-size columns in the LANGS sheet.
                        for (int col = 0; col < 2; col++) {
                            langsSheet.autoSizeColumn(col);
                        }

                        // Write the new workbook to the output file.
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            newWorkbook.write(fos);
                        }

                        workbook.close();
                        fis.close();
                        return null;
                    }

                    @Override
                    protected void process(List<Integer> chunks) {
                        for (int value : chunks) {
                            progressBar.setValue(value);
                        }
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(null,
                                "Filtered Excel file created successfully at: " + outputFile.getAbsolutePath());
                    }
                };

                worker.execute();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Extract short language code (e.g., 'deu_Latn_DE' -> 'DE').
    private static String getShortLanguageCode(String code) {
        if (code.contains("_")) {
            String[] parts = code.split("_");
            return parts[parts.length - 1];  // always return the last part, e.g., 'DE'
        }
        return code;
    }
}
