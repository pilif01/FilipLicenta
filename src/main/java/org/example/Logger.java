package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final String LOG_FILE = "OCR-log.txt";

    // Log header for each session
    public static void logSessionHeader() {
        log("===================================================");
        log("OCR Tool Session: " + getCurrentTimestamp());
        log("===================================================");
    }

    // Log a message to both console and file
    public static void log(String message) {
        System.out.println(message);
        writeToFile(getCurrentTimestamp() + " - " + message);
    }

    // Log an exception to both console and file
    public static void logException(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
        writeToFile(getCurrentTimestamp() + " - " + message + ": " + e.getMessage());
    }

    // Get the current timestamp
    private static String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    // Write a message to the log file
    private static void writeToFile(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
