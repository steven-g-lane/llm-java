package com.pergamon.llm.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper utilities for testing log file output.
 * Uses test-specific log files in target/test-logs to avoid interference between tests.
 */
public class LogTestHelper {

    private static final Path LOGS_DIR = Paths.get("target/test-logs");
    private static final Path API_LOG = LOGS_DIR.resolve("api.log");
    private static final Path ERROR_LOG = LOGS_DIR.resolve("error.log");

    /**
     * Reads all lines from the API log file.
     * @return list of log lines, or empty list if file doesn't exist
     */
    public static List<String> readApiLog() throws IOException {
        if (!Files.exists(API_LOG)) {
            return List.of();
        }
        return Files.readAllLines(API_LOG);
    }

    /**
     * Reads all lines from the error log file.
     * @return list of log lines, or empty list if file doesn't exist
     */
    public static List<String> readErrorLog() throws IOException {
        if (!Files.exists(ERROR_LOG)) {
            return List.of();
        }
        return Files.readAllLines(ERROR_LOG);
    }

    /**
     * Checks if the API log contains a specific vendor's request.
     * @param vendorName the vendor name (e.g., "ANTHROPIC")
     * @return true if a request from this vendor was logged
     */
    public static boolean apiLogContainsRequest(String vendorName) throws IOException {
        String fullLog = String.join("\n", readApiLog());
        return fullLog.contains("[" + vendorName + "] Request:");
    }

    /**
     * Checks if the API log contains a specific vendor's response.
     * @param vendorName the vendor name (e.g., "ANTHROPIC")
     * @return true if a response from this vendor was logged
     */
    public static boolean apiLogContainsResponse(String vendorName) throws IOException {
        String fullLog = String.join("\n", readApiLog());
        return fullLog.contains("[" + vendorName + "] Response:");
    }

    /**
     * Checks if the error log contains an exception with the given class name.
     * @param exceptionClassName the simple class name of the exception
     * @return true if the exception was logged
     */
    public static boolean errorLogContainsException(String exceptionClassName) throws IOException {
        return readErrorLog().stream()
            .anyMatch(line -> line.contains(exceptionClassName));
    }

    /**
     * Checks if the error log contains a message from a specific class and method.
     * @param className the simple class name
     * @param methodName the method name
     * @return true if an error from this class/method was logged
     */
    public static boolean errorLogContainsMethodError(String className, String methodName) throws IOException {
        String pattern = className + "." + methodName + "()";
        return readErrorLog().stream()
            .anyMatch(line -> line.contains(pattern));
    }

    /**
     * Clears the log files (useful for test setup).
     */
    public static void clearLogs() throws IOException {
        if (Files.exists(API_LOG)) {
            Files.delete(API_LOG);
        }
        if (Files.exists(ERROR_LOG)) {
            Files.delete(ERROR_LOG);
        }
    }

    /**
     * Gets the number of log entries in the API log.
     * Counts lines that start with a timestamp pattern.
     */
    public static long getApiLogEntryCount() throws IOException {
        return readApiLog().stream()
            .filter(line -> line.matches("^\\d{4}-\\d{2}-\\d{2}.*"))
            .count();
    }

    /**
     * Gets the number of log entries in the error log.
     * Counts lines that start with a timestamp pattern.
     */
    public static long getErrorLogEntryCount() throws IOException {
        return readErrorLog().stream()
            .filter(line -> line.matches("^\\d{4}-\\d{2}-\\d{2}.*"))
            .count();
    }

    /**
     * Gets recent API log entries (last n entries).
     */
    public static List<String> getRecentApiLogEntries(int count) throws IOException {
        List<String> allLines = readApiLog();
        List<String> timestampLines = allLines.stream()
            .filter(line -> line.matches("^\\d{4}-\\d{2}-\\d{2}.*"))
            .collect(Collectors.toList());

        int size = timestampLines.size();
        if (size <= count) {
            return timestampLines;
        }
        return timestampLines.subList(size - count, size);
    }
}
