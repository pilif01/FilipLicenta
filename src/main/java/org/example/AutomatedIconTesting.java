package org.example;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.awt.Rectangle;

public class AutomatedIconTesting extends JDialog {

    // automated icon testing dialog class for icon testing and crop area selection and excel integration
    private JTextField excelPathField, iconExcelField, refIconExcelField, cropAreaField, resultExcelField;
    // buttons for selecting files and performing actions
    private JButton startButton, selectExcelButton, selectIconExcelButton, selectRefIconExcelButton, selectResultsButton, selectCropAreaButton;
    private JButton singleTubeButton, twinTubeButton;
    // rectangle to store the selected crop area
    private Rectangle selectedArea;
    // buffered image to hold the reference icon image
    private BufferedImage referenceImage;
    // text area for logging messages
    private JTextArea logTextArea;
    // list to store warning icon data from excel
    private List<WarningIconData> iconData;

    // constructor to initialize the dialog and user interface
    public AutomatedIconTesting(JFrame parent) {
        super(parent, "Automated Icon Testing", true);

        // set dialog size layout and center on screen
        setSize(800, 650);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int labelWidth = 250;
        int textWidth = 350;
        int buttonWidth = 100;
        int y = 20;
        int gap = 40;

        // label and text field for selecting icon excel file
        JLabel selectExcelLabel = new JLabel("Select Icon Excel File:");
        selectExcelLabel.setBounds(20, y, labelWidth, 30);
        add(selectExcelLabel);

        iconExcelField = new JTextField();
        iconExcelField.setBounds(20, y + 30, textWidth, 30);
        add(iconExcelField);

        selectIconExcelButton = new JButton("Browse");
        selectIconExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectIconExcelButton);

        y += gap + 30;

        // label and text field for selecting reference icon excel file
        JLabel selectRefIconExcelLabel = new JLabel("Select Reference Icon Excel File:");
        selectRefIconExcelLabel.setBounds(20, y, labelWidth, 30);
        add(selectRefIconExcelLabel);

        refIconExcelField = new JTextField();
        refIconExcelField.setBounds(20, y + 30, textWidth, 30);
        add(refIconExcelField);

        selectRefIconExcelButton = new JButton("Browse");
        selectRefIconExcelButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectRefIconExcelButton);

        y += gap + 30;

        // label and text field and button for crop area selection
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

        // label and text field for results excel file save location
        JLabel selectResultsLabel = new JLabel("Select Results Excel Save Location:");
        selectResultsLabel.setBounds(20, y, labelWidth, 30);
        add(selectResultsLabel);

        resultExcelField = new JTextField();
        resultExcelField.setBounds(20, y + 30, textWidth, 30);
        add(resultExcelField);

        selectResultsButton = new JButton("Browse");
        selectResultsButton.setBounds(380, y + 30, buttonWidth, 30);
        add(selectResultsButton);

        y += gap + 30;

        // buttons for selecting reference images for single tube and twin tube
        singleTubeButton = new JButton("Single Tube");
        singleTubeButton.setBounds(20, y, 140, 30);
        add(singleTubeButton);

        twinTubeButton = new JButton("Twin Tube");
        twinTubeButton.setBounds(180, y, 140, 30);
        add(twinTubeButton);

        y += gap + 70;

        // button to start testing process
        startButton = new JButton("Start Testing");
        startButton.setBounds(200, y, 150, 40);
        add(startButton);

        // log text area at the bottom for status messages
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBounds(20, 550, 740, 80);
        add(logScrollPane);

        // add action listener for file selection for icon excel file
        selectIconExcelButton.addActionListener(e -> selectFile(iconExcelField));
        // add action listener for file selection for reference icon excel file
        selectRefIconExcelButton.addActionListener(e -> selectFile(refIconExcelField));
        // add action listener for selecting file save location for results excel file
        selectResultsButton.addActionListener(e -> selectFileSaveLocation(resultExcelField));

        // add action listener for crop area button to open crop dialog if reference image is loaded
        selectCropAreaButton.addActionListener(e -> {
            if (referenceImage != null) {
                openCropAreaDialog();
            } else {
                JOptionPane.showMessageDialog(this, "please select single or twin tube reference first", "error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // add action listener for single tube button to load reference image
        singleTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("SINGLE");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        // add action listener for twin tube button to load reference image
        twinTubeButton.addActionListener(e -> {
            try {
                loadReferenceImage("TWIN");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // add action listener for start button to begin testing
        startButton.addActionListener(e -> startTesting());
    }

    // utility method to append messages to the log text area
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> logTextArea.append(message + "\n"));
    }

    // method to open file chooser and set text field with selected file path
    private void selectFile(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // method to open file chooser for save location and set text field
    private void selectFileSaveLocation(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // method to load the reference image based on type and open crop dialog
    private void loadReferenceImage(String type) throws IOException {
        String basePath = "C:\\Licenta\\ExcelManager\\src\\main\\java\\org\\example\\pics";
        String imagePath = basePath + "\\" + "Reference_" + (type.equals("SINGLE") ? "SINGLE" : "TWIN") + ".png";
        referenceImage = ImageIO.read(new File(imagePath));
        openCropAreaDialog();
    }

    // method to open crop area dialog and set the selected crop area in text field
    private void openCropAreaDialog() {
        CropAreaDialog dialog = new CropAreaDialog(this, referenceImage);
        dialog.setVisible(true);
        Rectangle area = dialog.getSelectedArea();
        if (area != null) {
            selectedArea = area;
            cropAreaField.setText(area.x + " " + area.y + " " + area.width + " " + area.height);
        }
    }

    // main method to start the testing process using excel files and image comparison
    private void startTesting() {
        // get paths for icon excel file reference icon excel file and results excel file
        String iconExcelPath = iconExcelField.getText();
        String refIconExcelPath = refIconExcelField.getText();
        String resultsExcelPath = resultExcelField.getText().trim();

        try {
            // read icon excel file and store data in list
            FileInputStream iconFis = new FileInputStream(new File(iconExcelPath));
            XSSFWorkbook iconWorkbook = new XSSFWorkbook(iconFis);
            Sheet iconSheet = iconWorkbook.getSheetAt(0);

            iconData = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= iconSheet.getLastRowNum(); rowIndex++) {
                Row row = iconSheet.getRow(rowIndex);
                if (row != null) {
                    // get warning name icon name and control flag from excel row
                    String warningName = row.getCell(1).toString().trim();
                    String iconName = row.getCell(2).toString().trim();
                    String tControl = row.getCell(0).toString().trim();

                    // add the extracted data to the icon data list
                    iconData.add(new WarningIconData(warningName, iconName, tControl));
                }
            }

            // read reference icon excel file and get file paths for reference icons
            FileInputStream refFis = new FileInputStream(new File(refIconExcelPath));
            XSSFWorkbook refIconWorkbook = new XSSFWorkbook(refFis);
            Sheet refIconSheet = refIconWorkbook.getSheetAt(0);

            // process each warning and compare icon images
            for (WarningIconData data : iconData) {
                if ("RUN".equalsIgnoreCase(data.getTcontrol())) {
                    // find the matching reference icon based on icon name
                    BufferedImage referenceIcon = null;
                    for (int rowIndex = 1; rowIndex <= refIconSheet.getLastRowNum(); rowIndex++) {
                        Row row = refIconSheet.getRow(rowIndex);
                        if (row != null && data.getIconName().equals(row.getCell(1).toString().trim())) {
                            String referenceIconPath = "C:\\PathToReferenceIcons\\" + row.getCell(1).toString().trim() + ".png";
                            referenceIcon = ImageIO.read(new File(referenceIconPath));
                            break;
                        }
                    }

                    // if reference icon is not found log error and skip comparison
                    if (referenceIcon == null) {
                        appendLog("error reference icon for " + data.getIconName() + " not found");
                        continue;
                    }

                    // crop the selected area from the reference image
                    BufferedImage croppedImage = referenceImage.getSubimage(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);

                    // if cropped image is null log error and skip comparison
                    if (croppedImage == null) {
                        appendLog("error cropped image for warning " + data.getWarningName() + " is null");
                        continue;
                    }

                    // compare the cropped warning image with the reference icon image pixel by pixel
                    boolean match = compareImages(croppedImage, referenceIcon);

                    // log the result of the comparison
                    appendLog("warning " + data.getWarningName() + " icon match " + (match ? "passed" : "failed"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("error " + e.getMessage());
        }
    }

    // method to compare two images pixel by pixel and return true if they match
    private boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) {
            appendLog("error one or both images are null");
            return false;
        }

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            appendLog("error image sizes do not match");
            return false;
        }

        // iterate over each pixel and compare the rgb values
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    appendLog("mismatch found at " + x + " " + y);
                    return false;
                }
            }
        }

        return true;
    }

    // inner class to store warning icon data from excel
    class WarningIconData {
        private String warningName;
        private String iconName;
        private String tcontrol;

        public WarningIconData(String warningName, String iconName, String tcontrol) {
            this.warningName = warningName;
            this.iconName = iconName;
            this.tcontrol = tcontrol;
        }

        public String getWarningName() {
            return warningName;
        }

        public String getIconName() {
            return iconName;
        }

        public String getTcontrol() {
            return tcontrol;
        }
    }

    // inner class crop area dialog to select the crop area on the reference image
    class CropAreaDialog extends JDialog {
        private BufferedImage image;
        private CropPanel cropPanel;
        private Rectangle selectedArea;

        public CropAreaDialog(AutomatedIconTesting parent, BufferedImage image) {
            super(parent, "Select Crop Area", true);
            this.image = image;
            // set dialog size based on image dimensions with extra space
            setSize(image.getWidth() + 50, image.getHeight() + 100);
            setLayout(new BorderLayout());

            cropPanel = new CropPanel(image);
            add(cropPanel, BorderLayout.CENTER);

            JButton saveButton = new JButton("Save");
            add(saveButton, BorderLayout.SOUTH);

            // add listener to save the selected crop area and close the dialog
            saveButton.addActionListener(e -> {
                selectedArea = cropPanel.getSelection();
                dispose();
            });
        }

        public Rectangle getSelectedArea() {
            return selectedArea;
        }
    }

    // inner class crop panel to handle mouse events and draw the crop area
    class CropPanel extends JPanel {
        private BufferedImage image;
        private Rectangle selection;
        private Point startDrag, endDrag;

        public CropPanel(BufferedImage image) {
            this.image = image;
            // initialize selection to cover entire image
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

        // update the selection rectangle based on mouse drag
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
            // draw the image and the selection rectangle
            g.drawImage(image, 0, 0, null);
            g.setColor(Color.RED);
            ((Graphics2D) g).setStroke(new BasicStroke(2));
            g.drawRect(selection.x, selection.y, selection.width, selection.height);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }

    // main method to launch the dialog
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutomatedIconTesting(null).setVisible(true));
    }
}
