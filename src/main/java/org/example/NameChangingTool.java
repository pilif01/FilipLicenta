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

    public NameChangingTool() {
        setTitle("Name Changing Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Create UI Components with a 4x2 grid for 8 buttons
        JPanel centerPanel = new JPanel(new GridLayout(4, 2, 10, 10));

        JButton addPicButton = new JButton("Add PIC_");
        JButton removePicButton = new JButton("Remove PIC_");
        JButton addCenterButton = new JButton("Add CENTER_");
        JButton removeCenterButton = new JButton("Remove CENTER_");
        JButton addSingleButton = new JButton("Add SINGLE_");
        JButton removeSingleButton = new JButton("Remove SINGLE_");
        JButton add0xButton = new JButton("Add 0x");
        JButton remove0xButton = new JButton("Remove 0x");

        // Increase button size
        Dimension buttonSize = new Dimension(200, 50);
        addPicButton.setPreferredSize(buttonSize);
        removePicButton.setPreferredSize(buttonSize);
        addCenterButton.setPreferredSize(buttonSize);
        removeCenterButton.setPreferredSize(buttonSize);
        addSingleButton.setPreferredSize(buttonSize);
        removeSingleButton.setPreferredSize(buttonSize);
        add0xButton.setPreferredSize(buttonSize);
        remove0xButton.setPreferredSize(buttonSize);

        // Add buttons to center panel in order
        centerPanel.add(addPicButton);
        centerPanel.add(removePicButton);
        centerPanel.add(addCenterButton);
        centerPanel.add(removeCenterButton);
        centerPanel.add(addSingleButton);
        centerPanel.add(removeSingleButton);
        centerPanel.add(add0xButton);
        centerPanel.add(remove0xButton);

        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(centerPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("Status: No folder selected", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        // Progress bar (initially hidden)
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar, BorderLayout.NORTH);

        // Bottom panel for "Select Folder" button
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton selectFolderButton = new JButton("Select Folder");
        bottomPanel.add(selectFolderButton);
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
                String newFileName;

                if (add) {
                    if (!fileName.startsWith(prefix)) {
                        newFileName = prefix + fileName;
                    } else {
                        continue;
                    }
                } else {
                    if (fileName.startsWith(prefix)) {
                        newFileName = fileName.substring(prefix.length());
                    } else {
                        continue;
                    }
                }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NameChangingTool tool = new NameChangingTool();
            tool.setVisible(true);
        });
    }
}
