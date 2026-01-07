package services.file;

import models.Student;
import models.Grade;
import utilities.FileIOUtils;
import utilities.Logger;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
// Jackson dependency - uncomment when Jackson library is added to classpath
// import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages concurrent batch grade report generation using a fixed thread pool,
 * tracking per-student status, timings, and printing a real-time progress summary.
 */
public class BatchReportTaskManager {
    // Thread pool executor: manages worker threads for concurrent report generation
    // Fixed pool size prevents thread explosion and resource exhaustion
    private final ExecutorService executor;
    
    // Input data: list of students to generate reports for
    private final List<Student> students;
    
    // Service dependency: provides grade data and export functionality
    private final GradeImportExportService gradeImportExportService;
    
    // Export configuration
    private final int format;              // 1: CSV, 2: JSON, 3: Binary, 4: All formats
    private final String outputDir;       // Base directory for output files
    private final int totalTasks;         // Total number of reports to generate
    private final int threadCount;        // Number of worker threads
    
    // Thread-safe status tracking: maps student ID to current processing status
    // ConcurrentHashMap allows concurrent reads/writes without blocking
    private final ConcurrentHashMap<String, String> threadStatus = new ConcurrentHashMap<>();
    
    // Performance metrics: tracks time taken for each report generation
    // Used for calculating average throughput and identifying slow operations
    private final ConcurrentHashMap<String, Long> reportTimes = new ConcurrentHashMap<>();
    
    // Atomic counters for progress tracking: thread-safe without explicit locking
    // AtomicInteger uses compare-and-swap (CAS) operations for lock-free updates
    private final AtomicInteger submittedTasks = new AtomicInteger(0);
    private final AtomicInteger startedTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    public BatchReportTaskManager(List<Student> students, GradeImportExportService gradeImportExportService, int format, String outputDir, int threadCount) {
        this.students = students;
        this.gradeImportExportService = gradeImportExportService;
        this.format = format;
        this.outputDir = outputDir;
        this.totalTasks = students.size();
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
        new java.io.File(outputDir).mkdirs();
        Logger.info("BATCH_REPORT_MANAGER: Initialized with " + totalTasks + " tasks, " + threadCount + " threads");
    }

    /** Starts the concurrent batch export for all students and waits until all reports finish. */
    public void startBatchExport() {
        long startTime = System.currentTimeMillis();
        Logger.info("BATCH_EXPORT: Starting batch export for " + totalTasks + " students");
        List<Future<?>> futures = new ArrayList<>();
        
        for (Student student : students) {
            futures.add(executor.submit(() -> {
                // Task execution context: runs in worker thread
                String threadName = Thread.currentThread().getName();
                threadStatus.put(student.getStudentID(), "in progress (" + threadName + ")");
                long t0 = System.currentTimeMillis();
                boolean success = false;
                
                try {
                    // Construct base filename: outputDir + studentID
                    String baseFilename = outputDir + "/" + student.getStudentID();
                    List<String> generatedFiles = new ArrayList<>();
                    
                    // Format-specific export logic
                    // Each format generates different file structures
                    switch (format) {
                        case 1: // CSV format: single file
                            String csvPath = baseFilename + ".csv";
                            exportStudentReportCSV(student, csvPath);
                            generatedFiles.add(csvPath);
                            break;
                        case 2: // JSON format: single file
                            String jsonPath = baseFilename + ".json";
                            exportStudentReportJSON(student, jsonPath);
                            generatedFiles.add(jsonPath);
                            break;
                        case 3: // Binary format: single file
                            String binPath = baseFilename + ".dat";
                            exportStudentReportBinary(student, binPath);
                            generatedFiles.add(binPath);
                            break;
                        case 4: // All formats: generates files in subdirectories
                            String[] subdirs = {"csv", "json", "binary"};
                            String[] extensions = {".csv", ".json", ".dat"};
                            // Track expected file paths for verification
                            for (int i = 0; i < subdirs.length; i++) {
                                String dirPath = outputDir + "/" + subdirs[i];
                                String filePath = dirPath + "/" + student.getStudentID() + extensions[i];
                                generatedFiles.add(filePath);
                            }
                            gradeImportExportService.exportGradeReportMultiFormat(student, 2, baseFilename);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown format: " + format);
                    }
                    
                    // File existence verification: handle async file system operations
                    // Some file systems may delay file visibility after write completion
                    // Polling ensures we detect files even if there's a slight delay
                    boolean allExist = true;
                    for (String filePath : generatedFiles) {
                        java.io.File f = new java.io.File(filePath);
                        int waitCount = 0;
                        // Poll up to 5 seconds (50 * 100ms) for file to appear
                        while (!f.exists() && waitCount < 50) {
                            Thread.sleep(100); // Sleep 100ms between checks
                            waitCount++;
                        }
                        if (!f.exists()) {
                            allExist = false;
                            System.err.println("ERROR: File not found after creation: " + filePath);
                            System.err.println("  Absolute path: " + f.getAbsolutePath());
                            System.err.println("  Parent exists: " + (f.getParentFile() != null && f.getParentFile().exists()));
                            break; // One file missing = overall failure
                        }
                    }
                    success = allExist;
                    
                    // Update status: completed or failed
                    threadStatus.put(student.getStudentID(), success ? "completed (" + threadName + ")" : "failed (" + threadName + ")");
                } catch (Exception e) {
                    threadStatus.put(student.getStudentID(), "failed (" + threadName + ")");
                    failedTasks.incrementAndGet();
                    long duration = System.currentTimeMillis() - t0;
                    Logger.error("BATCH_EXPORT: Export failed for " + student.getStudentID() + " - " + e.getMessage(), e);
                    Logger.logAudit("BATCH_EXPORT", "Export report for " + student.getStudentID(), duration, false, 
                        "Error: " + e.getMessage());
                }
                
                long t1 = System.currentTimeMillis();
                long duration = t1 - t0;
                reportTimes.put(student.getStudentID(), duration);
                completedTasks.incrementAndGet();
                if (success) {
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("collectionSize", totalTasks);
                    metrics.put("collectionName", "students");
                    Logger.logPerformanceWithCollections("BATCH_EXPORT_STUDENT", duration, totalTasks, "students", executor);
                    Logger.logAudit("BATCH_EXPORT", "Export report for " + student.getStudentID(), duration, true, 
                        "Report generated successfully");
                }
            }));
        }
    
        // Progress monitoring loop: runs in main thread
        // Polls completion status every 50ms for real-time updates
        // This provides real-time feedback without blocking worker threads
        while (completedTasks.get() < totalTasks) {
            showProgress(startTime);
            int active = getActiveTasks();
            if (active > 0) {
                System.out.printf("\nBackground tasks running: %d", active);
                System.out.flush();
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) {} // 50ms for smoother real-time updates
        }
    
        showProgress(startTime);
        System.out.println();
        System.out.println("BATCH GENERATION COMPLETED!");
        long totalDuration = System.currentTimeMillis() - startTime;
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalTasks", totalTasks);
        metrics.put("completedTasks", completedTasks.get());
        metrics.put("failedTasks", failedTasks.get());
        metrics.put("threadCount", threadCount);
        Logger.logPerformance("BATCH_EXPORT_ALL", totalDuration, metrics);
        Logger.logAudit("BATCH_EXPORT_ALL", "Batch export completed", totalDuration, true, 
            "Total tasks: " + totalTasks + ", Completed: " + completedTasks.get() + ", Failed: " + failedTasks.get());
        showSummary(startTime);
        shutdown();
    }
    
    /**
     * Exports a single student's grades to CSV format.
     */
    private void exportStudentReportCSV(Student student, String filePath) throws IOException {
        // Ensure directory exists
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        List<Grade> studentGrades = getStudentGrades(student);
        
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write("gradeID,studentID,subjectName,subjectType,value,date\n");
            for (Grade g : studentGrades) {
                writer.write(String.format("%s,%s,%s,%s,%.1f,%s\n",
                    g.getGradeID(), g.getStudentID(), g.getSubjectName(),
                    g.getSubjectType(), g.getValue(), sdf.format(g.getDate())));
            }
            writer.flush(); // Ensure data is written immediately
        }
        // Verify file was created
        if (!new File(filePath).exists()) {
            throw new IOException("File was not created: " + filePath);
        }
    }
    
    /**
     * Exports a single student's grades to JSON format.
     */
    private void exportStudentReportJSON(Student student, String filePath) throws IOException {
        // Ensure directory exists
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        List<Grade> studentGrades = getStudentGrades(student);
        List<Map<String, Object>> formattedGrades = new ArrayList<>();
        
        for (Grade g : studentGrades) {
            Map<String, Object> map = new HashMap<>();
            map.put("gradeID", g.getGradeID());
            map.put("studentID", g.getStudentID());
            map.put("subjectName", g.getSubjectName());
            map.put("subjectType", g.getSubjectType());
            map.put("value", g.getValue());
            map.put("date", sdf.format(g.getDate()));
            formattedGrades.add(map);
        }
        
        // Note: JSON export requires Jackson library
        throw new UnsupportedOperationException("JSON export requires Jackson library. Please add com.fasterxml.jackson.core:jackson-databind to classpath.");
        /* Uncomment when Jackson is available:
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write(mapper.writeValueAsString(formattedGrades));
            writer.flush(); // Ensure data is written immediately
        }
        // Verify file was created
        File createdFile = new File(filePath);
        if (!createdFile.exists()) {
            throw new IOException("File was not created: " + filePath);
        }
        */
    }
    
    /**
     * Exports a single student's grades to Binary format.
     */
    private void exportStudentReportBinary(Student student, String filePath) throws IOException {
        // Ensure directory exists
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        List<Grade> studentGrades = getStudentGrades(student);
        FileIOUtils.writeGradesToBinary(Paths.get(filePath), studentGrades);
        
        // Verify file was created
        File createdFile = new File(filePath);
        if (!createdFile.exists()) {
            throw new IOException("File was not created: " + filePath);
        }
    }
    
    /**
     * Gets all grades for a specific student.
     */
    private List<Grade> getStudentGrades(Student student) {
        return gradeImportExportService.getStudentGrades(student);
    }

    private void showBackgroundTaskCount() {
        int active = getActiveTasks();
        if (active > 0) {
            System.out.printf("\nBackground tasks running: %d", active);
            System.out.flush(); // Flush for real-time display
        }
    }

    public int getActiveTasks() {
        int active = 0;
        for (String status : threadStatus.values()) {
            if (status.startsWith("in progress")) active++;
        }
        return active;
    }

    /** Prints a single real-time progress line (percentage, timings, throughput) for the batch run. */
    private void showProgress(long startTime) {
        // Get current completion status (atomic read)
        int done = completedTasks.get();
        
        // Calculate active threads: thread pool size indicates available threads
        // Active = total threads - available threads
        int active = threadCount - ((ThreadPoolExecutor) executor).getPoolSize();
        
        // Calculate queued tasks: total - completed - currently active
        int queue = totalTasks - done - active;
        
        // Calculate elapsed time in seconds
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        
        // Calculate average report generation time
        // Stream API processes all completed report times and computes mean
        double avgTime = reportTimes.values().stream().mapToLong(Long::longValue).average().orElse(0.0) / 1000.0;
        
        // Calculate throughput: reports per second
        // Avoid division by zero with conditional check
        double throughput = done / (elapsed > 0 ? elapsed : 1);

        // Calculate estimated remaining time
        double estimatedRemaining = done > 0 ? (avgTime * 1000) * (totalTasks - done) / 1000.0 : 0;
        
        // Progress bar (40 characters wide)
        int progressBarWidth = 40;
        int filled = (int) ((double) done / totalTasks * progressBarWidth);
        String progressBar = "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, progressBarWidth - filled));
        double percentage = (double) done / totalTasks * 100;
        
        // Clear previous line and display progress with flush for real-time updates
        System.out.print("\r");
        System.out.printf("Progress: [%s] %d%% (%d/%d completed) | Elapsed: %.1fs | Est. Remaining: %.1fs | Avg: %.0fms | Throughput: %.2f reports/sec",
            progressBar, (int)percentage, done, totalTasks, elapsed, estimatedRemaining, avgTime * 1000, throughput);
        System.out.flush(); // Force immediate output for real-time progress
        
        // Per-thread status (display on new lines, limit to avoid clutter)
        if (done < totalTasks && done < 10) {
            System.out.println();
            int displayed = 0;
            for (String id : threadStatus.keySet()) {
                if (displayed++ >= 5) break; // Limit display
                String status = threadStatus.get(id);
                if (status.contains("in progress")) {
                    System.out.printf("  %s: %s%n", id, status);
                }
            }
        }
    }

    /** Prints a final execution summary with success counts, timing, throughput and file details. */
    private void showSummary(long startTime) {
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        int successCount = totalTasks - failedTasks.get();
        
        System.out.println("\n=======================================================================");
        System.out.println("                         EXECUTION SUMMARY                              ");
        System.out.println("========================================================================");
        System.out.println();
        
        // Basic Statistics
        System.out.println(" BATCH PROCESSING RESULTS =============================================");
        System.out.printf("│ Total Reports: %-50d │%n", totalTasks);
        System.out.printf("│ Successful: %-53d │%n", successCount);
        System.out.printf("│ Failed: %-56d │%n", failedTasks.get());
        System.out.println("=========================================================================");
        System.out.println();
        
        // Time Statistics
        double avgTime = reportTimes.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
        double estimatedSequential = avgTime * totalTasks / 1000.0; // Convert to seconds
        double performanceGain = estimatedSequential > 0 ? estimatedSequential / elapsed : 1.0;
        
        System.out.println("===TIME STATISTICS========================================================");
        System.out.printf("│ Total Time: %-52.1f seconds │%n", elapsed);
        System.out.printf("│ Avg Time per Report: %-42.0fms │%n", avgTime);
        System.out.printf("│ Sequential Processing (estimated): %-30.1f seconds │%n", estimatedSequential);
        System.out.printf("│ Concurrent Processing (actual): %-33.1f seconds │%n", elapsed);
        System.out.printf("│ Performance Gain: %-47.1fx faster │%n", performanceGain);
        System.out.println("==========================================================================");
        System.out.println();
        
        // Throughput
        double throughput = totalTasks / (elapsed > 0 ? elapsed : 1);
        System.out.println("===THROUGHPUT METRICS========================================================");
        System.out.printf("│ Reports per Second: %-45.2f │%n", throughput);
        System.out.println("=============================================================================");
        System.out.println();
        
        // Thread Pool Statistics
        System.out.println("===THREAD POOL STATISTICS========================================================");
        System.out.printf("│ Peak Thread Count: %-49d │%n", threadCount);
        System.out.printf("│ Total Tasks Executed: %-46d │%n", totalTasks);
        
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            System.out.printf("│ Active Threads: %-52d │%n", tpe.getActiveCount());
            System.out.printf("│ Completed Tasks: %-50d │%n", tpe.getCompletedTaskCount());
            System.out.printf("│ Queue Size: %-55d │%n", tpe.getQueue().size());
        }
        System.out.println("================================================================================");
        System.out.println();
        
        // File Information
        System.out.println("===FILE GENERATION DETAILS========================================================");
        System.out.printf("│ Output Location: %-50s │%n", outputDir);
        System.out.printf("│ Total Files Generated: %-45d │%n", successCount);
        
        try {
            long totalSize = java.nio.file.Files.walk(java.nio.file.Paths.get(outputDir))
                .filter(java.nio.file.Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return java.nio.file.Files.size(p);
                    } catch (java.io.IOException e) {
                        return 0;
                    }
                })
                .sum();
            System.out.printf("│ Total Size: %-54s │%n", formatFileSize(totalSize));
        } catch (java.io.IOException e) {
            // Ignore size calculation error
        }
        System.out.println("=============================================================================");
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    public boolean isRunning() {
        return completedTasks.get() < totalTasks;
    }

    /** Shuts down the executor service gracefully, forcing shutdown if it does not terminate in time. */
    public void shutdown() {
        // Phase 1: Initiate graceful shutdown
        // Stops accepting new tasks but allows running tasks to complete
        executor.shutdown();
        
        try {
            // Phase 2: Wait for tasks to complete (with timeout)
            // awaitTermination blocks until all tasks finish or timeout occurs
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                // Timeout occurred: force shutdown
                // shutdownNow() interrupts all running tasks and returns list of pending tasks
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Current thread was interrupted: force shutdown immediately
            // This prevents hanging if the waiting thread is interrupted
            executor.shutdownNow();
            // Restore interrupt status for proper exception propagation
            Thread.currentThread().interrupt();
        }
    }
}