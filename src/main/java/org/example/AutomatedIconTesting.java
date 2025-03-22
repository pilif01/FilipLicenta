package org.example;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AutomatedIconTesting {

    private static JProgressBar progressBar;
    private static JLabel statusLabel;
    private static Workbook workbook;
    private static Sheet sheet;
    private static int totalTests;

    private static String excelFilePath = "";
    private static String warningFolder = "";
    private static String refFolder = "";

    private static JLabel excelLabel;
    private static JLabel warningLabel;
    private static JLabel refIconLabel;

    // initialise excel reading
    public static void initializeExcel(String excelFilePath) throws IOException {
        FileInputStream fis = new FileInputStream(new File(excelFilePath));
        workbook = new XSSFWorkbook(fis);
        sheet = workbook.getSheetAt(0);
    }

    // get warning details
    public static String[] getWarningDetails(int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        String warningName = row.getCell(1).getStringCellValue();
        String iconName = row.getCell(2).getStringCellValue();
        return new String[]{warningName, iconName};
    }

    // save the result
    public static void setTestResult(int rowIndex, String result) throws IOException {
        Row row = sheet.getRow(rowIndex);
        row.createCell(3).setCellValue(result);
        FileOutputStream fos = new FileOutputStream(new File("updated_excel.xlsx"));
        workbook.write(fos);
        fos.close();
    }

    // compare images
    public static boolean compareIcons(String warningImagePath, String referenceIconPath) throws IOException {
        BufferedImage warningImage = ImageIO.read(new File(warningImagePath));
        BufferedImage referenceIcon = ImageIO.read(new File(referenceIconPath));

        // possition of the icon
        BufferedImage croppedIcon = warningImage.getSubimage(500, 100, 100, 100); // coordinates of the icon

        return comparePixelByPixel(croppedIcon, referenceIcon);
    }

    private static boolean comparePixelByPixel(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }

        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ui and progress bar
    public static void createUI() {
        JFrame frame = new JFrame("Icon Testing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new FlowLayout());

        // buttons
        JButton selectExcelButton = new JButton("Select Excel File");
        JButton selectWarningButton = new JButton("Select Warning Image Folder");
        JButton selectRefButton = new JButton("Select Ref Icon Folder");

        // lables
        excelLabel = new JLabel("No file selected");
        warningLabel = new JLabel("No folder selected");
        refIconLabel = new JLabel("No folder selected");

        // start testing button that only works when files are selected
        JButton startTestButton = new JButton("Start Testing");
        startTestButton.setEnabled(false);
        startTestButton.add(Box.createVerticalStrut(100));
        startTestButton.setPreferredSize(new Dimension(250, 120));

        selectExcelButton.addActionListener(e -> {
            excelFilePath = chooseFile("Select Excel File", "xlsx");
            if (excelFilePath != null) {
                excelLabel.setText("Selected: " + excelFilePath);
                enableStartTestingButton(startTestButton);
            }
        });

        selectWarningButton.addActionListener(e -> {
            warningFolder = chooseDirectory("Select Warning Images Folder");
            if (warningFolder != null) {
                warningLabel.setText("Selected: " + warningFolder);
                enableStartTestingButton(startTestButton);
            }
        });

        selectRefButton.addActionListener(e -> {
            refFolder = chooseDirectory("Select Reference Icons Folder");
            if (refFolder != null) {
                refIconLabel.setText("Selected: " + refFolder);
                enableStartTestingButton(startTestButton);
            }
        });

        // Start testing button action
        startTestButton.addActionListener(e -> {
            try {
                createProgressUI();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        // add components
        frame.add(selectExcelButton);
        frame.add(excelLabel);
        frame.add(selectWarningButton);
        frame.add(warningLabel);
        frame.add(selectRefButton);
        frame.add(refIconLabel);
        frame.add(startTestButton);

        frame.setVisible(true);
    }

    // enable the start testing button when all selections are made
    private static void enableStartTestingButton(JButton startTestButton) {
        if (!excelFilePath.isEmpty() && !warningFolder.isEmpty() && !refFolder.isEmpty()) {
            startTestButton.setEnabled(true);
        }
    }

    // create the progress ui
    private static void createProgressUI() throws IOException {
        JFrame progressFrame = new JFrame("Testing Progress");
        progressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        progressFrame.setSize(400, 150);
        progressFrame.setLayout(new BorderLayout());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Starting tests...");

        progressFrame.add(progressBar, BorderLayout.NORTH);
        progressFrame.add(statusLabel, BorderLayout.CENTER);

        progressFrame.setVisible(true);

        // initialize excel and start testing
        initializeExcel(excelFilePath);
        totalTests = sheet.getPhysicalNumberOfRows();
        testIcons();
    }

    // test icons based on the data in the excel file
    private static void testIcons() {
        new Thread(() -> {
            try {
                for (int i = 1; i < totalTests; i++) {
                    String[] details = getWarningDetails(i);
                    String warningName = details[0];
                    String iconName = details[1];

                    String warningImagePath = warningFolder + File.separator + warningName + ".png";
                    String referenceIconPath = refFolder + File.separator + iconName + ".png";

                    boolean isIconMatch = compareIcons(warningImagePath, referenceIconPath);

                    String result = isIconMatch ? "PASS" : "FAIL";
                    setTestResult(i, result);

                    // Update progress bar and status
                    int progress = (int) ((i / (double) totalTests) * 100);
                    progressBar.setValue(progress);
                    statusLabel.setText("Testing: " + warningName);
                }
                JOptionPane.showMessageDialog(null, "Testing complete!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String chooseFile(String dialogTitle, String fileExtension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(fileExtension, fileExtension));
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public static String chooseDirectory(String dialogTitle) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public static void main(String[] args) {
        createUI();
    }
}
