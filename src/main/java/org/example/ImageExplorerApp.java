package org.example;

// bread and butter of testare manuala

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.RowData.getIntValueFromCell;
import static org.example.RowData.getStringValueFromCell;
class ImageLabel extends JLabel {
    private int xLeft, yTop, xRight, yBottom;

    public void setRectangleBounds(int xLeft, int yTop, int xRight, int yBottom) {
        this.xLeft = xLeft;
        this.yTop = yTop;
        this.xRight = xRight;
        this.yBottom = yBottom;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.RED);
        g.drawRect(xLeft, yTop, xRight - xLeft, yBottom - yTop);
        System.out.println(xLeft+""+yTop+""+yBottom+""+xRight);
    }
}

public class ImageExplorerApp {

    private static JFrame frame;
    private static JPanel mainPanel;
    private static JComboBox<String> sheetSelector;
    private static JList<String> fileList;
    private static ImageLabel mainImageLabel;
    private static JTextArea errorTextArea;
    private static JButton saveButton;
    private static JButton nextButton;
    private static JButton customizeColumnsButton;
    private static String baseImagePath = "";
    private static String baseExcelPath = "";
    private static JLabel label5;
    private static JLabel label6;
    private static JLabel label7;
    private static JButton selectExcelButton;
    private static JButton selectImageButton;
    private static JButton selectBasePathButton;


    private static List<RowData> validRows;


    private static String selectedSheet = "";
    private static int currentRow;

    private static String selectedImageType;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(() -> createAndShowGUI());
            }
        });

    }



    private static void createAndShowGUI() {
        frame = new JFrame("Image Explorer App");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        selectedSheet = "";
        baseExcelPath="";
        mainPanel = new JPanel(new BorderLayout());

        // UI Components
        sheetSelector = new JComboBox<>();
        sheetSelector.addItem("Please to start testing: Configure columns, load Excel, select image type, and select image path");

        fileList = new JList<>();
        mainImageLabel = new ImageLabel();
        errorTextArea = new JTextArea(5, 20);
        saveButton = new JButton("Save");
        nextButton = new JButton("Next");
        selectExcelButton = new JButton("Select Excel");
        selectImageButton = new JButton("Select Type (RHS/LHS/CENTER)");

        // Initialize UI components and set up the layout

        // Add action listener for sheet selector
        sheetSelector.addActionListener(e -> {
            selectedSheet = (String) sheetSelector.getSelectedItem();
            if (selectedSheet != null && !selectedSheet.equals("Select a sheet")) {
                loadDataFromXLSX(baseExcelPath,selectedSheet);
                updateFileList();
            }
        });

        // Add action listener for file selection
        selectExcelButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                selectedSheet = null; // Reset selected sheet
                updateSheetSelector(selectedFile.getAbsolutePath());
                baseExcelPath=selectedFile.getAbsolutePath();
            }
        });





        // Add action listener for image type selection
        selectImageButton.addActionListener(e -> {
            String[] imageTypes = {"CENTER_", "RHS_", "LHS_"};
            selectedImageType = (String) JOptionPane.showInputDialog(frame,
                    "Select Image Type", "Image Type",
                    JOptionPane.QUESTION_MESSAGE, null, imageTypes, imageTypes[0]);
            if (selectedImageType != null) {
                updateFileList(); // Update the file list based on the new image type
            }
        });

        selectBasePathButton = new JButton("Select Image Path Location");

        selectBasePathButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showDialog(frame, "Select");
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = folderChooser.getSelectedFile();
                // Set the selected folder path as the base image path
                setBaseImagePath(selectedFolder.getAbsolutePath());
                // Update the file list
                updateFileList();
            }
        });

// Add the selectBasePathButton to the button panel in createLeftPanel method



        fileList.addListSelectionListener(e -> {
            currentRow = fileList.getSelectedIndex();
            displayRow(currentRow);
        });

        saveButton.addActionListener(e -> {
            saveData();
        });

        nextButton.addActionListener(e -> {
            if (currentRow < validRows.size() - 1) {
                currentRow++;
                displayRow(currentRow);
            }
        });

        // Add components to main panel
        mainPanel.add(createLeftPanel(), BorderLayout.SOUTH);
        mainPanel.add(createRightPanel(), BorderLayout.CENTER);

        // Add main panel to the frame
        frame.add(mainPanel);


        // Set frame properties and make it visible
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void setBaseImagePath(String path) {
        baseImagePath = path;
    }


    private static void updateSheetSelector(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            sheetSelector.removeAllItems();
            sheetSelector.addItem("Select a sheet");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetSelector.addItem(workbook.getSheetName(i));
            }
            workbook.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleButtonClick(String buttonName) {
        if (currentRow >= 0 && currentRow < validRows.size()) {
            RowData rowData = validRows.get(currentRow);
            String userInput = errorTextArea.getText() + "logged";

            try (FileInputStream fis = new FileInputStream(new File(baseExcelPath));
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

                XSSFSheet sheet = workbook.getSheet(selectedSheet);
                XSSFRow row = sheet.getRow(currentRow + 2);

                // Update cells in the row with new data
                row.createCell(11).setCellValue(userInput);

                // Log the result in the corresponding column
                int resultColumn = 12; // Assuming the result column is at index 12 (adjust if needed)
                switch (buttonName.toLowerCase()) {
                    case "text error":
                        row.createCell(resultColumn).setCellValue("Text Error");
                        rowData.setRowStatus("Text Error"); // Set the row status
                        break;
                    case "truncation":
                        row.createCell(resultColumn).setCellValue("Truncation");
                        rowData.setRowStatus("Truncation"); // Set the row status
                        break;
                    case "strange":
                        row.createCell(resultColumn).setCellValue("Strange");
                        rowData.setRowStatus("Strange"); // Set the row status
                        break;
                    case "ok":
                        row.createCell(resultColumn).setCellValue("OK");
                        rowData.setRowStatus("OK"); // Set the row status
                        break;
                    case "both":
                        row.createCell(resultColumn).setCellValue("Text Error & Truncation");
                        rowData.setRowStatus("Text Error & Truncation"); // Set the row status
                        break;
                }

                // Save the updated workbook
                try (FileOutputStream fos = new FileOutputStream(new File(baseExcelPath))) {
                    workbook.write(fos);
                }

                // Update the text area
                errorTextArea.setText(userInput);

                // Display the row status
                //displayRowStatus(rowData.getRowStatus());
                label7.setText("Status update: "+rowData.getRowStatus());
                // Refresh the UI
                refreshUI();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void displayRowStatus(String status) {
        // Display the status in a JOptionPane or any other UI component as needed
        JOptionPane.showMessageDialog(frame, "Row Status: " + status, "Status", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void refreshUI() {
        // Refresh the UI, you might need to add specific logic based on your UI framework
        // For example, updating the file list, displaying the next row, etc.
        updateFileList();
        displayRow(currentRow);
    }





    private static JPanel createLeftPanel() {
        // Create and configure the left panel (file explorer)
        JPanel leftPanel = new JPanel(new BorderLayout());
        customizeColumnsButton = new JButton("Configure Columns");
        customizeColumnsButton.addActionListener(e -> openColumnCustomizationWindow());

        // Add sheet selector and file list to the left panel
        leftPanel.add(sheetSelector, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);

        // Create a new button panel for selecting Excel, Image, and Base Path directories
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(customizeColumnsButton);
        buttonPanel.add(selectExcelButton);
        buttonPanel.add(selectImageButton);
        buttonPanel.add(selectBasePathButton);

        // Add the button panel to the left panel
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set the preferred size of the left panel
        leftPanel.setPreferredSize(new Dimension(200, leftPanel.getPreferredSize().height)); // Adjust the height as needed

        return leftPanel;
    }

    private static void openColumnCustomizationWindow() {
        // Create a new JFrame for the customization window
        JFrame customizeFrame = new JFrame("Customize Columns");
        customizeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create a panel to hold the customization options
        JPanel customizePanel = new JPanel(new GridLayout(0, 3));
        JButton menuButton = new JButton("Menu");
        JButton warningsButton = new JButton("Warnings");



        customizePanel.add(menuButton);
        customizePanel.add(warningsButton);

        // Get the default column indices
        int defaultReferenceIndex = Configuration.getColumnIndex("Reference");
        int defaultMenuNameIndex = Configuration.getColumnIndex("Menu Name");
        int defaultXLeftIndex = Configuration.getColumnIndex("X Left");
        int defaultYTopIndex = Configuration.getColumnIndex("Y Top");
        int defaultXRightIndex = Configuration.getColumnIndex("X Right");
        int defaultYBottomIndex = Configuration.getColumnIndex("Y Bottom");
        int defaultErrorCodeIndex = Configuration.getColumnIndex("Error Code");
        int defaultTargetIndex = Configuration.getColumnIndex("Target");

        // Add labels and text fields for each column
        customizePanel.add(new JLabel("Column Name"));
        customizePanel.add(new JLabel("Current Value"));
        customizePanel.add(new JLabel("New Index"));

        customizePanel.add(new JLabel("Reference"));
        customizePanel.add(new JLabel(String.valueOf(defaultReferenceIndex)));
        JTextField referenceTextField = new JTextField(String.valueOf(defaultReferenceIndex));
        customizePanel.add(referenceTextField);

        customizePanel.add(new JLabel("Menu Name"));
        customizePanel.add(new JLabel(String.valueOf(defaultMenuNameIndex)));
        JTextField menuNameTextField = new JTextField(String.valueOf(defaultMenuNameIndex));
        customizePanel.add(menuNameTextField);

        customizePanel.add(new JLabel("X Left"));
        customizePanel.add(new JLabel(String.valueOf(defaultXLeftIndex)));
        JTextField xLeftTextField = new JTextField(String.valueOf(defaultXLeftIndex));
        customizePanel.add(xLeftTextField);

        customizePanel.add(new JLabel("Y Top"));
        customizePanel.add(new JLabel(String.valueOf(defaultYTopIndex)));
        JTextField yTopTextField = new JTextField(String.valueOf(defaultYTopIndex));
        customizePanel.add(yTopTextField);

        customizePanel.add(new JLabel("X Right"));
        customizePanel.add(new JLabel(String.valueOf(defaultXRightIndex)));
        JTextField xRightTextField = new JTextField(String.valueOf(defaultXRightIndex));
        customizePanel.add(xRightTextField);

        customizePanel.add(new JLabel("Y Bottom"));
        customizePanel.add(new JLabel(String.valueOf(defaultYBottomIndex)));
        JTextField yBottomTextField = new JTextField(String.valueOf(defaultYBottomIndex));
        customizePanel.add(yBottomTextField);

        customizePanel.add(new JLabel("Error Code"));
        customizePanel.add(new JLabel(String.valueOf(defaultErrorCodeIndex)));
        JTextField errorCodeTextField = new JTextField(String.valueOf(defaultErrorCodeIndex));
        customizePanel.add(errorCodeTextField);

        customizePanel.add(new JLabel("Target"));
        customizePanel.add(new JLabel(String.valueOf(defaultTargetIndex)));
        JTextField targetTextField = new JTextField(String.valueOf(defaultTargetIndex));
        customizePanel.add(targetTextField);

        menuButton.addActionListener(e -> {
            Configuration.updateColumnIndex("Reference", 8);
            Configuration.updateColumnIndex("Menu Name",3);
            Configuration.updateColumnIndex("X Left", 4);
            Configuration.updateColumnIndex("Y Top", 5);
            Configuration.updateColumnIndex("X Right", 6);
            Configuration.updateColumnIndex("Y Bottom", 7);
            Configuration.updateColumnIndex("Error Code", 10);
            Configuration.updateColumnIndex("Target", 9);
            customizeFrame.dispose();
        });
        warningsButton.addActionListener(e -> {
            Configuration.updateColumnIndex("Reference", 3);
            Configuration.updateColumnIndex("Menu Name",2);
            Configuration.updateColumnIndex("X Left", 11);
            Configuration.updateColumnIndex("Y Top", 11);
            Configuration.updateColumnIndex("X Right", 11);
            Configuration.updateColumnIndex("Y Bottom", 11);
            Configuration.updateColumnIndex("Error Code", 5);
            Configuration.updateColumnIndex("Target", 4);
            customizeFrame.dispose();
        });

        // Create and add Apply button to apply changes
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            // Apply changes to the configuration
            Configuration.updateColumnIndex("Reference", Integer.parseInt(referenceTextField.getText()));
            Configuration.updateColumnIndex("Menu Name", Integer.parseInt(menuNameTextField.getText()));
            Configuration.updateColumnIndex("X Left", Integer.parseInt(xLeftTextField.getText()));
            Configuration.updateColumnIndex("Y Top", Integer.parseInt(yTopTextField.getText()));
            Configuration.updateColumnIndex("X Right", Integer.parseInt(xRightTextField.getText()));
            Configuration.updateColumnIndex("Y Bottom", Integer.parseInt(yBottomTextField.getText()));
            Configuration.updateColumnIndex("Error Code", Integer.parseInt(errorCodeTextField.getText()));
            Configuration.updateColumnIndex("Target", Integer.parseInt(targetTextField.getText()));

            // Reload data
            loadDataFromXLSX(baseExcelPath, selectedSheet);

            // Close the customization window after applying changes
            customizeFrame.dispose();
        });
        customizePanel.add(applyButton);

        // Add customizePanel to customizeFrame
        customizeFrame.add(customizePanel);

        // Set frame properties for customization window
        customizeFrame.setSize(400, 300);
        customizeFrame.setLocationRelativeTo(frame);
        customizeFrame.setVisible(true);
    }



    private static void customizeColumn(String columnName, boolean isVisible) {
        // Implement logic to customize the columns based on user selection
        // For example, you can reorder the columns or update the display
        // This is a placeholder, and you need to implement the logic based on your requirements
        // You may need to modify the RowData class to reflect the changes
    }

    private static JPanel createRightPanel() {
        // Create and configure the right panel (image display, error input, save, next buttons)
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Add image labels to the top of the right panel
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(mainImageLabel, BorderLayout.NORTH);
        rightPanel.add(imagePanel, BorderLayout.NORTH);

        // Create separate panels for buttons and labels
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JPanel labelPanel = new JPanel(new GridLayout(3, 1));

        // Add the buttons for "Text Error," "Truncation," "Strange," and "OK"
        JButton textErrorButton = new JButton("Text Error");
        JButton truncationButton = new JButton("Truncation");
        JButton strangeButton = new JButton("Strange");
        JButton okButton = new JButton("OK");

        JButton textErrorTruncationButton = new JButton("Text Error & Truncation");  // New button

        // Add action listeners to the buttons
        textErrorButton.addActionListener(e -> handleButtonClick("Text Error"));
        truncationButton.addActionListener(e -> handleButtonClick("Truncation"));
        strangeButton.addActionListener(e -> handleButtonClick("Strange"));
        okButton.addActionListener(e -> handleButtonClick("OK"));
        textErrorTruncationButton.addActionListener(e -> handleButtonClick("both"));

        // Add buttons to the button panel
        buttonPanel.add(textErrorButton);
        buttonPanel.add(truncationButton);
        buttonPanel.add(strangeButton);
        buttonPanel.add(okButton);
        buttonPanel.add(textErrorTruncationButton);  // Add the new button

        // Create instances of JLabel for label5, label6, and label7
        label5 = new JLabel();
        label6 = new JLabel();
        label7 = new JLabel();

        // Add the Target, Reference, and Status labels on the right side
        labelPanel.add(label5);
        labelPanel.add(label6);
        labelPanel.add(label7);

        // Add the buttonPanel and labelPanel to the rightPanel
        rightPanel.add(buttonPanel, BorderLayout.WEST);
        rightPanel.add(labelPanel, BorderLayout.CENTER);

        return rightPanel;
    }




    private static void loadDataFromXLSX(String filePath, String sheetName) {
        validRows = new ArrayList<>();
        System.out.println(selectedSheet);
        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            System.out.println("Selected Sheet Full Path: " + new File(filePath).getAbsolutePath());
            System.out.println("Selected Sheet Path: " + filePath);
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheet(sheetName);

            for (int i = 2; i <= sheet.getLastRowNum(); i++) { // Starting from row 3
                XSSFRow row = sheet.getRow(i);

                    RowData rowData = new RowData();
                    rowData.setReference(getStringValueFromCell(row.getCell(Configuration.getColumnIndex("Reference"))));
                    rowData.setTarget(getStringValueFromCell(row.getCell(Configuration.getColumnIndex("Target"))));
                    rowData.setMenuName(getStringValueFromCell(row.getCell(Configuration.getColumnIndex("Menu Name"))));
                    rowData.setxLeft(getIntValueFromCell(row.getCell(Configuration.getColumnIndex("X Left"))));
                    rowData.setyTop(getIntValueFromCell(row.getCell(Configuration.getColumnIndex("Y Top"))));
                    rowData.setxRight(getIntValueFromCell(row.getCell(Configuration.getColumnIndex("X Right"))));
                    rowData.setyBottom(getIntValueFromCell(row.getCell(Configuration.getColumnIndex("Y Bottom"))));
                    rowData.setErrorCode(getStringValueFromCell(row.getCell(Configuration.getColumnIndex("Error Code"))));

                // Check if MenuName is empty or null, and stop loading rows
                if (rowData.getMenuName() == null || rowData.getMenuName().isEmpty()) {
                    break;
                }

                // Construct the full image path based on the selected image type and baseImagePath
                String imagePath = baseImagePath + File.separator + selectedImageType
                        + rowData.getMenuName() + "_" + sheetName + ".png";
                rowData.setImgPath(imagePath);

                validRows.add(rowData);
                printRowDataDetails(validRows);
            }

            workbook.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("File not found: " + selectedSheet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static void printRowDataDetails(List<RowData> rows) {
        for (RowData rowData : rows) {
            System.out.println("Row Index: " + rowData.getRowIndex());
            System.out.println("Reference: " + rowData.getReference());
            System.out.println("Menu Name: " + rowData.getMenuName());
            System.out.println("X Left: " + rowData.getxLeft());
            System.out.println("Y Top: " + rowData.getyTop());
            System.out.println("X Right: " + rowData.getxRight());
            System.out.println("Y Bottom: " + rowData.getyBottom());
            System.out.println("Error Code: " + rowData.getErrorCode());
            System.out.println("Image Cropped Path: " + rowData.getImgCroppedPath());
            System.out.println("Image Path: " + rowData.getImgPath());
            System.out.println("Target: " + rowData.getTarget());
            System.out.println("--------------");
        }
    }



    private static void updateFileList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (RowData rowData : validRows) {
            // Display reference and image path in the list
            String displayText = rowData.getReference() + " - " + rowData.getImgPath();

            // Set text color to green for rows with errorCode "0" or empty
            if (rowData.getErrorCode() == null || rowData.getErrorCode().isEmpty() || rowData.getErrorCode().equals("0")) {
                displayText = "<html><font color='green'>" + displayText + "</font></html>";
            } else {
                displayText = "<html><font color='red'>" + displayText + "</font></html>";
            }

            // Increase font size
            displayText = "<html><font size='5'>" + displayText + "</font></html>";

            listModel.addElement(displayText);
        }
        fileList.setModel(listModel);
    }


    private static void displayRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < validRows.size()) {
            RowData rowData = validRows.get(rowIndex);

            // Load the image and set it to the mainImageLabel
            ImageIcon mainImage = new ImageIcon(rowData.getImgPath());
            mainImageLabel.setIcon(mainImage);

            // Set the size of the ImageLabel to match the image size
            mainImageLabel.setSize(mainImage.getIconWidth(), mainImage.getIconHeight());

            // Clear the error text area
            errorTextArea.setText("");







            // Set font size and color for Target and Reference labels
            label5.setFont(new Font("Greta Sans", Font.PLAIN, 25));
            label6.setFont(new Font("Greta Sans", Font.PLAIN, 25));
            label7.setFont(new Font("Greta Sans", Font.PLAIN, 25));

            // Set label colors based on conditions
            if (rowData.getTarget().equals(rowData.getReference())) {
                label5.setForeground(Color.BLUE);
                label6.setForeground(Color.BLUE);
            } else {
                label5.setForeground(Color.RED);
                label6.setForeground(Color.BLUE);
            }

            // Set text for Target and Reference labels
            label5.setText("T: " + rowData.getTarget());
            label6.setText("R: " + rowData.getReference());
            label7.setText("Status: " + rowData.getRowStatus());

            // Refresh the main image label
            mainImageLabel.setRectangleBounds(rowData.getxLeft(), rowData.getyTop(), rowData.getxRight(), rowData.getyBottom());

            mainImageLabel.repaint(); // Ensure repaint to make the highlight visible
        }
    }



    private static void saveData() {
        if (currentRow >= 0 && currentRow < validRows.size()) {
            RowData rowData = validRows.get(currentRow);
            String userInput = errorTextArea.getText();

            try (FileInputStream fis = new FileInputStream(new File(selectedSheet));
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

                XSSFSheet sheet = workbook.getSheet(selectedSheet);
                XSSFRow row = sheet.getRow(currentRow + 2);

                // Update cells in the row with new data
                row.createCell(11).setCellValue(userInput);

                // Save the updated workbook
                try (FileOutputStream fos = new FileOutputStream(new File(selectedSheet))) {
                    workbook.write(fos);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class RowData {
    private int rowIndex;
    private String reference;
    private String menuName;
    private int xLeft;
    private int yTop;
    private int xRight;
    private int yBottom;
    private String errorCode;
    private String imgCroppedPath;
    private String imgPath;

    public String getRowStatus() {
        return rowStatus;
    }

    public void setRowStatus(String rowStatus) {
        this.rowStatus = rowStatus;
    }

    // Add this line inside the RowData class
    private String rowStatus;


    private String target;

    // Constructors, getters, and setters

    public RowData() {
        // Default constructor
        this.rowStatus = "";
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public int getxLeft() {
        return xLeft;
    }

    public void setxLeft(int xLeft) {
        this.xLeft = xLeft;
    }

    public int getyTop() {
        return yTop;
    }

    public void setyTop(int yTop) {
        this.yTop = yTop;
    }

    public int getxRight() {
        return xRight;
    }

    public void setxRight(int xRight) {
        this.xRight = xRight;
    }

    public int getyBottom() {
        return yBottom;
    }

    public void setyBottom(int yBottom) {
        this.yBottom = yBottom;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getImgCroppedPath() {
        System.out.println(imgCroppedPath);
        return imgCroppedPath;
    }

    public void setImgCroppedPath(String imgCroppedPath) {
        System.out.println(imgCroppedPath);
        this.imgCroppedPath = imgCroppedPath;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    static int getIntValueFromCell(Cell cell) {
        if (cell == null) {
            return -1; // or return a default value as needed, in this case, returning -1
        }

        switch(cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                String cellValue = cell.getStringCellValue().trim();
                try {
                    return Integer.parseInt(cellValue);
                } catch(NumberFormatException e) {
                    return -1; // or handle differently
                }
            default:
                return -1; // or handle differently
        }
    }

    static String getStringValueFromCell(Cell cell) {
        if (cell == null) {
            return ""; // or return a default value as needed, in this case, returning an empty string
        }

        if(cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if(cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        } else {
            return "";
        }
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}

class Configuration {
    private static int referenceIndex = 8;
    private static int menuNameIndex = 3;
    private static int xLeftIndex = 4;
    private static int yTopIndex = 5;
    private static int xRightIndex = 6;
    private static int yBottomIndex = 7;
    private static int errorCodeIndex = 10;
    private static int targetIndex = 9;

    public static int getColumnIndex(String columnName) {
        switch (columnName) {
            case "Reference":
                return referenceIndex;
            case "Menu Name":
                return menuNameIndex;
            case "X Left":
                return xLeftIndex;
            case "Y Top":
                return yTopIndex;
            case "X Right":
                return xRightIndex;
            case "Y Bottom":
                return yBottomIndex;
            case "Error Code":
                return errorCodeIndex;
            case "Target":
                return targetIndex;
            default:
                return -1;
        }
    }

    public static void updateColumnIndex(String columnName, int newIndex) {
        switch (columnName) {
            case "Reference":
                referenceIndex = newIndex;
                break;
            case "Menu Name":
                menuNameIndex = newIndex;
                break;
            case "X Left":
                xLeftIndex = newIndex;
                break;
            case "Y Top":
                yTopIndex = newIndex;
                break;
            case "X Right":
                xRightIndex = newIndex;
                break;
            case "Y Bottom":
                yBottomIndex = newIndex;
                break;
            case "Error Code":
                errorCodeIndex = newIndex;
                break;
            case "Target":
                targetIndex = newIndex;
                break;
        }
    }
}
