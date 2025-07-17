package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.*;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.List;

public class MenuTextUnits {

    // Desired fixed column order by region code
    private static final List<String> LANGUAGE_ORDER = Arrays.asList(
            "DE","GB","US","SA","HK","BG","CZ","CN","GR","TW",
            "DK","FI","FR","IL","HR","HU","ID","IT","JP","KR",
            "MY","NL","NO","PL","BR","PT","RO","RU","SK","SI",
            "ES","RS","SE","TH","TR","UA","VN"
    );

    public static void convertXMLToExcel() {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1) Pick the XML file
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select TextResource XML");
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
                File xmlFile = chooser.getSelectedFile();

                // 2) Pick where to save the Excel
                chooser.setDialogTitle("Save Excel File");
                chooser.setSelectedFile(new File("TextResources.xlsx"));
                if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
                File excelFile = chooser.getSelectedFile();

                // 3) Build modal progress dialog
                JDialog progressDialog = new JDialog((Frame) null, "Processing…", true);
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressDialog.getContentPane().add(progressBar);
                progressDialog.pack();
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                // 4) SwingWorker
                SwingWorker<Void,Integer> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        // Parse XML
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db  = dbf.newDocumentBuilder();
                        Document doc       = db.parse(xmlFile);
                        doc.getDocumentElement().normalize();

                        NodeList trNodes = doc.getElementsByTagName("TextResource");
                        int total = trNodes.getLength();

                        // Collect rows
                        List<Warning> rows = new ArrayList<>();

                        for (int i = 0; i < total; i++) {
                            Element tr = (Element) trNodes.item(i);
                            String textId = tr.getAttribute("textId");
                            if (textId == null || textId.isEmpty()) continue;

                            // Map region code -> translated text
                            Map<String,String> map = new HashMap<>();
                            NodeList langNodes = tr.getElementsByTagName("Language");
                            for (int j = 0; j < langNodes.getLength(); j++) {
                                Element langEl = (Element) langNodes.item(j);
                                String fullCode = langEl.getAttribute("lang");
                                String[] parts = fullCode.split("_");
                                String region = parts[parts.length-1].toUpperCase(); // last part
                                String txt    = langEl.getTextContent().trim();
                                map.put(region, txt);
                            }
                            if (!map.isEmpty()) {
                                rows.add(new Warning(textId, map));
                            }
                            publish((i+1)*100/total);
                        }

                        // Build Excel
                        Workbook wb = new XSSFWorkbook();
                        Sheet sh = wb.createSheet("TextResources");
                        CellStyle wrap = wb.createCellStyle();
                        wrap.setWrapText(true);

                        // Header
                        Row hdr = sh.createRow(0);
                        Cell c0 = hdr.createCell(0);
                        c0.setCellValue("TextResource ID");
                        c0.setCellStyle(wrap);
                        for (int c = 0; c < LANGUAGE_ORDER.size(); c++) {
                            Cell hc = hdr.createCell(1 + c);
                            hc.setCellValue(LANGUAGE_ORDER.get(c));
                            hc.setCellStyle(wrap);
                        }

                        // Data
                        for (int r = 0; r < rows.size(); r++) {
                            Warning w = rows.get(r);
                            Row row = sh.createRow(r+1);
                            row.createCell(0).setCellValue(w.textId);
                            for (int c = 0; c < LANGUAGE_ORDER.size(); c++) {
                                String region = LANGUAGE_ORDER.get(c);
                                String txt = w.languageTexts.getOrDefault(region, "");
                                Cell dc = row.createCell(1 + c);
                                dc.setCellValue(txt);
                                dc.setCellStyle(wrap);
                            }
                            publish(50 + (r+1)*50/rows.size());
                        }

                        // Autosize
                        for (int c = 0; c < 1 + LANGUAGE_ORDER.size(); c++) {
                            sh.autoSizeColumn(c);
                        }

                        // Save
                        try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                            wb.write(fos);
                        }
                        wb.close();
                        return null;
                    }

                    @Override
                    protected void process(List<Integer> chunks) {
                        for (int v : chunks) progressBar.setValue(v);
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(
                                null,
                                "Excel saved to:\n" + excelFile.getAbsolutePath(),
                                "Done",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                };

                // Start worker then show dialog
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

    // Holds one TextResource ID and its region→text map
    static class Warning {
        final String textId;
        final Map<String,String> languageTexts;
        Warning(String textId, Map<String,String> languageTexts){
            this.textId = textId;
            this.languageTexts = languageTexts;
        }
    }
}
