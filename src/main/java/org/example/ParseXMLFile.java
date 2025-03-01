package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParseXMLFile {

    static class Warning {
        String screenId;
        String name;
        String classification;
        String color;
        String icon;
        Map<String, String> languageTexts;

        public Warning(String screenId, String name, String classification, String color, String icon, Map<String, String> languageTexts) {
            this.screenId = screenId;
            this.name = name;
            this.classification = classification;
            this.color = color;
            this.icon = icon;
            this.languageTexts = languageTexts;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select XML File");

            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File xmlFile = fileChooser.getSelectedFile();

                fileChooser.setDialogTitle("Save Excel File");
                fileChooser.setSelectedFile(new File("Text_Warnings.xlsx"));
                result = fileChooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File excelFile = fileChooser.getSelectedFile();

                    try {
                        List<Warning> warnings = parseXML(xmlFile.getAbsolutePath());
                        writeExcel(warnings, excelFile.getAbsolutePath());
                        JOptionPane.showMessageDialog(null, "Excel file created successfully at: " + excelFile.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    private static List<Warning> parseXML(String filePath) throws Exception {
        List<Warning> warnings = new ArrayList<>();
        Set<String> processedScreenIds = new HashSet<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(filePath));

        NodeList screenNodes = document.getElementsByTagName("Screen");
        for (int i = 0; i < screenNodes.getLength(); i++) {
            Element screen = (Element) screenNodes.item(i);
            String screenId = screen.getAttribute("Hil-ID");

            if (processedScreenIds.contains(screenId)) {
                continue; // Skip duplicate Screen IDs
            }

            NodeList textResourceNodes = screen.getElementsByTagName("TextResource");
            if (textResourceNodes.getLength() == 0) {
                continue; // Skip screens with no text resources
            }

            processedScreenIds.add(screenId);

            for (int j = 0; j < textResourceNodes.getLength(); j++) {
                Element textResource = (Element) textResourceNodes.item(j);

                Element metaData = (Element) textResource.getElementsByTagName("MetaData").item(0);
                String name = getTagValue("Name", metaData);
                String classification = getTagValue("Classification", metaData);
                String color = getTagValue("Color", metaData);
                String icon = getTagValue("Icon", metaData);

                NodeList languageNodes = textResource.getElementsByTagName("Language");
                Map<String, String> languageTexts = new HashMap<>();
                for (int k = 0; k < languageNodes.getLength(); k++) {
                    Element language = (Element) languageNodes.item(k);
                    String langCode = language.getAttribute("lang");
                    String text = language.getTextContent().trim();
                    languageTexts.put(langCode, text);
                }

                warnings.add(new Warning(screenId, name, classification, color, icon, languageTexts));
            }
        }

        return warnings;
    }

    private static String getTagValue(String tagName, Element element) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    private static void writeExcel(List<Warning> warnings, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Warnings");

        // Add Tcontrol row
        Row tcontrolRow = sheet.createRow(0);
        tcontrolRow.createCell(0).setCellValue("Tcontrol");
        tcontrolRow.createCell(1).setCellValue("RUN");

        // Headers row
        Row headerRow = sheet.createRow(1);
        String[] headers = {"Screen ID", "Name", "Classification", "Color", "Icon"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Add language columns dynamically
        List<String> languageCodes = new ArrayList<>();
        for (Warning warning : warnings) {
            languageCodes.addAll(warning.languageTexts.keySet());
        }
        languageCodes = new ArrayList<>(new HashSet<>(languageCodes)); // Remove duplicates

        for (int i = 0; i < languageCodes.size(); i++) {
            headerRow.createCell(headers.length + i).setCellValue(languageCodes.get(i));
        }

        // Data rows
        int rowNum = 2;
        for (Warning warning : warnings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(warning.screenId);
            row.createCell(1).setCellValue(warning.name);
            row.createCell(2).setCellValue(warning.classification);
            row.createCell(3).setCellValue(warning.color);
            row.createCell(4).setCellValue(warning.icon);

            int colIndex = headers.length;
            for (String langCode : languageCodes) {
                row.createCell(colIndex++).setCellValue(warning.languageTexts.getOrDefault(langCode, ""));
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length + languageCodes.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        workbook.close();
    }
}