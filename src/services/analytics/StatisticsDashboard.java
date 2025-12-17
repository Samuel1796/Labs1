package services.analytics;

import models.Student;
import models.Grade;
import models.HonorsStudent;
import models.RegularStudent;
import services.file.GradeService;
import services.student.StudentService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Real-Time Statistics Dashboard with background thread for auto-refresh.
 * 
 * Implements US-5: Real-Time Statistics Dashboard with Background Thread.
 * 
 * Features:
 * - Background daemon thread calculating statistics every 5 seconds
 * - Auto-refreshing dashboard with live statistics
 * - Thread-safe data handling using ConcurrentHashMap
 * - Manual refresh, pause, and resume functionality
 * - Performance metrics (cache hit rate, memory usage)
 * - Thread status monitoring
 * 
 * Thread Safety:
 * - Uses ConcurrentHashMap for thread-safe statistics cache
 * - Atomic variables for counters and flags
 * - Synchronized blocks for complex operations
 * - Proper thread lifecycle management
 */
public class StatisticsDashboard {
    
    // Services and data
    private final services.file.GradeService gradeService;
    private final services.student.StudentService studentService;
    private final Collection<Student> students;
    private final int studentCount;
    
    // Background thread management
    private ScheduledExecutorService scheduler;
    private Future<?> statisticsTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    // Statistics cache: thread-safe storage for calculated statistics
    private final ConcurrentHashMap<String, Object> statsCache = new ConcurrentHashMap<>();
    
    // GPA rankings: TreeMap for automatic sorting by GPA (descending)
    private final TreeMap<Double, List<Student>> gpaRankings = new TreeMap<>(Collections.reverseOrder());
    
    // Refresh interval in seconds
    private static final int REFRESH_INTERVAL_SECONDS = 5;
    
    /**
     * Constructs a StatisticsDashboard with required services.
     * 
     * @param statisticsService Service for statistical calculations (kept for compatibility, but we'll recreate it)
     * @param gradeService Service for grade data access
     * @param students Collection of all students
     * @param studentCount Total number of students
     */
    public StatisticsDashboard(StatisticsService statisticsService, services.file.GradeService gradeService,
                              Collection<Student> students, int studentCount) {
        // Store services - we'll recreate StatisticsService with fresh data on each refresh
        this.gradeService = gradeService;
        this.students = students;
        this.studentCount = studentCount;
        // Get StudentService from students collection if needed, or pass it separately
        this.studentService = null; // Will be set if needed
    }
    
    /**
     * Alternative constructor that accepts StudentService.
     */
    public StatisticsDashboard(services.file.GradeService gradeService, services.student.StudentService studentService,
                              Collection<Student> students, int studentCount) {
        this.gradeService = gradeService;
        this.studentService = studentService;
        this.students = students;
        this.studentCount = studentCount;
    }
    
    /**
     * Starts the statistics dashboard (manual refresh only).
     * No auto-refresh - statistics are calculated only on manual refresh.
     */
    public void start() {
        if (isRunning.get()) {
            System.out.println("Dashboard is already running.");
            return;
        }
        
        isRunning.set(true);
        isPaused.set(false);
        
        // Calculate initial statistics
        calculateAndCacheStatistics();
        
        System.out.println("Statistics Dashboard started (Manual refresh only).");
        System.out.println("Press 'R' to refresh statistics, 'Q' to quit.");
    }
    
    /**
     * Calculates statistics and updates cache.
     * Uses the same approach as "View Class Statistics" - creates a fresh StatisticsService
     * with current data from GradeService on each refresh.
     * 
     * This ensures statistics are always up-to-date with the latest grade data.
     */
    public void calculateAndCacheStatistics() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Create fresh StatisticsService with current data (same as "View Class Statistics")
            // This ensures we always have the latest grades and students
            StatisticsService statsService = new StatisticsService(
                gradeService.getGrades(),
                gradeService.getGradeCount(),
                students,
                studentCount,
                gradeService
            );
            
            // Calculate all statistics using the same methods as view class statistics
            double mean = statsService.calculateMean();
            double median = statsService.calculateMedian();
            double stdDev = statsService.calculateStdDev();
            Map<String, Integer> gradeDistribution = statsService.getGradeDistribution();
            
            // Calculate GPA rankings
            updateGpaRankings();
            
            // Get top performers
            List<Map<String, Object>> topPerformers = getTopPerformers(3);
            
            // Update cache with thread-safe operations
            statsCache.put("mean", mean);
            statsCache.put("median", median);
            statsCache.put("stdDev", stdDev);
            statsCache.put("gradeDistribution", gradeDistribution);
            statsCache.put("topPerformers", topPerformers);
            statsCache.put("totalGrades", gradeService.getGradeCount());
            statsCache.put("totalStudents", studentCount);
            
            // Update timestamp
            lastUpdateTime.set(System.currentTimeMillis());
            
            long calculationTime = System.currentTimeMillis() - startTime;
            statsCache.put("calculationTime", calculationTime);
            
            // Cache hit/miss tracking (for demonstration)
            cacheMisses.incrementAndGet();
            
        } catch (Exception e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Updates GPA rankings using TreeMap for automatic sorting.
     * TreeMap maintains descending order (highest GPA first).
     */
    private void updateGpaRankings() {
        // Clear existing rankings
        gpaRankings.clear();
        
        // Calculate GPA for each student and group by GPA value
        // TreeMap automatically sorts by key (GPA) in descending order
        for (Student student : students) {
            double avg = student.calculateAverage(gradeService);
            if (avg > 0) { // Only include students with grades
                // Group students with same GPA together
                gpaRankings.computeIfAbsent(avg, k -> new ArrayList<>()).add(student);
            }
        }
    }
    
    /**
     * Gets top N performers based on GPA rankings.
     * 
     * @param count Number of top performers to return
     * @return List of top performers with their details
     */
    private List<Map<String, Object>> getTopPerformers(int count) {
        List<Map<String, Object>> performers = new ArrayList<>();
        int collected = 0;
        
        // Iterate through TreeMap (already sorted by GPA descending)
        for (Map.Entry<Double, List<Student>> entry : gpaRankings.entrySet()) {
            double gpa = entry.getKey();
            List<Student> studentsWithGpa = entry.getValue();
            
            // Add all students with this GPA
            for (Student student : studentsWithGpa) {
                if (collected >= count) break;
                
                Map<String, Object> performer = new HashMap<>();
                performer.put("studentId", student.getStudentID());
                performer.put("name", student.getName());
                performer.put("average", gpa);
                performer.put("gpa", convertToGPA(gpa));
                performers.add(performer);
                collected++;
            }
            
            if (collected >= count) break;
        }
        
        return performers;
    }
    
    /**
     * Converts percentage grade to 4.0 GPA scale.
     */
    private double convertToGPA(double grade) {
        if (grade >= 93) return 4.0;
        if (grade >= 90) return 3.7;
        if (grade >= 87) return 3.3;
        if (grade >= 83) return 3.0;
        if (grade >= 80) return 2.7;
        if (grade >= 77) return 2.3;
        if (grade >= 73) return 2.0;
        if (grade >= 70) return 1.7;
        if (grade >= 67) return 1.3;
        if (grade >= 60) return 1.0;
        return 0.0;
    }
    
    /**
     * Displays the real-time statistics dashboard.
     */
    public void displayDashboard() {
        clearScreen();
        
        // Header
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              REAL-TIME STATISTICS DASHBOARD                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Status bar
        String threadStatus = isRunning.get() ? "RUNNING" : "STOPPED";
        
        System.out.println("Mode: Manual Refresh Only | Status: " + threadStatus);
        System.out.println("Press 'Q' to quit | 'R' to refresh statistics");
        
        // Last update time
        long lastUpdate = lastUpdateTime.get();
        if (lastUpdate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("Last Updated: " + sdf.format(new Date(lastUpdate)));
        } else {
            System.out.println("Last Updated: Never");
        }
        System.out.println();
        
        // System Status
        System.out.println("┌─ SYSTEM STATUS ──────────────────────────────────────────────────────┐");
        System.out.printf("│ Total Students: %-50d │%n", studentCount);
        
        // Active threads
        int activeThreads = Thread.activeCount();
        System.out.printf("│ Active Threads: %-50d │%n", activeThreads);
        
        // Cache hit rate
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        double hitRate = (hits + misses > 0) ? (hits * 100.0 / (hits + misses)) : 0.0;
        System.out.printf("│ Cache Hit Rate: %-49.1f%% │%n", hitRate);
        
        // Memory usage (approximate)
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        System.out.printf("│ Memory Usage: %-52s │%n", 
            formatBytes(usedMemory) + " / " + formatBytes(totalMemory));
        System.out.println("└────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        // Live Statistics
        System.out.println("┌─ LIVE STATISTICS ────────────────────────────────────────────────────┐");
        Integer totalGrades = (Integer) statsCache.get("totalGrades");
        if (totalGrades != null) {
            System.out.printf("│ Total Grades: %-51d │%n", totalGrades);
        }
        
        Long calcTime = (Long) statsCache.get("calculationTime");
        if (calcTime != null) {
            System.out.printf("│ Average Processing Time: %-42dms │%n", calcTime);
        }
        System.out.println("└────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        // Grade Distribution
        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) statsCache.get("gradeDistribution");
        if (distribution != null) {
            System.out.println("┌─ GRADE DISTRIBUTION (Live) ────────────────────────────────────────┐");
            displayGradeDistribution(distribution, totalGrades != null ? totalGrades : 0);
            System.out.println("└────────────────────────────────────────────────────────────────────────┘");
            System.out.println();
        }
        
        // Current Statistics
        System.out.println("┌─ CURRENT STATISTICS ──────────────────────────────────────────────────┐");
        Double mean = (Double) statsCache.get("mean");
        Double median = (Double) statsCache.get("median");
        Double stdDev = (Double) statsCache.get("stdDev");
        
        if (mean != null) {
            System.out.printf("│ Mean: %-58.1f%% │%n", mean);
        }
        if (median != null) {
            System.out.printf("│ Median: %-56.1f%% │%n", median);
        }
        if (stdDev != null) {
            System.out.printf("│ Std Dev: %-55.1f%% │%n", stdDev);
        }
        System.out.println("└────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        // Top Performers
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topPerformers = (List<Map<String, Object>>) statsCache.get("topPerformers");
        if (topPerformers != null && !topPerformers.isEmpty()) {
            System.out.println("┌─ TOP PERFORMERS (Live Rankings) ───────────────────────────────────┐");
            int rank = 1;
            for (Map<String, Object> performer : topPerformers) {
                System.out.printf("│ %d. %s - %s - %.1f%% GPA: %.2f%n",
                    rank++,
                    performer.get("studentId"),
                    performer.get("name"),
                    performer.get("average"),
                    performer.get("gpa"));
            }
            System.out.println("└────────────────────────────────────────────────────────────────────────┘");
            System.out.println();
        }
        
        // Thread Pool Status
        if (scheduler != null) {
            System.out.println("┌─ THREAD POOL STATUS ──────────────────────────────────────────────────┐");
            if (scheduler instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) scheduler;
                System.out.printf("│ Scheduled Pool: %d tasks scheduled%n", 
                    ((ScheduledThreadPoolExecutor) scheduler).getQueue().size());
            }
            System.out.println("└────────────────────────────────────────────────────────────────────────┘");
            System.out.println();
        }
        
        // Manual refresh mode - no countdown needed
    }
    
    /**
     * Displays grade distribution with visual bars.
     */
    private void displayGradeDistribution(Map<String, Integer> distribution, int totalGrades) {
        String[] ranges = {"90-100% (A)", "80-89% (B)", "70-79% (C)", "60-69% (D)", "0-59% (F)"};
        String[] keys = {"90-100", "80-89", "70-79", "60-69", "0-59"};
        
        for (int i = 0; i < ranges.length; i++) {
            Integer count = distribution.get(keys[i]);
            if (count == null) count = 0;
            
            double percentage = totalGrades > 0 ? (count * 100.0 / totalGrades) : 0.0;
            
            // Create visual bar (simplified - 20 chars max)
            int barLength = totalGrades > 0 ? (int) (percentage / 5) : 0;
            String bar = "█".repeat(Math.min(barLength, 20));
            
            System.out.printf("│ %-15s %-20s %.1f%% (%d grades)%n", 
                ranges[i] + ":", bar, percentage, count);
        }
    }
    
    /**
     * Formats bytes to human-readable format.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Clears the console screen.
     */
    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing fails, just print newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    /**
     * Manually refreshes statistics (forces immediate calculation).
     */
    public void refresh() {
        if (!isRunning.get()) {
            System.out.println("Dashboard is not running. Start it first.");
            return;
        }
        
        System.out.println("Refreshing statistics...");
        calculateAndCacheStatistics();
        displayDashboard();
    }
    
    /**
     * Pauses or resumes the dashboard.
     */
    public void togglePause() {
        if (!isRunning.get()) {
            System.out.println("Dashboard is not running.");
            return;
        }
        
        boolean wasPaused = isPaused.getAndSet(!isPaused.get());
        System.out.println("Dashboard " + (wasPaused ? "resumed" : "paused"));
    }
    
    /**
     * Stops the dashboard and shuts down background thread.
     */
    public void stop() {
        if (!isRunning.get()) {
            return;
        }
        
        isRunning.set(false);
        isPaused.set(false);
        
        if (statisticsTask != null) {
            statisticsTask.cancel(true);
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Real-Time Statistics Dashboard stopped.");
    }
    
    /**
     * Gets the current thread status.
     */
    public String getThreadStatus() {
        if (!isRunning.get()) return "STOPPED";
        if (isPaused.get()) return "PAUSED";
        return "RUNNING";
    }
    
    /**
     * Checks if dashboard is running.
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}

