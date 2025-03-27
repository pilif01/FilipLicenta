package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class NameChangingTool extends JFrame {

    private JLabel statusLabel;
    private File selectedFolder;
    private JProgressBar progressBar;
    private JTextField prefixField, suffixField;

    public NameChangingTool() {
        setTitle("Name Changing Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Create UI Components with improved layout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new GridLayout(4, 2, 10, 10)); // 4 rows, 2 columns
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton addPicButton = createButton("Add PIC_");
        JButton removePicButton = createButton("Remove PIC_");
        JButton addCenterButton = createButton("Add CENTER_");
        JButton removeCenterButton = createButton("Remove CENTER_");
        JButton addSingleButton = createButton("Add SINGLE_");
        JButton removeSingleButton = createButton("Remove SINGLE_");
        JButton add0xButton = createButton("Add 0x");
        JButton remove0xButton = createButton("Remove 0x");

        // Add buttons to the panel
        buttonsPanel.add(addPicButton);
        buttonsPanel.add(removePicButton);
        buttonsPanel.add(addCenterButton);
        buttonsPanel.add(removeCenterButton);
        buttonsPanel.add(addSingleButton);
        buttonsPanel.add(removeSingleButton);
        buttonsPanel.add(add0xButton);
        buttonsPanel.add(remove0xButton);

        centerPanel.add(buttonsPanel);

        // Prefix/Suffix Fields and labels
        JPanel textPanel = new JPanel(new FlowLayout());
        textPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(new JLabel("Prefix:"));
        prefixField = new JTextField(10);
        textPanel.add(prefixField);
        textPanel.add(new JLabel("Suffix:"));
        suffixField = new JTextField(10);
        textPanel.add(suffixField);

        // Add and Remove buttons for Prefix and Suffix
        JPanel applyPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        applyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton addPrefixButton = new JButton("Add Prefix");
        addPrefixButton.setPreferredSize(new Dimension(100, 40));
        addPrefixButton.addActionListener(e -> handlePrefixAction(prefixField.getText().trim(), true));

        JButton removePrefixButton = new JButton("Remove Prefix");
        removePrefixButton.setPreferredSize(new Dimension(100, 40));
        removePrefixButton.addActionListener(e -> handlePrefixAction(prefixField.getText().trim(), false));

        JButton addSuffixButton = new JButton("Add Suffix");
        addSuffixButton.setPreferredSize(new Dimension(100, 40));
        addSuffixButton.addActionListener(e -> handleSuffixAction(suffixField.getText().trim(), true));

        JButton removeSuffixButton = new JButton("Remove Suffix");
        removeSuffixButton.setPreferredSize(new Dimension(100, 40));
        removeSuffixButton.addActionListener(e -> handleSuffixAction(suffixField.getText().trim(), false));

        applyPanel.add(addPrefixButton);
        applyPanel.add(removePrefixButton);
        applyPanel.add(addSuffixButton);
        applyPanel.add(removeSuffixButton);

        centerPanel.add(Box.createVerticalStrut(20));  // Adding space between elements
        centerPanel.add(textPanel);
        centerPanel.add(applyPanel);

        add(centerPanel, BorderLayout.CENTER);

        // Status Label
        statusLabel = new JLabel("Status: No folder selected", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(statusLabel, BorderLayout.NORTH);

        // Progress bar (initially hidden)
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar, BorderLayout.NORTH);

        // Bottom panel for "Select Folder" button
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton selectFolderButton = new JButton("Select Folder");
        selectFolderButton.setPreferredSize(new Dimension(150, 40)); // Adjust button size
        bottomPanel.add(selectFolderButton);
        bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button Listeners
        selectFolderButton.addActionListener(this::handleSelectFolder);
        addPicButton.addActionListener(e -> handlePrefixAction("PIC_", true));
        removePicButton.addActionListener(e -> handlePrefixAction("PIC_", false));
        addCenterButton.addActionListener(e -> handlePrefixAction("CENTER_", true));
        removeCenterButton.addActionListener(e -> handlePrefixAction("CENTER_", false));
        addSingleButton.addActionListener(e -> handlePrefixAction("SINGLE_", true));
        removeSingleButton.addActionListener(e -> handlePrefixAction("SINGLE_", false));
        add0xButton.addActionListener(e -> handlePrefixAction("0x", true));
        remove0xButton.addActionListener(e -> handlePrefixAction("0x", false));

        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40)); // Adjust size of buttons
        button.setFont(new Font("Arial", Font.PLAIN, 14)); // Set a uniform font for buttons
        return button;
    }

    private void handleSelectFolder(ActionEvent event) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);

        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFolder = folderChooser.getSelectedFile();
            statusLabel.setText("Selected folder: " + selectedFolder.getAbsolutePath());
        } else {
            statusLabel.setText("No folder selected");
        }
    }

    private void handlePrefixAction(String prefix, boolean add) {
        if (selectedFolder == null) {
            JOptionPane.showMessageDialog(this, "Please select a folder first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] pngFiles = selectedFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngFiles == null || pngFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No PNG files found in the selected folder.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        progressBar.setMaximum(pngFiles.length);
        progressBar.setValue(0);
        progressBar.setVisible(true);

        AtomicInteger count = new AtomicInteger(0);

        new Thread(() -> {
            for (File file : pngFiles) {
                String fileName = file.getName();
                String extension = getFileExtension(fileName);
                String baseName = removeExtension(fileName);  // Get base name without extension
                String newFileName = fileName;

                // Add prefix or suffix
                if (add) {
                    if (baseName.startsWith(prefix)) {
                        continue; // Skip if the prefix already exists
                    } else {
                        newFileName = prefix + baseName + extension;
                    }
                } else {
                    if (baseName.startsWith(prefix)) {
                        newFileName = baseName.substring(prefix.length()) + extension;
                    } else {
                        continue; // Skip if it doesn't start with the prefix
                    }
                }

                // Rename files
                File renamedFile = new File(file.getParent(), newFileName);
                if (file.renameTo(renamedFile)) {
                    int currentCount = count.incrementAndGet();
                    SwingUtilities.invokeLater(() -> progressBar.setValue(currentCount));
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Failed to rename file: " + fileName, "Error", JOptionPane.ERROR_MESSAGE));
                }
            }

            SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(false);
                JOptionPane.showMessageDialog(this, "Files renamed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("Status: Files renamed successfully");
            });
        }).start();
    }

    private void handleSuffixAction(String suffix, boolean add) {
        if (selectedFolder == null) {
            JOptionPane.showMessageDialog(this, "Please select a folder first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] pngFiles = selectedFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngFiles == null || pngFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No PNG files found in the selected folder.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        progressBar.setMaximum(pngFiles.length);
        progressBar.setValue(0);
        progressBar.setVisible(true);

        AtomicInteger count = new AtomicInteger(0);

        new Thread(() -> {
            for (File file : pngFiles) {
                String fileName = file.getName();
                String extension = getFileExtension(fileName);
                String baseName = removeExtension(fileName);  // Get base name without extension
                String newFileName = fileName;

                // Add or remove suffix
                if (add) {
                    if (baseName.endsWith(suffix)) {
                        continue; // Skip if the suffix already exists
                    } else {
                        newFileName = baseName + suffix + extension;
                    }
                } else {
                    if (baseName.endsWith(suffix)) {
                        newFileName = baseName.substring(0, baseName.length() - suffix.length()) + extension;
                    } else {
                        continue; // Skip if it doesn't end with the suffix
                    }
                }

                // Rename files
                File renamedFile = new File(file.getParent(), newFileName);
                if (file.renameTo(renamedFile)) {
                    int currentCount = count.incrementAndGet();
                    SwingUtilities.invokeLater(() -> progressBar.setValue(currentCount));
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Failed to rename file: " + fileName, "Error", JOptionPane.ERROR_MESSAGE));
                }
            }

            SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(false);
                JOptionPane.showMessageDialog(this, "Files renamed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("Status: Files renamed successfully");
            });
        }).start();
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex); // Returns ".png" or empty if no extension
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex); // Returns filename without extension
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NameChangingTool tool = new NameChangingTool();
            tool.setVisible(true);
        });
    }
}
