package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoordinateMaker extends JFrame {

    private String directoryPath; // Path to the image folder
    private String excelFilePath; // Path to the Excel file

    private JFrame mainFrame;
    private ImagePanel imagePanel;
    private JList<ImageData> rowsList; // Changed to JList<ImageData> for better data handling
    private DefaultListModel<ImageData> listModel;

    private JLabel imageNameDisplayLabel;
    private JLabel textDisplayLabel;
    private JLabel coordinatesDisplayLabel;
    private JLabel currentStatusLabel; // To show OK / - status

    private JButton selectExcelButton;
    private JButton selectImageFolderButton;
    private JButton logCoordinatesButton;
    private JButton nextButton;
    private JButton backButton;

    private BufferedImage currentImage;
    private Rectangle currentSelection;
    private Point startPoint;
    private int currentIndex = 0;

    private List<ImageData> imageDataSet;
    private ImageData currentImageData; // Track the current image data


    public CoordinateMaker() {
        mainFrame = new JFrame("Image Coordinate Selector");
        // --- MODIFICATION START ---
        // Change from JFrame.EXIT_ON_CLOSE to JFrame.DISPOSE_ON_CLOSE
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose only this window
        // --- MODIFICATION END ---
        mainFrame.setSize(1000, 750); // Increased size for better layout
        mainFrame.setLocationRelativeTo(null); // Center the window

        setupUI();
        setupListeners();

        mainFrame.setVisible(true);
    }

    private void setupUI() {
        // Main panel with BorderLayout for left and right sections
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10)); // Add spacing
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // --- Left Panel ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(250, mainFrame.getHeight())); // Fixed width for left panel

        // File selection buttons at the top of the left panel
        JPanel fileSelectionButtonsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        selectExcelButton = new JButton("Select Excel File");
        selectImageFolderButton = new JButton("Select Image Folder");
        fileSelectionButtonsPanel.add(selectExcelButton);
        fileSelectionButtonsPanel.add(selectImageFolderButton);
        leftPanel.add(fileSelectionButtonsPanel, BorderLayout.NORTH);

        // JList for image data
        listModel = new DefaultListModel<>();
        rowsList = new JList<>(listModel);
        rowsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rowsList.setFont(new Font("Arial", Font.PLAIN, 16));
        // Custom renderer for JList to show OK/NOK status
        rowsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ImageData) {
                    ImageData data = (ImageData) value;
                    label.setText(data.imageName); // Display only image name in list
                    if (data.isCoordinatesSet()) {
                        label.setForeground(Color.BLUE); // Mark as blue if coordinates set
                    } else {
                        label.setForeground(Color.BLACK);
                    }
                }
                return label;
            }
        });
        JScrollPane listScrollPane = new JScrollPane(rowsList);
        leftPanel.add(listScrollPane, BorderLayout.CENTER);
        contentPanel.add(leftPanel, BorderLayout.WEST);

        // --- Right Panel ---
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        // Info Panel at the top of the right panel
        JPanel infoPanel = new JPanel(new GridLayout(4, 1));
        imageNameDisplayLabel = new JLabel("Image Name: ");
        textDisplayLabel = new JLabel("Text: ");
        coordinatesDisplayLabel = new JLabel("Coordinates: ");
        currentStatusLabel = new JLabel("Status: -");

        Font infoFont = new Font("SansSerif", Font.BOLD, 16);
        imageNameDisplayLabel.setFont(infoFont);
        textDisplayLabel.setFont(infoFont);
        coordinatesDisplayLabel.setFont(infoFont);
        currentStatusLabel.setFont(infoFont);

        infoPanel.add(imageNameDisplayLabel);
        infoPanel.add(textDisplayLabel);
        infoPanel.add(coordinatesDisplayLabel);
        infoPanel.add(currentStatusLabel);
        rightPanel.add(infoPanel, BorderLayout.NORTH);

        // Image Panel in the center
        imagePanel = new ImagePanel();
        JScrollPane imageScrollPane = new JScrollPane(imagePanel);
        rightPanel.add(imageScrollPane, BorderLayout.CENTER);

        // Control Buttons Panel at the bottom of the right panel
        JPanel controlButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        backButton = new JButton("Previous");
        logCoordinatesButton = new JButton("Save Coordinates");
        nextButton = new JButton("Next");

        controlButtonsPanel.add(backButton);
        controlButtonsPanel.add(logCoordinatesButton);
        controlButtonsPanel.add(nextButton);
        rightPanel.add(controlButtonsPanel, BorderLayout.SOUTH);

        contentPanel.add(rightPanel, BorderLayout.CENTER);
        mainFrame.add(contentPanel);
    }

    private void setupListeners() {
        selectExcelButton.addActionListener(e -> selectExcelFile());
        selectImageFolderButton.addActionListener(e -> selectImageFolder());

        rowsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentIndex = rowsList.getSelectedIndex();
                if (currentIndex >= 0) {
                    loadImageDataAndDisplay(currentIndex);
                }
            }
        });

        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                currentSelection = new Rectangle(startPoint);
                imagePanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                startPoint = null;
                // Update coordinates display immediately after selection is complete
                if (currentSelection != null) {
                    coordinatesDisplayLabel.setText(String.format("Coordinates: (%d, %d, %d, %d)",
                            currentSelection.x, currentSelection.y,
                            currentSelection.x + currentSelection.width, currentSelection.y + currentSelection.height));
                }
            }
        });

        imagePanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint != null) {
                    Point currentPoint = e.getPoint();
                    currentSelection.setBounds(
                            Math.min(startPoint.x, currentPoint.x),
                            Math.min(startPoint.y, currentPoint.y),
                            Math.abs(startPoint.x - currentPoint.x),
                            Math.abs(startPoint.y - currentPoint.y)
                    );
                    imagePanel.repaint();
                }
            }
        });

        logCoordinatesButton.addActionListener(e -> logCoordinates());
        nextButton.addActionListener(e -> loadNextImage());
        backButton.addActionListener(e -> loadPreviousImage());
    }

    private void selectExcelFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Excel File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));

        int userSelection = fileChooser.showOpenDialog(mainFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            excelFilePath = selectedFile.getAbsolutePath();
            loadExcelData();
        }
    }

    private void selectImageFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Select Image Directory");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int userSelection = folderChooser.showOpenDialog(mainFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = folderChooser.getSelectedFile();
            directoryPath = selectedDirectory.getAbsolutePath();
            // If Excel data is already loaded, try to load images
            if (imageDataSet != null && !imageDataSet.isEmpty()) {
                loadImageDataAndDisplay(currentIndex);
            }
        }
    }

    private void loadExcelData() {
        imageDataSet = new ArrayList<>();
        listModel.clear();

        try (FileInputStream fileInputStream = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            // Assuming the sheet name is "DE" as per the original code
            Sheet sheet = workbook.getSheet("DE");

            if (sheet == null) {
                JOptionPane.showMessageDialog(mainFrame, "Sheet 'DE' not found in the Excel file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DataFormatter formatter = new DataFormatter();

            // Loop from row 1 (index 1) as row 0 is header.
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row != null) {
                    String textResourceId = formatter.formatCellValue(row.getCell(1)); // Column B (index 1)
                    String photoName = formatter.formatCellValue(row.getCell(2)); // Column C (index 2)
                    String languageText = formatter.formatCellValue(row.getCell(4)); // Column E (index 4 for DE text)

                    int excelRowIndex = i; // Store the actual 0-based Excel row index

                    int x1 = 0, y1 = 0, x2 = 0, y2 = 0;

                    // Read coordinate values from new columns: D, E, F, G (indices 3, 4, 5, 6)
                    Cell cellX1 = row.getCell(3); // Column D
                    Cell cellY1 = row.getCell(4); // Column E
                    Cell cellX2 = row.getCell(5); // Column F
                    Cell cellY2 = row.getCell(6); // Column G

                    x1 = getNumericCellValue(cellX1);
                    y1 = getNumericCellValue(cellY1);
                    x2 = getNumericCellValue(cellX2);
                    y2 = getNumericCellValue(cellY2);

                    // Using photoName as imageName, and languageText as reference for display
                    ImageData data = new ImageData(photoName, languageText, excelRowIndex, x1, y1, x2, y2);
                    imageDataSet.add(data);
                    listModel.addElement(data);
                }
            }
            if (!imageDataSet.isEmpty()) {
                rowsList.setSelectedIndex(0); // Select the first item
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Error reading Excel file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "An unexpected error occurred while reading Excel: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private int getNumericCellValue(Cell cell) {
        if (cell == null) return 0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0; // Return 0 if string is not a valid number
                }
            default:
                return 0;
        }
    }

    private void loadImageDataAndDisplay(int index) {
        if (imageDataSet == null || imageDataSet.isEmpty() || index < 0 || index >= imageDataSet.size()) {
            clearDisplay();
            return;
        }

        currentImageData = imageDataSet.get(index);
        currentSelection = null; // Clear previous drawing selection

        // Set image name, text, and coordinates labels
        imageNameDisplayLabel.setText("Image Name: " + currentImageData.imageName);
        textDisplayLabel.setText("Text: " + currentImageData.reference); // 'reference' now holds the language-specific text
        coordinatesDisplayLabel.setText(String.format("Coordinates: (%d, %d, %d, %d)",
                currentImageData.x1, currentImageData.y1,
                currentImageData.x2, currentImageData.y2));
        currentStatusLabel.setText("Status: " + (currentImageData.isCoordinatesSet() ? "OK" : "-"));


        // Load image if directory path is set
        if (directoryPath != null && !directoryPath.isEmpty()) {
            try {
                // Assuming image names are photoName_de.png as per the original structure
                File imageFile = new File(directoryPath + File.separator + currentImageData.imageName + "_de.png");
                if (imageFile.exists()) {
                    currentImage = javax.imageio.ImageIO.read(imageFile);
                    imagePanel.setPreferredSize(new Dimension(currentImage.getWidth(), currentImage.getHeight()));
                    imagePanel.revalidate();
                    imagePanel.repaint();
                } else {
                    currentImage = null;
                    imagePanel.repaint();
                    JOptionPane.showMessageDialog(mainFrame, "Image not found: " + imageFile.getName(), "Image Load Error", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException ex) {
                currentImage = null;
                imagePanel.repaint();
                JOptionPane.showMessageDialog(mainFrame, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            currentImage = null;
            imagePanel.repaint();
        }
    }

    private void clearDisplay() {
        imageNameDisplayLabel.setText("Image Name: ");
        textDisplayLabel.setText("Text: ");
        coordinatesDisplayLabel.setText("Coordinates: ");
        currentStatusLabel.setText("Status: -");
        currentImage = null;
        imagePanel.repaint();
    }

    private void loadNextImage() {
        if (currentIndex < listModel.getSize() - 1) {
            currentIndex++;
            rowsList.setSelectedIndex(currentIndex);
            // loadImageDataAndDisplay is called by the ListSelectionListener
        }
    }

    private void loadPreviousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            rowsList.setSelectedIndex(currentIndex);
            // loadImageDataAndDisplay is called by the ListSelectionListener
        }
    }

    private void logCoordinates() {
        if (currentSelection == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an area on the image first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentImageData == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image data loaded to log coordinates for.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (excelFilePath == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an Excel file first.", "No Excel", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int x1 = currentSelection.x;
        int y1 = currentSelection.y;
        int x2 = currentSelection.x + currentSelection.width;
        int y2 = currentSelection.y + currentSelection.height;

        try (FileInputStream fileInputStream = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            // --- Update the "DE" sheet ---
            Sheet deSheet = workbook.getSheet("DE");
            if (deSheet == null) {
                JOptionPane.showMessageDialog(mainFrame, "Sheet 'DE' not found in Excel. Cannot log coordinates.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Row deRow = deSheet.getRow(currentImageData.excelRowIndex);
            if (deRow == null) {
                deRow = deSheet.createRow(currentImageData.excelRowIndex);
            }

            // Update coordinate cells in DE sheet to D, E, F, G (indices 3, 4, 5, 6)
            deRow.createCell(3).setCellValue(x1); // Column D
            deRow.createCell(4).setCellValue(y1); // Column E
            deRow.createCell(5).setCellValue(x2); // Column F
            deRow.createCell(6).setCellValue(y2); // Column G

            // --- Update all other language sheets ---
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                String sheetName = workbook.getSheetName(s);
                // Skip the "DE" sheet and "TControl" sheet
                if (!sheetName.equals("DE") && !sheetName.equals("TControl")) {
                    Sheet languageSheet = workbook.getSheet(sheetName);
                    if (languageSheet != null) { // Ensure sheet exists
                        Row targetRow = languageSheet.getRow(currentImageData.excelRowIndex);
                        if (targetRow == null) {
                            targetRow = languageSheet.createRow(currentImageData.excelRowIndex);
                        }
                        // Write the same coordinates to columns D, E, F, G
                        targetRow.createCell(3).setCellValue(x1); // Column D
                        targetRow.createCell(4).setCellValue(y1); // Column E
                        targetRow.createCell(5).setCellValue(x2); // Column F
                        targetRow.createCell(6).setCellValue(y2); // Column G
                    }
                }
            }

            // --- Save the workbook ---
            try (FileOutputStream fos = new FileOutputStream(excelFilePath)) {
                workbook.write(fos);
            }

            // Update in-memory ImageData object and UI
            currentImageData.x1 = x1;
            currentImageData.y1 = y1;
            currentImageData.x2 = x2;
            currentImageData.y2 = y2;
            currentImageData.setCoordinatesSet(true);

            coordinatesDisplayLabel.setText(String.format("Coordinates: (%d, %d, %d, %d)", x1, y1, x2, y2));
            currentStatusLabel.setText("Status: OK");

            // Refresh the list model to update the color/status
            listModel.setElementAt(currentImageData, currentIndex);
            rowsList.repaint(); // Repaint JList to reflect changes

            currentSelection = null; // Clear the temporary drawing selection
            imagePanel.repaint(); // Redraw image to remove temp selection
            JOptionPane.showMessageDialog(mainFrame, "Coordinates logged successfully to all relevant sheets!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving coordinates to Excel: " + e.getMessage() +
                    "\nEnsure the Excel file is not open in another program.", "File Access Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "An unexpected error occurred while logging coordinates: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CoordinateMaker());
    }

    class ImagePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (currentImage != null) {
                g.drawImage(currentImage, 0, 0, null);
            }
            if (currentImageData != null && currentImageData.isCoordinatesSet()) {
                // Draw previously saved selection in blue
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(currentImageData.x1, currentImageData.y1,
                        currentImageData.x2 - currentImageData.x1,
                        currentImageData.y2 - currentImageData.y1);
            }
            if (currentSelection != null) {
                // Draw new selection in red
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.draw(currentSelection);
            }
        }
    }

    // Inner class to store image and coordinate data
    class ImageData {
        String imageName; // Corresponds to "Photo name" (Column C/index 2)
        String reference; // This will hold the language-specific "Text" (Column E/index 4 for DE sheet)
        int excelRowIndex; // Actual 0-based row index in Excel for saving
        boolean coordinatesSet; // Indicates if coordinates have been set for this entry
        int x1, y1, x2, y2;

        public ImageData(String imageName, String reference, int excelRowIndex, int x1, int y1, int x2, int y2) {
            this.imageName = imageName;
            this.reference = reference;
            this.excelRowIndex = excelRowIndex;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            // A coordinate is considered "set" if any of its values are non-zero
            // Assuming default/empty coordinates are 0,0,0,0
            this.coordinatesSet = (x1 != 0 || y1 != 0 || x2 != 0 || y2 != 0);
        }

        public boolean isCoordinatesSet() {
            return coordinatesSet;
        }

        public void setCoordinatesSet(boolean coordinatesSet) {
            this.coordinatesSet = coordinatesSet;
        }

        @Override
        public String toString() {
            // This is primarily for the JList display, but the JList renderer overrides it.
            // Still good for debugging if needed.
            return String.format("Image: %s, Coords: (%d,%d,%d,%d), Status: %s",
                    imageName, x1, y1, x2, y2, coordinatesSet ? "OK" : "-");
        }
    }
}