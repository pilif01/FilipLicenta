package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

// this class serves as a logger utility for the ocr tool.
// it logs messages and exceptions to both the console and a log file.
public class Logger {

    // constant holding the log file name
    private static final String LOG_FILE = "OCR-log.txt";

    // logs a session header to mark the start of a new logging session
    // it writes a separator, the current timestamp, and another separator
    public static void logSessionHeader() {
        // write a separator line
        log("===================================================");
        // write the session header with current timestamp
        log("ocr tool session: " + getCurrentTimestamp());
        // write another separator line
        log("===================================================");
    }

    // logs a message to the console and to the log file
    // the message is prefixed with the current timestamp
    public static void log(String message) {
        // print the message to the console
        System.out.println(message);
        // write the message with timestamp to the log file
        writeToFile(getCurrentTimestamp() + " - " + message);
    }

    // logs an exception by printing an error message and the exception details
    // both the console and the log file are updated with the error information
    public static void logException(String message, Exception e) {
        // print the error message and exception details to the error console
        System.err.println(message + ": " + e.getMessage());
        // print the stack trace to the console
        e.printStackTrace();
        // write the error message with timestamp to the log file
        writeToFile(getCurrentTimestamp() + " - " + message + ": " + e.getMessage());
    }

    // returns the current timestamp as a formatted string
    // the format used is "yyyy-MM-dd hh:mm:ss"
    private static String getCurrentTimestamp() {
        // create a simple date format with the desired pattern
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // return the formatted current date and time
        return dateFormat.format(new Date());
    }

    // writes a message to the log file
    // the message is appended to the file; if an io exception occurs, it prints an error to the console
    private static void writeToFile(String message) {
        // use try-with-resources to automatically close the writer
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            // write the message followed by a new line
            writer.println(message);
        } catch (IOException e) {
            // print an error message if writing to the file fails
            System.err.println("error writing to log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
