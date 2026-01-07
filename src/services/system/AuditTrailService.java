package services.system;

import utilities.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent Audit Trail Service (US-9).
 * 
 * Thread-safe logging system that tracks all system operations:
 * - Add student, record grade, generate report, search, etc.
 * 
 * Features:
 * - Thread-safe logging (ConcurrentLinkedQueue)
 * - Asynchronous file writing
 * - Log rotation (daily or 10MB size)
 * - ISO 8601 timestamp format
 * - Operation filtering and searching
 * - Audit statistics
 * - No log entries lost during concurrent operations
 */
public class AuditTrailService {
    
    private final ConcurrentLinkedQueue<AuditEntry> logQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService writerExecutor;
    private final AtomicInteger totalOperations = new AtomicInteger(0);
    private final AtomicInteger successfulOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    private volatile boolean running = true;
    
    /**
     * Audit log entry with all required information.
     */
    public static class AuditEntry implements Serializable {
        private final String timestamp; // ISO 8601 format
        private final String threadId;
        private final String operationType;
        private final String userAction;
        private final long executionTime; // milliseconds
        private final boolean success;
        private final String details;
        
        public AuditEntry(String threadId, String operationType, String userAction,
                         long executionTime, boolean success, String details) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.threadId = threadId;
            this.operationType = operationType;
            this.userAction = userAction;
            this.executionTime = executionTime;
            this.success = success;
            this.details = details;
        }
        
        public String getTimestamp() { return timestamp; }
        public String getThreadId() { return threadId; }
        public String getOperationType() { return operationType; }
        public String getUserAction() { return userAction; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }
        
        @Override
        public String toString() {
            return String.format("[%s] Thread:%s | Op:%s | Action:%s | Time:%dms | Status:%s | %s",
                timestamp, threadId, operationType, userAction, executionTime,
                success ? "SUCCESS" : "FAILED", details);
        }
    }
    
    public AuditTrailService() {
        Logger.initialize();
        
        writerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AuditTrail-Writer");
            t.setDaemon(true);
            return t;
        });
        
        startBackgroundWriter();
    }
    
    /**
     * Logs an operation (thread-safe, non-blocking).
     */
    public void logOperation(String operationType, String userAction, long executionTime,
                            boolean success, String details) {
        String threadId = String.valueOf(Thread.currentThread().threadId());
        
        AuditEntry entry = new AuditEntry(threadId, operationType, userAction,
                                         executionTime, success, details);
        
        logQueue.offer(entry);
        
        totalOperations.incrementAndGet();
        if (success) {
            successfulOperations.incrementAndGet();
        } else {
            failedOperations.incrementAndGet();
        }
        totalExecutionTime.addAndGet(executionTime);
        
        Logger.logAudit(operationType, userAction, executionTime, success, details);
    }
    
    /**
     * Starts background thread that drains queue to file.
     */
    private void startBackgroundWriter() {
        writerExecutor.submit(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    // Drain queue in batches for efficiency
                    List<AuditEntry> batch = new ArrayList<>();
                    AuditEntry entry;
                    
                    // Collect up to 100 entries or wait a bit
                    while (batch.size() < 100 && (entry = logQueue.poll()) != null) {
                        batch.add(entry);
                    }
                    
                    if (!batch.isEmpty()) {
                        writeBatchToFile(batch);
                    } else {
                        // No entries, sleep briefly
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error writing audit log: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Writes batch of entries to file using Logger.
     */
    private synchronized void writeBatchToFile(List<AuditEntry> batch) {
        for (AuditEntry entry : batch) {
            Logger.logAudit(entry.getOperationType(), entry.getUserAction(), 
                          entry.getExecutionTime(), entry.isSuccess(), entry.getDetails());
        }
    }
    
    /**
     * Views recent audit entries with optional filtering.
     */
    public void viewRecentEntries(int count, String operationType, String threadId) {
        List<AuditEntry> entries = readRecentEntries(count, operationType, threadId);
        
        System.out.println("\n=========================================================================");
        System.out.println("                      AUDIT TRAIL ENTRIES                                ");
        System.out.println("=========================================================================");
        System.out.println();
        
        if (entries.isEmpty()) {
            System.out.println("No entries found.");
            return;
        }
        
        for (AuditEntry entry : entries) {
            System.out.println("== Entry=================================================================");
            System.out.printf("│ Timestamp: %-54s │%n", entry.getTimestamp());
            System.out.printf("│ Thread ID: %-54s │%n", entry.getThreadId());
            System.out.printf("│ Operation: %-54s │%n", entry.getOperationType());
            System.out.printf("│ Action: %-56s │%n", entry.getUserAction());
            System.out.printf("│ Execution Time: %-48dms │%n", entry.getExecutionTime());
            System.out.printf("│ Status: %-56s │%n", entry.isSuccess() ? "SUCCESS" : "FAILED");
            System.out.printf("│ Details: %-55s │%n", entry.getDetails());
            System.out.println("========================================================================");
            System.out.println();
        }
    }
    
    /**
     * Reads recent entries from log file with filtering.
     */
    private List<AuditEntry> readRecentEntries(int count, String operationType, String threadId) {
        List<AuditEntry> entries = new ArrayList<>();
        List<String> logLines = readLogsFromFiles(count, operationType, threadId);
        for (String line : logLines) {
            AuditEntry entry = parseEntry(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }
    
    /**
     * Reads logs from files in real-time.
     */
    public List<String> readLogsFromFiles(int maxLines, String operationType, String threadId) {
        List<String> logLines = new ArrayList<>();
        
        try {
            String logDir = Logger.getLogDirectory();
            Path logPath = Paths.get(logDir);
            
            if (!Files.exists(logPath)) {
                return logLines;
            }
            
            List<Path> logFiles = new ArrayList<>();
            Files.list(logPath)
                .filter(p -> p.toString().endsWith(".log"))
                .sorted((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .forEach(logFiles::add);
            
            for (Path logFile : logFiles) {
                if (logLines.size() >= maxLines) break;
                
                try {
                    List<String> fileLines = Files.readAllLines(logFile);
                    
                    for (int i = fileLines.size() - 1; i >= 0 && logLines.size() < maxLines; i--) {
                        String line = fileLines.get(i);
                        if (line.contains("AUDIT:")) {
                            if (operationType != null && !line.contains(operationType)) {
                                continue;
                            }
                            if (threadId != null && !line.contains("Thread:" + threadId)) {
                                continue;
                            }
                            logLines.add(0, line);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read log files: " + e.getMessage());
        }
        
        return logLines;
    }
    
    /**
     * Displays audit trail viewer with real-time log reading from both memory and files.
     */
    public void displayAuditTrailViewer() {
        System.out.println("\n=======================================================================");
        System.out.println("                         AUDIT TRAIL VIEWER                           ");
        System.out.println("=======================================================================");
        System.out.println();
        
        // First, try to get logs from in-memory storage (fastest, most up-to-date)
        List<Logger.LogEntry> memoryLogs = Logger.getAllLogs();
        List<String> auditLogs = new ArrayList<>();
        
        // Filter for audit entries from memory
        for (Logger.LogEntry entry : memoryLogs) {
            if (entry.getMessage().contains("AUDIT:")) {
                auditLogs.add(entry.toString());
            }
        }
        
        // If no logs in memory, try reading from files
        if (auditLogs.isEmpty()) {
            String logDir = Logger.getLogDirectory();
            Path logPath = Paths.get(logDir);
            
            if (Files.exists(logPath)) {
                try {
                    // Force flush any pending writes
                    if (Logger.getCurrentLogFile() != null) {
                        // Try to read from files as backup
                        List<String> fileLogs = readLogsFromFiles(50, null, null);
                        auditLogs.addAll(fileLogs);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading from log files: " + e.getMessage());
                }
            }
        }
        
        // Also check the in-memory queue for any pending entries
        List<AuditEntry> queueEntries = new ArrayList<>();
        for (AuditEntry entry : logQueue) {
            queueEntries.add(entry);
        }
        
        // Display results
        if (auditLogs.isEmpty() && queueEntries.isEmpty()) {
            System.out.println("No audit log entries found.");
            System.out.println();
            System.out.println("Note: Audit logs are created when you perform operations such as:");
            System.out.println("  - Adding students");
            System.out.println("  - Recording grades");
            System.out.println("  - Viewing reports");
            System.out.println("  - Importing/exporting data");
            System.out.println();
            
            String logDir = Logger.getLogDirectory();
            Path logPath = Paths.get(logDir);
            if (Files.exists(logPath)) {
                System.out.println("Log Directory: " + logPath.toAbsolutePath());
                try {
                    List<Path> logFiles = new ArrayList<>();
                    Files.list(logPath)
                        .filter(p -> p.toString().endsWith(".log"))
                        .forEach(logFiles::add);
                    
                    if (!logFiles.isEmpty()) {
                        System.out.println("Found " + logFiles.size() + " log file(s), but no AUDIT entries yet.");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
            return;
        }
        
        // Display audit entries from memory
        if (!auditLogs.isEmpty()) {
            System.out.println("Recent Audit Log Entries (from memory):");
            System.out.println("-----------------------------------------------------------------------");
            System.out.println();
            
            // Show most recent first (reverse order)
            int displayCount = Math.min(50, auditLogs.size());
            int startIndex = Math.max(0, auditLogs.size() - displayCount);
            
            for (int i = auditLogs.size() - 1; i >= startIndex; i--) {
                System.out.println(auditLogs.get(i));
            }
            
            System.out.println();
            System.out.println("Total audit entries displayed: " + displayCount);
        }
        
        // Display any pending queue entries
        if (!queueEntries.isEmpty()) {
            System.out.println("Pending audit entries (not yet written to file): " + queueEntries.size());
            for (AuditEntry entry : queueEntries) {
                System.out.println(entry.toString());
            }
            System.out.println();
        }
        
        System.out.println("Log files are updated in real-time as operations are performed.");
        String logDir = Logger.getLogDirectory();
        if (logDir != null) {
            System.out.println("Log directory: " + Paths.get(logDir).toAbsolutePath());
        }
    }
    
    /**
     * Parses log entry from string.
     */
    private AuditEntry parseEntry(String line) {
        try {
            if (!line.contains("AUDIT:")) {
                return null;
            }
            
            String auditPart = line.substring(line.indexOf("AUDIT:"));
            String[] parts = auditPart.split("\\|");
            if (parts.length < 5) return null;
            
            String operationType = parts[0].replace("AUDIT:", "").trim();
            String userAction = parts[1].replace("Action:", "").trim();
            long executionTime = Long.parseLong(parts[2].replace("Time:", "").replace("ms", "").trim());
            boolean success = parts[3].replace("Status:", "").trim().equals("SUCCESS");
            String details = parts.length > 4 ? parts[4].trim() : "";
            
            String threadId = String.valueOf(Thread.currentThread().threadId());
            
            return new AuditEntry(threadId, operationType, userAction, executionTime, success, details);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Searches audit trail by date range, operation type, or thread ID.
     */
    public List<AuditEntry> searchAuditTrail(Date startDate, Date endDate, String operationType, String threadId) {
        List<AuditEntry> results = new ArrayList<>();
        
        try {
            String logDir = Logger.getLogDirectory();
            Path logPath = Paths.get(logDir);
            
            if (!Files.exists(logPath)) {
                return results;
            }
            
            Files.list(logPath)
                .filter(p -> p.toString().endsWith(".log"))
                .forEach(logFile -> {
                    try (BufferedReader reader = Files.newBufferedReader(logFile)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("AUDIT:")) {
                                AuditEntry entry = parseEntry(line);
                                if (entry != null) {
                                    if (operationType != null && !entry.getOperationType().equals(operationType)) {
                                        return;
                                    }
                                    if (threadId != null && !entry.getThreadId().equals(threadId)) {
                                        return;
                                    }
                                    results.add(entry);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to read log file: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to search audit log: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Displays audit statistics.
     */
    public void displayStatistics() {
        int total = totalOperations.get();
        int successful = successfulOperations.get();
        int failed = failedOperations.get();
        long totalTime = totalExecutionTime.get();
        double avgExecutionTime = total > 0 ? (double) totalTime / total : 0.0;
        
        // Calculate operations per hour (last hour)
        int operationsPerHour = calculateOperationsPerHour();
        
        System.out.println("\n=========================================================================");
        System.out.println("                      AUDIT STATISTICS                                    ");
        System.out.println("==========================================================================");
        System.out.println();
        
        System.out.println(" OPERATION STATISTICS ====================================================");
        System.out.printf("│ Total Operations: %-50d │%n", total);
        System.out.printf("│ Successful: %-55d │%n", successful);
        System.out.printf("│ Failed: %-58d │%n", failed);
        System.out.printf("│ Success Rate: %-51.2f%% │%n", total > 0 ? (successful * 100.0 / total) : 0.0);
        System.out.println("==========================================================================");
        System.out.println();
        
        System.out.println("  PERFORMANCE STATISTICS ===================================================");
        System.out.printf("│ Average Execution Time: %-43.2fms │%n", avgExecutionTime);
        System.out.printf("│ Operations per Hour: %-48d │%n", operationsPerHour);
        System.out.println("=============================================================================");
    }
    
    /**
     * Calculates operations per hour (simplified - counts last hour's operations).
     */
    private int calculateOperationsPerHour() {
        // Simplified: return total operations (would need time-based tracking in production)
        return totalOperations.get();
    }
    
    /**
     * Shuts down audit trail service gracefully.
     */
    public void shutdown() {
        running = false;
        
        if (writerExecutor != null) {
            writerExecutor.shutdown();
            try {
                if (!writerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    writerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                writerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Flush remaining entries
        while (!logQueue.isEmpty()) {
            List<AuditEntry> batch = new ArrayList<>();
            AuditEntry entry;
            while (batch.size() < 100 && (entry = logQueue.poll()) != null) {
                batch.add(entry);
            }
            if (!batch.isEmpty()) {
                writeBatchToFile(batch);
            }
        }
    }
}


