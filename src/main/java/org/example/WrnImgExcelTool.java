package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.*;

public class WrnImgExcelTool {

    public static void filterExcelFile() {
        SwingUtilities.invokeLater(() -> {
            try {
                // file chooser for original excel file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select the Original Excel File");

                int result = fileChooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return; // user cancelled
                }
                File inputFile = fileChooser.getSelectedFile();

                // file chooser for saving filtered excel file
                fileChooser.setDialogTitle("Save Filtered Excel File");
                fileChooser.setSelectedFile(new File("Filtered_Warnings.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return; // user cancelled
                }
                File outputFile = fileChooser.getSelectedFile();

                // progress dialog setup
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Processing...");
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressDialog.add(progressBar);
                progressDialog.setVisible(true);

                // SwingWorker for processing
                SwingWorker<Void, Integer> worker = new SwingWorker<>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        FileInputStream fis = new FileInputStream(inputFile);
                        Workbook workbook = new XSSFWorkbook(fis);
                        Sheet sheet = workbook.getSheetAt(0);

                        Workbook newWorkbook = new XSSFWorkbook();
                        Sheet newSheet = newWorkbook.createSheet("Filtered Warnings");

                        Row headerRow = sheet.getRow(0);
                        int totalRows = sheet.getLastRowNum();

                        // set up column indexes
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

                        // create header row for new sheet
                        Row newHeaderRow = newSheet.createRow(0);
                        newHeaderRow.createCell(0).setCellValue("TControl");
                        newHeaderRow.createCell(1).setCellValue("HilID");
                        newHeaderRow.createCell(2).setCellValue("Warning Image");

                        int newRowNum = 1;

                        for (int i = 1; i <= totalRows; i++) {
                            Row row = sheet.getRow(i);
                            if (row == null) continue;

                            String tControl = row.getCell(tControlIndex).getStringCellValue().trim();
                            if (!"RUN".equalsIgnoreCase(tControl)) continue;

                            String hilID = row.getCell(hilIdIndex).getStringCellValue().trim();
                            Cell iconCell = row.getCell(iconIndex);
                            String warningImage = (iconCell == null || iconCell.getStringCellValue().trim().isEmpty()) ? "NO_ICON" : iconCell.getStringCellValue().trim();

                            Row newRow = newSheet.createRow(newRowNum++);
                            newRow.createCell(0).setCellValue(tControl);
                            newRow.createCell(1).setCellValue(hilID);
                            newRow.createCell(2).setCellValue(warningImage);

                            int progress = (int) (((double) i / totalRows) * 100);
                            publish(progress);
                        }

                        for (int i = 0; i < 3; i++) {
                            newSheet.autoSizeColumn(i);
                        }

                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            newWorkbook.write(fos);
                        }

                        workbook.close();
                        fis.close();
                        return null;
                    }

                    @Override
                    protected void process(java.util.List<Integer> chunks) {
                        for (int value : chunks) {
                            progressBar.setValue(value);
                        }
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(null, "Filtered Excel file created successfully at: " + outputFile.getAbsolutePath());
                    }
                };

                worker.execute();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
