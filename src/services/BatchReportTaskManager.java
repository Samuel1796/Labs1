package services;

import models.Student;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchReportTaskManager {
    private final ExecutorService executor;
    private final List<Student> students;
    private final GradeService gradeService;
    private final int format;
    private final String outputDir;
    private final int totalTasks;
    private final int threadCount;
    private final ConcurrentHashMap<String, String> threadStatus = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> reportTimes = new ConcurrentHashMap<>();
    private final AtomicInteger submittedTasks = new AtomicInteger(0);
    private final AtomicInteger startedTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    public BatchReportTaskManager(List<Student> students, GradeService gradeService, int format, String outputDir, int threadCount) {
        this.students = students;
        this.gradeService = gradeService;
        this.format = format;
        this.outputDir = outputDir;
        this.totalTasks = students.size();
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
        // Ensure output directory exists
        new java.io.File(outputDir).mkdirs();
    }

    public void startBatchExport() {
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = new ArrayList<>();
        for (Student student : students) {
            futures.add(executor.submit(() -> {
                String threadName = Thread.currentThread().getName();
                threadStatus.put(student.getStudentID(), "in progress (" + threadName + ")");
                long t0 = System.currentTimeMillis();
                boolean success = false;
                try {
                    String baseFilename = outputDir + "/" + student.getStudentID();
                    List<String> generatedFiles = new ArrayList<>();
                    switch (format) {
                        case 1: // CSV
                            String csvPath = baseFilename + ".csv";
                            gradeService.exportGradeReport(student, 1, baseFilename);
                            generatedFiles.add(csvPath);
                            break;
                        case 2: // JSON
                            String jsonPath = baseFilename + ".json";
                            gradeService.exportGradeReport(student, 2, baseFilename);
                            generatedFiles.add(jsonPath);
                            break;
                        case 3: // Binary
                            String binPath = baseFilename + ".dat";
                            gradeService.exportGradeReport(student, 3, baseFilename);
                            generatedFiles.add(binPath);
                            break;
                        case 4: // All formats
                            String[] subdirs = {"csv", "json", "binary"};
                            String[] extensions = {".csv", ".json", ".dat"};
                            for (int i = 0; i < subdirs.length; i++) {
                                String dirPath = outputDir + "/" + subdirs[i];
                                String filePath = dirPath + "/" + student.getStudentID() + extensions[i];
                                generatedFiles.add(filePath);
                            }
                            gradeService.exportGradeReportMultiFormat(student, 2, baseFilename);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown format: " + format);
                    }
                    // Wait for files to be generated
                    boolean allExist = true;
                    for (String filePath : generatedFiles) {
                        java.io.File f = new java.io.File(filePath);
                        int waitCount = 0;
                        while (!f.exists() && waitCount < 20) { // Wait up to 2 seconds
                            Thread.sleep(100);
                            waitCount++;
                        }
                        if (!f.exists()) {
                            allExist = false;
                            break;
                        }
                    }
                    success = allExist;
                    threadStatus.put(student.getStudentID(), success ? "completed (" + threadName + ")" : "failed (" + threadName + ")");
                } catch (Exception e) {
                    threadStatus.put(student.getStudentID(), "failed (" + threadName + ")");
                    failedTasks.incrementAndGet();
                    System.out.println("Export failed for " + student.getStudentID() + ": " + e.getMessage());
                }
                long t1 = System.currentTimeMillis();
                reportTimes.put(student.getStudentID(), t1 - t0);
                completedTasks.incrementAndGet();
            }));
        }
    
        // Progress bar loop
        while (completedTasks.get() < totalTasks) {
            showProgress(startTime);
            showBackgroundTaskCount();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    
        showProgress(startTime); // Final update
        showBackgroundTaskCount();
        System.out.println("\nBATCH GENERATION COMPLETED!");
        showSummary(startTime);
        shutdown();
    }

    private void showBackgroundTaskCount() {
        int active = getActiveTasks();
        System.out.printf("\nBackground tasks running: %d\n", active);
    }

    public int getActiveTasks() {
        int active = 0;
        for (String status : threadStatus.values()) {
            if (status.startsWith("in progress")) active++;
        }
        return active;
    }

    private void showProgress(long startTime) {
        int done = completedTasks.get();
        int active = threadCount - ((ThreadPoolExecutor) executor).getPoolSize();
        int queue = totalTasks - done - active;
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        double avgTime = reportTimes.values().stream().mapToLong(Long::longValue).average().orElse(0.0) / 1000.0;
        double throughput = done / (elapsed > 0 ? elapsed : 1);

        System.out.printf("\rProgress: [%d/%d] | Elapsed: %.1fs | Avg Report Time: %.0fms | Throughput: %.2f reports/sec",
            done, totalTasks, elapsed, avgTime * 1000, throughput);

        // Per-thread status
        for (String id : threadStatus.keySet()) {
            System.out.printf("\n%s: %s", id, threadStatus.get(id));
        }
    }

    private void showSummary(long startTime) {
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        int successCount = totalTasks - failedTasks.get();
        System.out.println("EXECUTION SUMMARY");
        System.out.println("Total Reports: " + totalTasks);
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failedTasks.get());
        System.out.printf("Total Time: %.1f seconds\n", elapsed);
        System.out.printf("Avg Time per Report: %.0fms\n", reportTimes.values().stream().mapToLong(Long::longValue).average().orElse(0.0));
        System.out.println("Thread Pool Statistics:");
        System.out.println("Peak Thread Count: " + threadCount);
        System.out.println("Total Tasks Executed: " + totalTasks);
        System.out.println("Output Location: " + outputDir);
        System.out.println("Total Files Generated: " + successCount);
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
            System.out.println("Total Size: " + formatFileSize(totalSize));
        } catch (java.io.IOException e) {
            // Ignore size calculation error
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    public boolean isRunning() {
        return completedTasks.get() < totalTasks;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}