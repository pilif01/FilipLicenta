package org.example;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AutomatedWarningTesting extends JDialog {

    private JTextField excelPathField, picturesFolderField, croppedImagesFolderField, resultsExcelField, cropAreaField;
    private JButton startButton, selectExcelButton, selectPicturesButton, selectCroppedImagesButton, selectResultsButton, selectCropAreaButton;
    private Rectangle selectedArea;

    public AutomatedWarningTesting(JFrame parent) {
        super(parent, "Automated Warning Testing", true);

        setSize(600, 500);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel selectExcelLabel = new JLabel("Select Filtered Excel File:");
        selectExcelLabel.setBounds(20, 20, 200, 30);
        add(selectExcelLabel);

        excelPathField = new JTextField();
        excelPathField.setBounds(20, 50, 400, 30);
        add(excelPathField);

        selectExcelButton = new JButton("Browse");
        selectExcelButton.setBounds(440, 50, 100, 30);
        add(selectExcelButton);

        JLabel selectPicturesLabel = new JLabel("Select Pictures Folder:");
        selectPicturesLabel.setBounds(20, 90, 200, 30);
        add(selectPicturesLabel);

        picturesFolderField = new JTextField();
        picturesFolderField.setBounds(20, 120, 400, 30);
        add(picturesFolderField);

        selectPicturesButton = new JButton("Browse");
        selectPicturesButton.setBounds(440, 120, 100, 30);
        add(selectPicturesButton);

        JLabel selectCroppedImagesLabel = new JLabel("Select Cropped Images Folder:");
        selectCroppedImagesLabel.setBounds(20, 160, 300, 30);
        add(selectCroppedImagesLabel);

        croppedImagesFolderField = new JTextField();
        croppedImagesFolderField.setBounds(20, 190, 400, 30);
        add(croppedImagesFolderField);

        selectCroppedImagesButton = new JButton("Browse");
        selectCroppedImagesButton.setBounds(440, 190, 100, 30);
        add(selectCroppedImagesButton);

        JLabel selectResultsLabel = new JLabel("Select Results Excel Save Location:");
        selectResultsLabel.setBounds(20, 230, 300, 30);
        add(selectResultsLabel);

        resultsExcelField = new JTextField();
        resultsExcelField.setBounds(20, 260, 400, 30);
        add(resultsExcelField);

        selectResultsButton = new JButton("Browse");
        selectResultsButton.setBounds(440, 260, 100, 30);
        add(selectResultsButton);

        JLabel cropAreaLabel = new JLabel("Crop Area (x, y, width, height):");
        cropAreaLabel.setBounds(20, 300, 300, 30);
        add(cropAreaLabel);

        cropAreaField = new JTextField();
        cropAreaField.setBounds(20, 330, 300, 30);
        add(cropAreaField);

        selectCropAreaButton = new JButton("Select Area");
        selectCropAreaButton.setBounds(330, 330, 120, 30);
        add(selectCropAreaButton);

        startButton = new JButton("Start Testing");
        startButton.setBounds(180, 380, 200, 40);
        add(startButton);

        selectExcelButton.addActionListener(e -> selectFile(excelPathField));
        selectPicturesButton.addActionListener(e -> selectFolder(picturesFolderField));
        selectCroppedImagesButton.addActionListener(e -> selectFolder(croppedImagesFolderField));
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultsExcelField));
        selectCropAreaButton.addActionListener(e -> openImageSelectionWindow());

        startButton.addActionListener(e -> startTesting());
    }

    private void selectFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void selectFolder(JTextField textField) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(folderChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void selectFileSaveLocation(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openImageSelectionWindow() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage image = ImageIO.read(fileChooser.getSelectedFile());
                selectedArea = new Rectangle(0, 0, image.getWidth(), image.getHeight());
                cropAreaField.setText(selectedArea.x + ", " + selectedArea.y + ", " + selectedArea.width + ", " + selectedArea.height);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startTesting() {
        String excelPath = excelPathField.getText();
        String picturesFolder = picturesFolderField.getText();
        String croppedImagesFolder = croppedImagesFolderField.getText();
        String resultsExcelPath = resultsExcelField.getText();

        if (excelPath.isEmpty() || picturesFolder.isEmpty() || croppedImagesFolder.isEmpty() || resultsExcelPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all the fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Sample OCR Testing (Will be improved in the next step)
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata"); // Adjust the path if necessary

        try {
            File imageFile = new File(picturesFolder + "/sample.png"); // Test image
            BufferedImage image = ImageIO.read(imageFile);
            BufferedImage croppedImage = image.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);

            String extractedText = tesseract.doOCR(croppedImage);
            System.out.println("Extracted Text: " + extractedText);
        } catch (IOException | TesseractException e) {
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(this, "Testing Complete!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutomatedWarningTesting(null).setVisible(true));
    }
}
