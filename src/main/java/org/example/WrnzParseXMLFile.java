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

public class WrnzParseXMLFile {

    public static void convertXMLToExcel() {
        SwingUtilities.invokeLater(() -> {
            try {
                // file chooser for xml file selection
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select XML File");

                int result = fileChooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File xmlFile = fileChooser.getSelectedFile();

                // select save location for excel file
                fileChooser.setDialogTitle("Save Excel File");
                fileChooser.setSelectedFile(new File("Text_Warnings.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File excelFile = fileChooser.getSelectedFile();

                // create progress dialog
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Processing...");
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                // progress bar initialization
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressDialog.add(progressBar);
                progressDialog.setVisible(true);

                // background worker to handle processing
                SwingWorker<Void, Integer> worker = new SwingWorker<>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        List<Warning> warnings = new ArrayList<>();
                        Set<String> processedHilIDs = new HashSet<>();
                        Set<String> languageCodes = new LinkedHashSet<>();

                        // create xml parser
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document document = builder.parse(xmlFile);
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
                                    languageCodes.add(langCode); // track all found languages
                                }
                            }

                            if (!languageTexts.isEmpty()) {
                                warnings.add(new Warning(hilID, icon, languageTexts));
                            }

                            int progress = (int) (((double) (i + 1) / totalScreens) * 50);
                            publish(progress);
                        }

                        Workbook workbook = new XSSFWorkbook();
                        Sheet sheet = workbook.createSheet("Warnings");
                        CellStyle wrapStyle = workbook.createCellStyle();
                        wrapStyle.setWrapText(true);

                        Row headerRow = sheet.createRow(0);
                        String[] headers = {"TControl", "HilID", "Icon"};
                        for (int i = 0; i < headers.length; i++) {
                            Cell cell = headerRow.createCell(i);
                            cell.setCellValue(headers[i]);
                            cell.setCellStyle(wrapStyle);
                        }

                        List<String> languageList = new ArrayList<>(languageCodes);
                        for (int i = 0; i < languageList.size(); i++) {
                            Cell cell = headerRow.createCell(headers.length + i);
                            cell.setCellValue(languageList.get(i));
                            cell.setCellStyle(wrapStyle);
                        }

                        int rowNum = 1;
                        for (Warning warning : warnings) {
                            Row row = sheet.createRow(rowNum++);
                            row.createCell(0).setCellValue("");
                            row.createCell(1).setCellValue(warning.hilID);
                            row.createCell(2).setCellValue(warning.icon);

                            int colIndex = headers.length;
                            for (String langCode : languageList) {
                                Cell cell = row.createCell(colIndex++);
                                cell.setCellValue(warning.languageTexts.getOrDefault(langCode, ""));
                                cell.setCellStyle(wrapStyle);
                            }

                            int progress = 50 + (int) (((double) rowNum / totalScreens) * 50);
                            publish(progress);
                        }

                        try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                            workbook.write(fos);
                        }

                        workbook.close();
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

    // warning object to hold parsed data
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
}
