package services.analytics;

import models.Student;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Console dashboard that shows live class statistics (mean, median, std dev, grade distribution)
 * using the same calculations as "View Class Statistics", with a background daemon thread that
 * periodically recalculates statistics while still allowing manual refresh.
 */
public class StatisticsDashboard {
    
    // Services and data (GradeService supplies the grade array used for statistics).
    private final services.file.GradeService gradeService;
    private final Collection<Student> students;
    private final int studentCount;
    
    // Background thread management
    private ScheduledExecutorService scheduler;
    private Future<?> statisticsTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    // Statistics cache: ConcurrentHashMap gives O(1) average get/put for live metric lookups.
    private final ConcurrentHashMap<String, Object> statsCache = new ConcurrentHashMap<>();
    
    // GPA rankings: TreeMap keeps students sorted by GPA descending; inserts and lookups are O(log n).
    private final TreeMap<Double, List<Student>> gpaRankings = new TreeMap<>(Collections.reverseOrder());
    
    // Refresh interval in seconds
    private static final int REFRESH_INTERVAL_SECONDS = 5;
    
    /**
     * Constructs a StatisticsDashboard with GradeService and the current student collection.
     */
    public StatisticsDashboard(services.file.GradeService gradeService,
                              Collection<Student> students, int studentCount) {
        this.gradeService = gradeService;
        this.students = students;
        this.studentCount = studentCount;
    }
    
    /** Starts the statistics dashboard and launches the auto-refresh background task. */
    public void start() {
        if (isRunning.get()) {
            System.out.println("Dashboard is already running.");
            return;
        }
        
        isRunning.set(true);
        isPaused.set(false);

        // Initial statistics before scheduling periodic recalculation.
        calculateAndCacheStatistics();

        // Single daemon thread recalculates statistics every REFRESH_INTERVAL_SECONDS seconds.
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StatisticsDashboard-Thread");
            t.setDaemon(true);
            return t;
        });

        statisticsTask = scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning.get() || isPaused.get()) {
                return; // Skip when dashboard stopped or paused.
            }
            calculateAndCacheStatistics();
        }, REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        System.out.println("Statistics Dashboard started (Auto-refresh every " + REFRESH_INTERVAL_SECONDS + "s).");
        System.out.println("Commands: [R]efresh now, [P]ause/Resume auto-refresh, [Q]uit dashboard.");
    }
    
    /**
     * Calculates statistics and updates cache using the same methods as "View Class Statistics".
     * Builds a fresh StatisticsService from current GradeService data on each call.
     */
    public void calculateAndCacheStatistics() {
        try {
            // Prevent overlapping calculations and mark dashboard as "loading".
            if (!isCalculating.compareAndSet(false, true)) {
                return;
            }

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
            
        } catch (Exception e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isCalculating.set(false);
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
        String threadStatus = getThreadStatus();
        
        System.out.println("Mode: Auto + Manual Refresh | Status: " + threadStatus);
        System.out.println("Press 'Q' to quit | 'R' to refresh now | 'P' to pause/resume auto-refresh");
        
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
        System.out.printf("│ Background Thread: %-44s │%n", threadStatus);
        System.out.printf("│ Calculation State: %-44s │%n", isCalculating.get() ? "LOADING..." : "Idle / Up-to-date");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        // Live Statistics
        System.out.println("┌─ LIVE STATISTICS ────────────────────────────────────────────────────┐");
        Integer totalGrades = (Integer) getFromCache("totalGrades");
        if (totalGrades != null) {
            System.out.printf("│ Total Grades: %-51d │%n", totalGrades);
        }
        
        Long calcTime = (Long) getFromCache("calculationTime");
        if (calcTime != null) {
            System.out.printf("│ Average Processing Time: %-42dms │%n", calcTime);
        }
        System.out.println("└────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        // Grade Distribution
        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) getFromCache("gradeDistribution");
        if (distribution != null) {
            System.out.println("┌─ GRADE DISTRIBUTION (Live) ────────────────────────────────────────┐");
            displayGradeDistribution(distribution, totalGrades != null ? totalGrades : 0);
            System.out.println("└────────────────────────────────────────────────────────────────────────┘");
            System.out.println();
        }
        
        // Current Statistics
        System.out.println("┌─ CURRENT STATISTICS ──────────────────────────────────────────────────┐");
        Double mean = (Double) getFromCache("mean");
        Double median = (Double) getFromCache("median");
        Double stdDev = (Double) getFromCache("stdDev");
        
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
        List<Map<String, Object>> topPerformers = (List<Map<String, Object>>) getFromCache("topPerformers");
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
        
        // Auto-refresh handled by background thread; no countdown needed here.
    }

    /**
     * Helper that reads from stats cache and updates hit/miss counters.
     */
    private Object getFromCache(String key) {
        Object value = statsCache.get(key);
        if (value != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return value;
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

