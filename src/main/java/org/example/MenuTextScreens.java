package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.*;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream; // Re-added as per user's provided code structure
import java.io.FileOutputStream;
import java.util.*;
import java.util.List;

public class MenuTextScreens {

    public static void convertXMLToExcel() {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1) Choose the XML file
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select TextResource XML");
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
                File xmlFile = chooser.getSelectedFile();

                // 2) Choose where to save the Excel
                chooser.setDialogTitle("Save Excel File");
                // The user explicitly requested "TextResources.xlsx" as default name
                chooser.setSelectedFile(new File("TextResources.xlsx"));
                if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
                File selectedExcelFile = chooser.getSelectedFile(); // Use a new variable for the selected file

                // Ensure the file has .xlsx extension if not already present
                String excelFilePath = selectedExcelFile.getAbsolutePath();
                if (!excelFilePath.toLowerCase().endsWith(".xlsx") && !excelFilePath.toLowerCase().endsWith(".xls")) {
                    excelFilePath = excelFilePath + ".xlsx"; // Default to .xlsx
                }
                // Declare excelFile as effectively final by assigning it here
                File excelFile = new File(excelFilePath);


                // 3) Build the modal progress dialog
                JDialog progressDialog = new JDialog((Frame) null, "Processing…", true);
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressDialog.getContentPane().add(progressBar);
                progressDialog.pack();
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                // 4) Define and launch the SwingWorker
                // excelFile is now effectively final, so it can be accessed from within the inner class
                SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        // Parse XML
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(xmlFile);
                        doc.getDocumentElement().normalize();

                        // Find all <TextResource> nodes
                        NodeList trNodes = doc.getElementsByTagName("TextResource");
                        int total = trNodes.getLength();

                        // Reverted to "Warning" class name as per user's request for original structure
                        List<Warning> rows = new ArrayList<>();
                        Set<String> languageCodes = new LinkedHashSet<>(); // Stores "deu_Latn_DE", "eng", etc.

                        // Phase 1: Parse XML and collect data
                        for (int i = 0; i < total; i++) {
                            Element tr = (Element) trNodes.item(i);
                            String textId = tr.getAttribute("textId");
                            if (textId == null || textId.isEmpty()) continue;

                            NodeList langNodes = tr.getElementsByTagName("Language");
                            Map<String, String> map = new HashMap<>(); // fullLangCode -> text
                            for (int j = 0; j < langNodes.getLength(); j++) {
                                Element lang = (Element) langNodes.item(j);
                                String code = lang.getAttribute("lang"); // e.g., "deu_Latn_DE"
                                String txt = lang.getTextContent().trim();
                                map.put(code, txt);
                                languageCodes.add(code); // Collect all unique full language codes
                            }

                            if (!map.isEmpty()) {
                                rows.add(new Warning(textId, map));
                            }
                            publish(i * 50 / total); // Progress for XML parsing (0-50%)
                        }

                        // Phase 2: Build Excel workbook with a single sheet
                        Workbook wb = new XSSFWorkbook();
                        Sheet sheet = wb.createSheet("TextResources"); // Create a single sheet named "TextResources"
                        CellStyle wrap = wb.createCellStyle();
                        wrap.setWrapText(true);

                        List<String> sortedFullLanguageCodes = new ArrayList<>(languageCodes);
                        // Sort language codes for consistent column order
                        Collections.sort(sortedFullLanguageCodes);

                        // Header row
                        Row hdr = sheet.createRow(0);
                        Cell h0 = hdr.createCell(0);
                        h0.setCellValue("TextResource ID");
                        h0.setCellStyle(wrap);

                        // Add language headers - now using the last two characters
                        for (int c = 0; c < sortedFullLanguageCodes.size(); c++) {
                            Cell hc = hdr.createCell(1 + c);
                            String fullLangCode = sortedFullLanguageCodes.get(c);
                            String displayLangCode;
                            if (fullLangCode.length() >= 2) {
                                // Take the last 2 characters and convert to uppercase
                                displayLangCode = fullLangCode.substring(fullLangCode.length() - 2).toUpperCase();
                            } else {
                                // Fallback for very short codes (less than 2 characters)
                                displayLangCode = fullLangCode.toUpperCase();
                            }
                            hc.setCellValue(displayLangCode); // Use the derived short language code as header
                            hc.setCellStyle(wrap);
                        }

                        // Data rows
                        for (int r = 0; r < rows.size(); r++) {
                            Warning w = rows.get(r); // Using Warning class
                            Row row = sheet.createRow(r + 1); // Data starts from row 1

                            row.createCell(0).setCellValue(w.textId);

                            // Populate text for each language in respective columns
                            for (int c = 0; c < sortedFullLanguageCodes.size(); c++) {
                                String currentLangCode = sortedFullLanguageCodes.get(c);
                                Cell dc = row.createCell(1 + c);
                                dc.setCellValue(w.languageTexts.getOrDefault(currentLangCode, ""));
                                dc.setCellStyle(wrap);
                            }
                            // Adjusted progress calculation to match the original logic better
                            publish(50 + ((r + 1) * 50 / rows.size()));
                        }

                        // Autosize all columns
                        for (int c = 0; c < 1 + sortedFullLanguageCodes.size(); c++) {
                            sheet.autoSizeColumn(c);
                        }

                        // Save workbook
                        try (FileOutputStream fos = new FileOutputStream(excelFile)) { // excelFile is effectively final
                            wb.write(fos);
                        }
                        wb.close();
                        return null;
                    }

                    @Override
                    protected void process(List<Integer> chunks) {
                        for (int v : chunks) {
                            progressBar.setValue(v);
                        }
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(
                                null,
                                "Excel saved at:\n" + excelFile.getAbsolutePath(), // excelFile is effectively final
                                "Done",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                };

                // **Start** the background work, then show the modal dialog
                worker.execute();
                progressDialog.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    /** Holds one TextResource ID and its language→text map */
    static class Warning { // Reverted to "Warning" as per user's request
        String textId;
        Map<String,String> languageTexts;
        public Warning(String textId, Map<String,String> languageTexts) {
            this.textId = textId;
            this.languageTexts = languageTexts;
        }
    }
}
