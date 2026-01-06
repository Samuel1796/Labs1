package utilities;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

    /**
     * Centralized logging utility using Java's built-in Logger with daily file rotation.
     */
public class Logger {
    
    /**
     * Represents a log entry stored in memory for fast access.
     */
    public static class LogEntry {
        private final long timestamp;
        private final String level;
        private final String threadName;
        private final String message;
        
        public LogEntry(long timestamp, String level, String threadName, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.threadName = threadName;
            this.message = message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getLevel() {
            return level;
        }
        
        public String getThreadName() {
            return threadName;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getFormattedTimestamp() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
        }
        
        @Override
        public String toString() {
            return String.format("[%s] [%s] [%s] %s", 
                getFormattedTimestamp(), level, threadName, message);
        }
    }
    
    private static final String LOG_DIR = "logs";
    private static final String LOGGER_NAME = "StudentGradeManagementSystem";
    private static java.util.logging.Logger logger;
    private static FileHandler fileHandler;
    private static String currentLogFile;
    private static LocalDate currentLogDate;
    
    private static final AtomicInteger totalLogs = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static final AtomicInteger warnCount = new AtomicInteger(0);
    private static final AtomicLong totalLogTime = new AtomicLong(0);
    
    private static final AtomicInteger initialized = new AtomicInteger(0);
    
    private static final ConcurrentLinkedQueue<LogEntry> inMemoryLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_MEMORY_LOGS = 10000;
    
    /**
     * Initializes the logger system using Java's built-in Logger.
     */
    public static synchronized void initialize() {
        if (initialized.get() > 0) {
            return;
        }
        
        try {
            // Ensure LOG_DIR is not null
            if (LOG_DIR == null || LOG_DIR.isEmpty()) {
                throw new IllegalStateException("LOG_DIR is not initialized");
            }
            
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            logger = java.util.logging.Logger.getLogger(LOGGER_NAME);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL] [%2$s] [%3$s] %4$s%n",
                        new Date(record.getMillis()),
                        record.getLevel(),
                        Thread.currentThread().getName(),
                        record.getMessage());
                }
            });
            logger.addHandler(consoleHandler);
            
            currentLogDate = LocalDate.now();
            try {
                rotateLogFile();
            } catch (IOException e) {
                System.err.println("Error creating log file: " + e.getMessage());
                e.printStackTrace();
                // Set a fallback log file path even if rotation failed
                if (currentLogFile == null && currentLogDate != null) {
                    try {
                        String dateStr = currentLogDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        Path fallbackLogDir = Paths.get(LOG_DIR);
                        Path logPath = fallbackLogDir.resolve("app-" + dateStr + ".log");
                        currentLogFile = logPath.toString();
                    } catch (Exception ex) {
                        System.err.println("Failed to set fallback log file path: " + ex.getMessage());
                    }
                }
                // Don't throw - allow application to continue with limited logging
                System.err.println("Warning: Logger initialization had errors, but continuing...");
            }
            
            initialized.incrementAndGet();
            
            // Force write to ensure file is created
            if (logger != null) {
                logger.info("Logger initialized successfully");
                if (currentLogFile != null) {
                    logger.info("Log file: " + currentLogFile);
                }
                if (fileHandler != null) {
                    fileHandler.flush();
                }
            }
            
            // Verify file was created
            if (currentLogFile != null) {
                Path logPath = Paths.get(currentLogFile);
                if (Files.exists(logPath)) {
                    System.out.println("Logger initialized - Log file created: " + logPath.toAbsolutePath());
                } else {
                    System.err.println("Warning: Log file was not created at: " + logPath.toAbsolutePath());
                }
            } else {
                System.err.println("Warning: Log file path is null - logging may not work correctly");
            }
        } catch (Exception e) {
            System.err.println("Warning: Logger initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Rotates log file if date has changed.
     */
    private static synchronized void rotateLogFile() throws IOException {
        LocalDate now = LocalDate.now();
        if (currentLogDate == null || !now.equals(currentLogDate)) {
            if (fileHandler != null && logger != null) {
                logger.removeHandler(fileHandler);
                fileHandler.close();
                fileHandler = null;
            }
            
            currentLogDate = now;
            String dateStr = currentLogDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Construct log file path safely using Paths
            if (LOG_DIR == null || LOG_DIR.isEmpty()) {
                throw new IllegalStateException("LOG_DIR is not initialized");
            }
            
            // Ensure log directory exists
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            // Construct log file path using Paths for cross-platform compatibility
            Path logPath = logDir.resolve("app-" + dateStr + ".log");
            currentLogFile = logPath.toString();
            if (!Files.exists(logPath)) {
                try {
                    Files.createFile(logPath);
                } catch (IOException e) {
                    System.err.println("Failed to create log file: " + currentLogFile);
                    throw e;
                }
            }
            
            // Create file handler
            try {
                fileHandler = new FileHandler(currentLogFile, true);
            } catch (IOException e) {
                System.err.println("Failed to create file handler for: " + currentLogFile);
                throw e;
            }
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL] [%2$s] [%3$s] %4$s%n",
                        new Date(record.getMillis()),
                        record.getLevel(),
                        Thread.currentThread().getName(),
                        record.getMessage());
                }
            });
            fileHandler.setLevel(Level.ALL);
            
            if (logger != null) {
                logger.addHandler(fileHandler);
                // Force immediate write to verify file creation
                logger.info("Log file created: " + currentLogFile);
                fileHandler.flush();
            }
        }
    }
    
    /**
     * Forces log file rotation check and ensures file handler exists.
     */
    private static synchronized void ensureFileHandler() {
        try {
            if (fileHandler == null || currentLogDate == null) {
                rotateLogFile();
            } else {
                LocalDate now = LocalDate.now();
                if (!now.equals(currentLogDate)) {
                    rotateLogFile();
                }
            }
        } catch (IOException e) {
            System.err.println("Error ensuring file handler: " + e.getMessage());
        }
    }
    
    /**
     * Ensures logger is initialized and log file is current.
     */
    private static void ensureInitialized() {
        if (initialized.get() == 0) {
            initialize();
        }
        ensureFileHandler();
    }
    
    /**
     * Adds a log entry to in-memory storage.
     */
    private static void addToMemoryLogs(String level, String message) {
        long timestamp = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        LogEntry entry = new LogEntry(timestamp, level, threadName, message);
        inMemoryLogs.offer(entry);
        
        if (inMemoryLogs.size() > MAX_MEMORY_LOGS) {
            inMemoryLogs.poll();
        }
    }
    
    /**
     * Logs a message at DEBUG level.
     */
    public static void debug(String message) {
        ensureInitialized();
        addToMemoryLogs("DEBUG", message);
        if (logger != null) {
            logger.log(Level.FINE, message);
        }
        updateCounters(Level.FINE);
    }
    
    /**
     * Logs a message at INFO level.
     */
    public static void info(String message) {
        ensureInitialized();
        addToMemoryLogs("INFO", message);
        if (logger != null) {
            logger.info(message);
            if (fileHandler != null) {
                fileHandler.flush();
            }
        }
        updateCounters(Level.INFO);
    }
    
    /**
     * Logs a message at WARN level.
     */
    public static void warn(String message) {
        ensureInitialized();
        addToMemoryLogs("WARN", message);
        if (logger != null) {
            logger.warning(message);
        }
        updateCounters(Level.WARNING);
    }
    
    /**
     * Logs a message at WARN level with exception.
     */
    public static void warn(String message, Throwable throwable) {
        ensureInitialized();
        String fullMessage = message + " - " + throwable.getMessage();
        addToMemoryLogs("WARN", fullMessage);
        if (logger != null) {
            logger.log(Level.WARNING, message, throwable);
        }
        updateCounters(Level.WARNING);
    }
    
    /**
     * Logs a message at ERROR level.
     */
    public static void error(String message) {
        ensureInitialized();
        addToMemoryLogs("ERROR", message);
        if (logger != null) {
            logger.severe(message);
        }
        updateCounters(Level.SEVERE);
    }
    
    /**
     * Logs a message at ERROR level with exception.
     */
    public static void error(String message, Throwable throwable) {
        ensureInitialized();
        String fullMessage = message + " - " + throwable.getMessage();
        addToMemoryLogs("ERROR", fullMessage);
        if (logger != null) {
            logger.log(Level.SEVERE, message, throwable);
        }
        updateCounters(Level.SEVERE);
    }
    
    /**
     * Updates log counters.
     */
    private static void updateCounters(Level level) {
        totalLogs.incrementAndGet();
        if (level == Level.SEVERE) {
            errorCount.incrementAndGet();
        } else if (level == Level.WARNING) {
            warnCount.incrementAndGet();
        }
    }
    
    /**
     * Logs performance metrics with operation time, collection sizes, and thread pool metrics.
     */
    public static void logPerformance(String operation, long durationMs, Map<String, Object> metrics) {
        StringBuilder sb = new StringBuilder(String.format("PERF: %s took %d ms", operation, durationMs));
        if (metrics != null && !metrics.isEmpty()) {
            sb.append(" | Metrics: ");
            for (Map.Entry<String, Object> metric : metrics.entrySet()) {
                sb.append(metric.getKey()).append("=").append(metric.getValue()).append(" ");
            }
        }
        info(sb.toString().trim());
    }
    
    /**
     * Logs performance metrics with collection sizes and thread pool information.
     */
    public static void logPerformanceWithCollections(String operation, long durationMs, 
            int collectionSize, String collectionName, ExecutorService threadPool) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("collectionSize", collectionSize);
        metrics.put("collectionName", collectionName);
        if (threadPool != null) {
            if (threadPool instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                metrics.put("activeThreads", tpe.getActiveCount());
                metrics.put("poolSize", tpe.getPoolSize());
                metrics.put("queueSize", tpe.getQueue().size());
            }
        }
        logPerformance(operation, durationMs, metrics);
    }
    
    /**
     * Logs audit trail entry.
     */
    public static void logAudit(String operation, String userAction, long executionTime, boolean success, String details) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("AUDIT: %s | Action: %s | Time: %d ms | Status: %s | %s", 
            operation, userAction, executionTime, status, details);
        info(message);
    }
    
    /**
     * Gets logger statistics.
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", totalLogs.get());
        stats.put("errorCount", errorCount.get());
        stats.put("warnCount", warnCount.get());
        stats.put("currentLogFile", currentLogFile);
        long avgLogTime = totalLogs.get() > 0 ? totalLogTime.get() / totalLogs.get() : 0;
        stats.put("averageLogTimeNs", avgLogTime);
        return stats;
    }
    
    /**
     * Gets the current log file path.
     */
    public static String getCurrentLogFile() {
        return currentLogFile;
    }
    
    /**
     * Gets the log directory path.
     */
    public static String getLogDirectory() {
        return LOG_DIR;
    }
    
    /**
     * Gets all log entries from memory for fast access.
     */
    public static List<LogEntry> getAllLogs() {
        return new ArrayList<>(inMemoryLogs);
    }
    
    /**
     * Gets recent log entries from memory.
     */
    public static List<LogEntry> getRecentLogs(int count) {
        List<LogEntry> allLogs = new ArrayList<>(inMemoryLogs);
        int start = Math.max(0, allLogs.size() - count);
        return allLogs.subList(start, allLogs.size());
    }
    
    /**
     * Gets log entries filtered by level.
     */
    public static List<LogEntry> getLogsByLevel(String level) {
        List<LogEntry> filtered = new ArrayList<>();
        for (LogEntry entry : inMemoryLogs) {
            if (entry.getLevel().equals(level)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
    
    /**
     * Exports all logs to a text file in the logs folder.
     */
    public static synchronized void exportLogsToFile() {
        if (initialized.get() == 0) {
            return;
        }
        
        try {
            System.out.println("Exporting logs to file...");
            
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path exportFile = logDir.resolve("exported_logs_" + dateStr + "_" + timestamp + ".txt");
            
            try (BufferedWriter writer = Files.newBufferedWriter(exportFile)) {
                writer.write("================================================================================\n");
                writer.write("                    LOG EXPORT - " + new Date() + "\n");
                writer.write("================================================================================\n");
                writer.write("Total Log Entries: " + inMemoryLogs.size() + "\n");
                writer.write("Total Logs Counted: " + totalLogs.get() + "\n");
                writer.write("Error Count: " + errorCount.get() + "\n");
                writer.write("Warning Count: " + warnCount.get() + "\n");
                writer.write("================================================================================\n\n");
                
                int entryNumber = 1;
                for (LogEntry entry : inMemoryLogs) {
                    writer.write(String.format("[%d] %s\n", entryNumber++, entry.toString()));
                }
                
                writer.write("\n================================================================================\n");
                writer.write("End of Log Export\n");
                writer.write("================================================================================\n");
            }
            
            System.out.println("Logs exported successfully to: " + exportFile.toAbsolutePath());
            System.out.println("Total entries exported: " + inMemoryLogs.size());
            
            if (logger != null) {
                logger.info("Logs exported to: " + exportFile.toString());
            }
            
        } catch (IOException e) {
            System.err.println("Failed to export logs: " + e.getMessage());
            e.printStackTrace();
            if (logger != null) {
                logger.severe("Failed to export logs: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shuts down the logger gracefully and exports logs.
     */
    public static synchronized void shutdown() {
        if (initialized.get() == 0) {
            return;
        }
        
        if (logger != null) {
            logger.info("Logger shutting down...");
        }
        
        exportLogsToFile();
        
        if (fileHandler != null) {
            fileHandler.close();
            fileHandler = null;
        }
        
        initialized.set(0);
    }
}
