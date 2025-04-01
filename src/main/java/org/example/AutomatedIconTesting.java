package org.example;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// this class extends jdialog and serves as the main gui for automated icon testing
public class AutomatedIconTesting extends JDialog {

    // textfields for selecting various files and folders needed by the application
    private JTextField iconExcelField, downPicsField, refPicsField, croppedImagesFolderField, resultsExcelField, cropAreaField;
    // buttons for browsing files and folders and for actions like selecting the crop area and starting the test
    private JButton selectIconExcelButton, selectDownPicsButton, selectRefPicsButton, selectCroppedImagesButton, selectResultsButton, selectCropAreaButton;
    private JButton singleTubeButton, twinTubeButton, startButton;
    // rectangle representing the user-selected crop area (applied to both down and reference images)
    private Rectangle selectedArea;
    // buffered image for the reference image loaded via the single/twin tube button (used in cropping)
    private BufferedImage referenceImage;
    // log text area to display messages in the main dialog and in the log window
    private JTextArea logTextArea;
    // list to store warning data read from the excel file (each object contains the warning name and row index)
    private List<WarningIconData> iconData;

    // constructor that initializes the gui components and layouts
    public AutomatedIconTesting(JFrame parent) {
        super(parent, "Automated Icon Testing", true);
        setSize(800, 750);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int labelWidth = 250;
        int textWidth = 350;
        int buttonWidth = 100;
        int y = 20;
        int gap = 40;

        // --- icon excel file selection ---
        // label and textfield to allow user to select the excel file containing icon data
        JLabel iconExcelLabel = new JLabel("Select Icon Excel File:");
        iconExcelLabel.setBounds(20, y, labelWidth, 30);
        add(iconExcelLabel);

        iconExcelField = new JTextField();
        iconExcelField.setBounds(20, y + 30, textWidth, 30);
        add(iconExcelField);

        selectIconExcelButton = new JButton("Browse");
        selectIconExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectIconExcelButton);

        y += gap + 30;

        // --- down pics folder selection ---
        // components to select the folder that contains the down images (the images to be tested)
        JLabel downPicsLabel = new JLabel("Select Down Pics Folder:");
        downPicsLabel.setBounds(20, y, labelWidth, 30);
        add(downPicsLabel);

        downPicsField = new JTextField();
        downPicsField.setBounds(20, y + 30, textWidth, 30);
        add(downPicsField);

        selectDownPicsButton = new JButton("Browse");
        selectDownPicsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectDownPicsButton);

        y += gap + 30;

        // --- ref pics folder selection ---
        // components to allow user to select the folder containing the reference images
        JLabel refPicsLabel = new JLabel("Select Ref Pics Folder:");
        refPicsLabel.setBounds(20, y, labelWidth, 30);
        add(refPicsLabel);

        refPicsField = new JTextField();
        refPicsField.setBounds(20, y + 30, textWidth, 30);
        add(refPicsField);

        selectRefPicsButton = new JButton("Browse");
        selectRefPicsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectRefPicsButton);

        y += gap + 30;

        // --- cropped images folder selection ---
        // components to select the folder where the cropped images will be saved automatically during testing
        JLabel croppedImagesLabel = new JLabel("Select Cropped Images Folder:");
        croppedImagesLabel.setBounds(20, y, labelWidth, 30);
        add(croppedImagesLabel);

        croppedImagesFolderField = new JTextField();
        croppedImagesFolderField.setBounds(20, y + 30, textWidth, 30);
        add(croppedImagesFolderField);

        selectCroppedImagesButton = new JButton("Browse");
        selectCroppedImagesButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectCroppedImagesButton);

        y += gap + 30;

        // --- results excel file save location ---
        // components for selecting the output excel file where the test results will be saved
        JLabel resultsExcelLabel = new JLabel("Select Results Excel Save Location:");
        resultsExcelLabel.setBounds(20, y, labelWidth, 30);
        add(resultsExcelLabel);

        resultsExcelField = new JTextField();
        resultsExcelField.setBounds(20, y + 30, textWidth, 30);
        add(resultsExcelField);

        selectResultsButton = new JButton("Browse");
        selectResultsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectResultsButton);

        y += gap + 30;

        // --- crop area selection ---
        // components to select a specific crop area (x, y, width, height) for comparing images
        JLabel cropAreaLabel = new JLabel("Crop Area (x y width height):");
        cropAreaLabel.setBounds(20, y, labelWidth, 30);
        add(cropAreaLabel);

        cropAreaField = new JTextField();
        cropAreaField.setBounds(20, y + 30, 250, 30);
        add(cropAreaField);

        selectCropAreaButton = new JButton("Select Area");
        selectCropAreaButton.setBounds(280, y + 30, 120, 30);
        add(selectCropAreaButton);

        y += gap + 30;

        // --- buttons for reference image mode (single/twin) under crop area ---
        // these buttons allow the user to load the appropriate reference image from a fixed path
        singleTubeButton = new JButton("Single Tube");
        singleTubeButton.setBounds(20, y, 140, 30);
        add(singleTubeButton);

        twinTubeButton = new JButton("Twin Tube");
        twinTubeButton.setBounds(180, y, 140, 30);
        add(twinTubeButton);

        y += gap + 30;

        // --- start testing button ---
        // the button that initiates the testing process
        startButton = new JButton("Start Testing");
        startButton.setBounds(150, y, 150, 40);
        add(startButton);

        y += gap + 30;

        // --- log text area ---
        // a text area (within a scroll pane) to display log messages during processing
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBounds(20, y, 740, 150);
        add(logScrollPane);

        // --- action listeners for the components ---
        // listener for icon excel file selection
        selectIconExcelButton.addActionListener(e -> selectFile(iconExcelField));
        // listener for down pics folder selection
        selectDownPicsButton.addActionListener(e -> selectDirectory(downPicsField));
        // listener for ref pics folder selection
        selectRefPicsButton.addActionListener(e -> selectDirectory(refPicsField));
        // listener for cropped images folder selection
        selectCroppedImagesButton.addActionListener(e -> selectDirectory(croppedImagesFolderField));
        // listener for results excel file save location
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultsExcelField));

        // listener for crop area selection button; opens crop area dialog if a reference image is loaded
        selectCropAreaButton.addActionListener(e -> {
            if (referenceImage != null) {
                openCropAreaDialog();
            } else {
                JOptionPane.showMessageDialog(this, "please select single or twin tube reference first.", "error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // listener for single tube button; loads the reference image for single tube
        singleTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("SINGLE");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "error loading single tube reference image: " + ex.getMessage(), "error", JOptionPane.ERROR_MESSAGE);
            }
        });
        // listener for twin tube button; loads the reference image for twin tube
        twinTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("TWIN");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "error loading twin tube reference image: " + ex.getMessage(), "error", JOptionPane.ERROR_MESSAGE);
            }
        });
        // listener for start testing button; starts the testing process
        startButton.addActionListener(e -> startTesting());
    }

    // --- utility methods for file/folder selection ---
    // opens a file chooser and sets the text of the provided textfield to the chosen file's absolute path
    private void selectFile(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    // opens a directory chooser and sets the text of the provided textfield to the chosen folder's absolute path
    private void selectDirectory(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    // opens a file chooser for saving a file and sets the text of the provided textfield accordingly
    private void selectFileSaveLocation(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // --- loadreferenceimage method ---
    // this method always loads the reference image from the fixed path with filenames "reference_single.png" or "reference_twin.png"
    private void loadReferenceImage(String type) throws IOException {
        String basePath = "c:\\licenta\\excelmanager\\src\\main\\java\\org\\example\\pics";
        // construct the file name based on type; expects "Reference_SINGLE.png" or "Reference_TWIN.png"
        String imageFile = "Reference_" + (type.equals("SINGLE") ? "SINGLE" : "TWIN") + ".png";
        File file = new File(basePath + System.getProperty("file.separator") + imageFile);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "reference image " + imageFile + " not found in " + basePath, "error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // read the image from the file
        referenceImage = ImageIO.read(file);
        // open the crop area dialog to allow the user to select a crop region
        openCropAreaDialog();
    }

    // --- opencropareadialog method ---
    // this method creates and displays the crop area dialog, then sets the selectedArea based on user selection
    private void openCropAreaDialog() {
        CropAreaDialog dialog = new CropAreaDialog(this, referenceImage);
        dialog.setVisible(true);
        Rectangle area = dialog.getSelectedArea();
        if (area != null) {
            selectedArea = area;
            cropAreaField.setText(area.x + " " + area.y + " " + area.width + " " + area.height);
        }
    }

    // --- starttesting method ---
    // this method reads the excel file for warning names, then for each warning:
    // - it builds file paths for the down pic and reference pic (both expected to have the same name)
    // - it crops the images based on the selected area, saves the cropped down image, compares the two cropped images pixel-by-pixel,
    // - and writes the result ("correct", "incorrect", or "pic not found") into column 4 (cell index 3) of the final excel.
    private void startTesting() {
        LogWindow logWindow = new LogWindow(this);
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                String iconExcelPath = iconExcelField.getText().trim();
                String downPicsFolder = downPicsField.getText().trim();
                String refPicsFolder = refPicsField.getText().trim();
                String resultsExcelPath = resultsExcelField.getText().trim();
                String croppedFolder = croppedImagesFolderField.getText().trim();
                if (iconExcelPath.isEmpty() || downPicsFolder.isEmpty() || refPicsFolder.isEmpty() || resultsExcelPath.isEmpty() || croppedFolder.isEmpty() || selectedArea == null) {
                    publish("please ensure all selections are made and crop area is set.");
                    return null;
                }
                try (FileInputStream fis = new FileInputStream(new File(iconExcelPath));
                     XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    iconData = new ArrayList<>();
                    // read only the warning name from column 0 (assuming header is in row 0)
                    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null && row.getCell(0) != null) {
                            String warningName = row.getCell(0).toString().trim();
                            iconData.add(new WarningIconData(warningName, rowIndex));
                        }
                    }

                    // process each warning: compare the down pic with the reference pic (both are expected to have the same name)
                    for (WarningIconData data : iconData) {
                        String warningName = data.getWarningName();
                        // build file paths for the down pic and the reference pic
                        String downPicPath = downPicsFolder + System.getProperty("file.separator") + warningName + ".png";
                        String refPicPath = refPicsFolder + System.getProperty("file.separator") + warningName + ".png";
                        File downFile = new File(downPicPath);
                        File refFile = new File(refPicPath);
                        if (!downFile.exists() || !refFile.exists()) {
                            data.setResult("pic not found");
                            publish("warning: " + warningName + " -> pic not found");
                            continue;
                        }
                        BufferedImage downImage = ImageIO.read(downFile);
                        BufferedImage refImage = ImageIO.read(refFile);
                        // optionally crop both images using the selected area
                        BufferedImage croppedDown = downImage.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
                        BufferedImage croppedRef = refImage.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
                        // save the cropped down image automatically
                        String croppedImageName = warningName + "_crop.png";
                        File output = new File(croppedFolder + System.getProperty("file.separator") + croppedImageName);
                        ImageIO.write(croppedDown, "png", output);
                        publish("cropped image saved for warning: " + warningName);
                        // perform pixel-by-pixel comparison of the cropped images
                        boolean match = compareImages(croppedDown, croppedRef);
                        String resultStr = match ? "correct" : "incorrect";
                        data.setResult(resultStr);
                        publish("warning: " + warningName + " -> " + resultStr);
                    }

                    // write the results into the excel file in column 4 (cell index 3)
                    Row headerRow = sheet.getRow(0);
                    // create header for the result column if desired
                    Cell resultHeaderCell = headerRow.createCell(3);
                    resultHeaderCell.setCellValue("result");
                    for (WarningIconData data : iconData) {
                        Row row = sheet.getRow(data.getRowIndex());
                        if (row != null) {
                            Cell cell = row.createCell(3);
                            cell.setCellValue(data.getResult());
                        }
                    }
                    try (FileOutputStream fos = new FileOutputStream(new File(resultsExcelPath))) {
                        workbook.write(fos);
                    }
                    publish("results written to " + resultsExcelPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("error: " + e.getMessage());
                }
                return null;
            }
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    logWindow.appendLog(msg);
                }
            }
        };
        worker.execute();
    }

    // --- compareimages method ---
    // this method compares two buffered images pixel-by-pixel; returns true if they match exactly, false otherwise
    private boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) return false;
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) return false;
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- warningicondata inner class ---
    // this class serves as a simple data container for each warning; it stores the warning name, the row index in the excel file, and the test result
    class WarningIconData {
        private String warningName;
        private String result;
        private int rowIndex;
        public WarningIconData(String warningName, int rowIndex) {
            this.warningName = warningName;
            this.rowIndex = rowIndex;
        }
        public String getWarningName() { return warningName; }
        public int getRowIndex() { return rowIndex; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }

    // --- logwindow inner class ---
    // this class creates a separate dialog window to display log messages during processing; it is updated via the swingworker process method
    class LogWindow extends JDialog {
        private JTextArea logTextArea;
        public LogWindow(AutomatedIconTesting parent) {
            super(parent, "testing log", false);
            setSize(600, 400);
            setLocationRelativeTo(parent);
            logTextArea = new JTextArea();
            logTextArea.setEditable(false);
            logTextArea.setFont(new Font("aptos narrow", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(logTextArea);
            add(scrollPane);
            setVisible(true);
        }
        public void appendLog(String message) {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
    }

    // --- cropareadialog inner class ---
    // this dialog allows the user to select a crop area on the reference image; the selected area is returned to the main class
    class CropAreaDialog extends JDialog {
        private BufferedImage image;
        private CropPanel cropPanel;
        private Rectangle selectedArea;
        public CropAreaDialog(AutomatedIconTesting parent, BufferedImage image) {
            super(parent, "select crop area", true);
            this.image = image;
            setSize(image.getWidth() + 50, image.getHeight() + 100);
            setLayout(new BorderLayout());
            cropPanel = new CropPanel(image);
            add(cropPanel, BorderLayout.CENTER);
            JButton saveButton = new JButton("save");
            add(saveButton, BorderLayout.SOUTH);
            saveButton.addActionListener(e -> {
                selectedArea = cropPanel.getSelection();
                dispose();
            });
        }
        public Rectangle getSelectedArea() {
            return selectedArea;
        }
    }

    // --- croppanel inner class ---
    // this panel handles mouse events to allow the user to draw a rectangle (the crop area) over the image
    class CropPanel extends JPanel {
        private BufferedImage image;
        private Rectangle selection;
        private Point startDrag, endDrag;
        public CropPanel(BufferedImage image) {
            this.image = image;
            // initially, the selection covers the whole image
            selection = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startDrag = e.getPoint();
                    endDrag = startDrag;
                    repaint();
                }
                @Override
                public void mouseDragged(MouseEvent e) {
                    endDrag = e.getPoint();
                    updateSelection();
                    repaint();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    endDrag = e.getPoint();
                    updateSelection();
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }
        // updates the selection rectangle based on the current mouse drag positions
        private void updateSelection() {
            int x = Math.min(startDrag.x, endDrag.x);
            int y = Math.min(startDrag.y, endDrag.y);
            int width = Math.abs(startDrag.x - endDrag.x);
            int height = Math.abs(startDrag.y - endDrag.y);
            selection = new Rectangle(x, y, width, height);
        }
        public Rectangle getSelection() {
            return selection;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // draw the image
            g.drawImage(image, 0, 0, null);
            // set color and stroke for drawing the selection rectangle
            g.setColor(Color.RED);
            ((Graphics2D) g).setStroke(new BasicStroke(2));
            g.drawRect(selection.x, selection.y, selection.width, selection.height);
        }
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }

    // main method to launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutomatedIconTesting(null).setVisible(true));
    }
}
