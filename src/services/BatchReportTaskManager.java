package services;

import models.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchReportTaskManager {
    private final ExecutorService executor;
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final int totalTasks;
    private final List<Student> students;
    private final GradeService gradeService;
    private final String reportType;
    private final String format;

    public BatchReportTaskManager(List<Student> students, GradeService gradeService, String reportType, String format, int threadCount) {
        this.students = students;
        this.gradeService = gradeService;
        this.reportType = reportType;
        this.format = format;
        this.totalTasks = students.size();
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public void startBatchExport() {
        long startTime = System.currentTimeMillis();
        List<Future<Long>> futures = new ArrayList<>();
        for (Student student : students) {
            futures.add(executor.submit(() -> {
                long t0 = System.currentTimeMillis();
                activeTasks.incrementAndGet();
                try {
                    String filename = student.getStudentID() + "_" + reportType;
                    if ("csv".equalsIgnoreCase(format)) {
                        gradeService.exportGradesCSV(filename);
                    } else if ("json".equalsIgnoreCase(format)) {
                        gradeService.exportGradesJSON(filename);
                    } else if ("binary".equalsIgnoreCase(format)) {
                        gradeService.exportGradesBinary(filename);
                    } else if ("all".equalsIgnoreCase(format)) {
                        gradeService.exportGradeReportMultiFormat(student, Integer.parseInt(reportType), filename);
                    }
                } catch (Exception e) {
                    System.out.println("Error exporting for " + student.getStudentID() + ": " + e.getMessage());
                }
                long t1 = System.currentTimeMillis();
                completedTasks.incrementAndGet();
                activeTasks.decrementAndGet();
                return t1 - t0;
            }));
        }

        // Progress bar loop
        while (completedTasks.get() < totalTasks) {
            showProgress();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        // Collect times
        long totalElapsed = System.currentTimeMillis() - startTime;
        int reportsPerSec = (int) (totalTasks / (totalElapsed / 1000.0));
        System.out.println("\nBatch Export Complete!");
        System.out.printf("Total Time: %.1f s\n", totalElapsed / 1000.0);
        System.out.printf("Throughput: %d reports/sec\n", reportsPerSec);

        shutdown();
    }

    private void showProgress() {
        int done = completedTasks.get();
        int active = activeTasks.get();
        int queue = totalTasks - done - active;
        System.out.printf("\rBackground Tasks: + %d active | %d queued | %d completed", active, queue, done);
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
