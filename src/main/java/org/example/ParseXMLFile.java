package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.*;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class ParseXMLFile {

    public static void convertXMLToExcel() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Select XML file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select XML File");

                int result = fileChooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return; // User cancelled
                }
                File xmlFile = fileChooser.getSelectedFile();

                // Select save location for Excel file
                fileChooser.setDialogTitle("Save Excel File");
                fileChooser.setSelectedFile(new File("Text_Warnings.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return; // User cancelled
                }
                File excelFile = fileChooser.getSelectedFile();

                // Show a progress bar
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Processing...");
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressDialog.add(progressBar);
                progressDialog.setVisible(true);

                // Run conversion in a background thread
                SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        List<Warning> warnings = parseXML(xmlFile, progressBar);
                        writeExcel(warnings, excelFile, progressBar);
                        return null;
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose(); // Close progress dialog
                        JOptionPane.showMessageDialog(null, "Excel file created successfully at: " + excelFile.getAbsolutePath());
                    }
                };
                worker.execute();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Class to store warning details
    static class Warning {
        String hilID;
        String icon;
        Map<String, String> languageTexts;

        public Warning(String hilID, String icon, Map<String, String> languageTexts) {
            this.hilID = hilID;
            this.icon = icon;
            this.languageTexts = languageTexts;
        }
    }

    private static List<Warning> parseXML(File file, JProgressBar progressBar) throws Exception {
        List<Warning> warnings = new ArrayList<>();
        Set<String> processedHilIDs = new HashSet<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);
        document.getDocumentElement().normalize();

        NodeList screenNodes = document.getElementsByTagName("Screen");
        int totalScreens = screenNodes.getLength();

        for (int i = 0; i < totalScreens; i++) {
            Element screen = (Element) screenNodes.item(i);
            String hilID = screen.getAttribute("Hil-ID");

            if (processedHilIDs.contains(hilID)) {
                continue;
            }
            processedHilIDs.add(hilID);

            NodeList textResources = screen.getElementsByTagName("TextResource");
            String icon = "";
            Map<String, String> languageTexts = new HashMap<>();

            for (int j = 0; j < textResources.getLength(); j++) {
                Element textResource = (Element) textResources.item(j);

                if (textResource.getElementsByTagName("Languages").getLength() == 0) {
                    continue;
                }

                if (icon.isEmpty()) {
                    NodeList variables = textResource.getElementsByTagName("Variable");
                    for (int k = 0; k < variables.getLength(); k++) {
                        Element variable = (Element) variables.item(k);
                        if ("Icon".equals(variable.getAttribute("daimlerType"))) {
                            icon = variable.getAttribute("daimlerIDReference");
                            break;
                        }
                    }
                }

                NodeList languageNodes = textResource.getElementsByTagName("Language");
                for (int k = 0; k < languageNodes.getLength(); k++) {
                    Element language = (Element) languageNodes.item(k);
                    String langCode = language.getAttribute("lang");
                    String text = language.getTextContent().trim();
                    languageTexts.put(langCode, text);
                }
            }

            if (!languageTexts.isEmpty()) {
                warnings.add(new Warning(hilID, icon, languageTexts));
            }

            // Update progress
            int progress = (int) (((double) (i + 1) / totalScreens) * 50); // 50% allocated to parsing
            progressBar.setValue(progress);
        }

        return warnings;
    }

    private static void writeExcel(List<Warning> warnings, File file, JProgressBar progressBar) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Warnings");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"TControl", "HilID", "Icon"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        Set<String> languageCodes = new LinkedHashSet<>();
        for (Warning warning : warnings) {
            languageCodes.addAll(warning.languageTexts.keySet());
        }

        List<String> languageList = new ArrayList<>(languageCodes);
        for (int i = 0; i < languageList.size(); i++) {
            headerRow.createCell(headers.length + i).setCellValue(languageList.get(i));
        }

        int rowNum = 1;
        int totalWarnings = warnings.size();
        for (int i = 0; i < totalWarnings; i++) {
            Warning warning = warnings.get(i);
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(""); // TControl (Blank)
            row.createCell(1).setCellValue(warning.hilID);
            row.createCell(2).setCellValue(warning.icon);

            int colIndex = headers.length;
            for (String langCode : languageList) {
                row.createCell(colIndex++).setCellValue(warning.languageTexts.getOrDefault(langCode, ""));
            }

            // Update progress
            int progress = 50 + (int) (((double) (i + 1) / totalWarnings) * 50); // Remaining 50% allocated to writing
            progressBar.setValue(progress);
        }

        for (int i = 0; i < headers.length + languageList.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }

        workbook.close();
    }
}
