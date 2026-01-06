package services.system;

import models.Student;
import services.analytics.StatisticsService;
import services.file.GradeService;
import utilities.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.io.*;

/**
 * Manages recurring background tasks (GPA recalculation, stats refresh, backups, etc.)
 * using a ScheduledExecutorService with basic persistence and execution history.
 */
public class TaskScheduler {
    
    private ScheduledExecutorService scheduler;
    // ConcurrentHashMap gives O(1) average add/get for tasks by ID across threads.
    private final Map<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();
    // PriorityQueue orders tasks by next execution time so the nearest task can be peeked in O(1), updated in O(log n).
    private final PriorityQueue<ScheduledTask> taskQueue =
            new PriorityQueue<>(Comparator.comparing(ScheduledTask::getNextExecution, Comparator.nullsLast(Date::compareTo)));
    // Synchronized List keeps execution history in insertion order; append is O(1) amortized.
    private final List<TaskExecution> executionHistory = Collections.synchronizedList(new ArrayList<>());
    private final StatisticsService statisticsService;
    private final GradeService gradeService;
    private final Collection<Student> students;
    private final String persistenceFile = "./data/scheduled_tasks.dat";
    
    /**
     * Represents a scheduled task with its configuration.
     */
    public static class ScheduledTask {
        private final String taskId;
        private final String taskName;
        private final TaskType taskType;
        private final ScheduleType scheduleType;
        private final int scheduleValue; // hours, days, etc.
        private final String scheduleTime; // HH:mm format for daily tasks
        private ScheduledFuture<?> future;
        private Date lastExecution;
        private long lastExecutionDuration;
        private String lastExecutionStatus;
        private Date nextExecution;
        
        public ScheduledTask(String taskId, String taskName, TaskType taskType, 
                            ScheduleType scheduleType, int scheduleValue, String scheduleTime) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.taskType = taskType;
            this.scheduleType = scheduleType;
            this.scheduleValue = scheduleValue;
            this.scheduleTime = scheduleTime;
        }
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public String getTaskName() { return taskName; }
        public TaskType getTaskType() { return taskType; }
        public ScheduleType getScheduleType() { return scheduleType; }
        public int getScheduleValue() { return scheduleValue; }
        public String getScheduleTime() { return scheduleTime; }
        public ScheduledFuture<?> getFuture() { return future; }
        public void setFuture(ScheduledFuture<?> future) { this.future = future; }
        public Date getLastExecution() { return lastExecution; }
        public void setLastExecution(Date lastExecution) { this.lastExecution = lastExecution; }
        public long getLastExecutionDuration() { return lastExecutionDuration; }
        public void setLastExecutionDuration(long lastExecutionDuration) { this.lastExecutionDuration = lastExecutionDuration; }
        public String getLastExecutionStatus() { return lastExecutionStatus; }
        public void setLastExecutionStatus(String lastExecutionStatus) { this.lastExecutionStatus = lastExecutionStatus; }
        public Date getNextExecution() { return nextExecution; }
        public void setNextExecution(Date nextExecution) { this.nextExecution = nextExecution; }
    }
    
    /**
     * Task execution record for history tracking.
     */
    public static class TaskExecution {
        private final Date timestamp;
        private final String taskId;
        private final String taskName;
        private final long executionTime;
        private final boolean success;
        private final String details;
        
        public TaskExecution(String taskId, String taskName, long executionTime, boolean success, String details) {
            this.timestamp = new Date();
            this.taskId = taskId;
            this.taskName = taskName;
            this.executionTime = executionTime;
            this.success = success;
            this.details = details;
        }
        
        public Date getTimestamp() { return timestamp; }
        public String getTaskId() { return taskId; }
        public String getTaskName() { return taskName; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }
    }
    
    /**
     * Task types supported by the scheduler.
     */
    public enum TaskType {
        GPA_RECALCULATION,
        STATISTICS_CACHE_REFRESH,
        BATCH_REPORT_GENERATION,
        DATABASE_BACKUP
    }
    
    /**
     * Schedule types: DAILY, HOURLY, WEEKLY.
     */
    public enum ScheduleType {
        DAILY,
        HOURLY,
        WEEKLY
    }
    
    public TaskScheduler(StatisticsService statisticsService, GradeService gradeService, 
                         Collection<Student> students) {
        this.statisticsService = statisticsService;
        this.gradeService = gradeService;
        this.students = students;
        
        scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "TaskScheduler-Thread");
            t.setDaemon(true);
            return t;
        });
        
        Logger.info("TASK_SCHEDULER: Initialized with 3 threads");
        loadPersistedTasks();
    }
    
    /**
     * Schedules a new task.
     */
    public void scheduleTask(String taskId, String taskName, TaskType taskType, 
                            ScheduleType scheduleType, int scheduleValue, String scheduleTime) {
        ScheduledTask task = new ScheduledTask(taskId, taskName, taskType, scheduleType, scheduleValue, scheduleTime);
        
        ScheduledFuture<?> future = null;
        long initialDelay = calculateInitialDelay(scheduleType, scheduleValue, scheduleTime);
        long period = calculatePeriod(scheduleType, scheduleValue);
        
        Runnable taskRunnable = createTaskRunnable(task);
        
        if (scheduleType == ScheduleType.DAILY || scheduleType == ScheduleType.WEEKLY) {
            future = scheduler.scheduleAtFixedRate(taskRunnable, initialDelay, period, TimeUnit.SECONDS);
        } else if (scheduleType == ScheduleType.HOURLY) {
            future = scheduler.scheduleAtFixedRate(taskRunnable, initialDelay, period, TimeUnit.SECONDS);
        }
        
        task.setFuture(future);
        task.setNextExecution(new Date(System.currentTimeMillis() + (initialDelay * 1000)));
        scheduledTasks.put(taskId, task);
        taskQueue.offer(task);
        
        persistTasks();
        
        Logger.info("SCHEDULE_TASK: Task scheduled - " + taskName + " (ID: " + taskId + ")");
        Logger.logAudit("SCHEDULE_TASK", "Schedule task " + taskName, 0, true, 
            "Task ID: " + taskId + ", Type: " + taskType + ", Schedule: " + scheduleType);
        System.out.println("Task scheduled: " + taskName + " (ID: " + taskId + ")");
    }
    
    /**
     * Creates a runnable for the specified task type.
     */
    private Runnable createTaskRunnable(ScheduledTask task) {
        return () -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            String details = "";
            
            try {
                switch (task.getTaskType()) {
                    case GPA_RECALCULATION:
                        details = "Recalculated GPA for " + students.size() + " students";
                        for (Student student : students) {
                            student.calculateAverage(gradeService);
                        }
                        success = true;
                        Logger.debug("SCHEDULED_TASK: GPA recalculation completed for " + students.size() + " students");
                        break;
                        
                    case STATISTICS_CACHE_REFRESH:
                        details = "Refreshed statistics cache";
                        statisticsService.calculateMean();
                        statisticsService.calculateMedian();
                        success = true;
                        Logger.debug("SCHEDULED_TASK: Statistics cache refreshed");
                        break;
                        
                    case BATCH_REPORT_GENERATION:
                        details = "Generated batch reports for " + students.size() + " students";
                        // Generate batch reports (simulated)
                        success = true;
                        break;
                        
                    case DATABASE_BACKUP:
                        details = "Backed up database to ./data/backup_" + 
                                 new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".dat";
                        // Simulate backup
                        File backupDir = new File("./data");
                        if (!backupDir.exists()) backupDir.mkdirs();
                        success = true;
                        break;
                }
                
                long duration = System.currentTimeMillis() - startTime;
                
                task.setLastExecution(new Date());
                task.setLastExecutionDuration(duration);
                task.setLastExecutionStatus(success ? "SUCCESS" : "FAILED");
                
                TaskExecution execution = new TaskExecution(
                    task.getTaskId(), task.getTaskName(), duration, success, details
                );
                executionHistory.add(execution);
                
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("taskType", task.getTaskType().toString());
                metrics.put("scheduleType", task.getScheduleType().toString());
                Logger.logPerformance("SCHEDULED_TASK_EXECUTION", duration, metrics);
                Logger.logAudit("SCHEDULED_TASK", "Execute " + task.getTaskName(), duration, success, details);
                
                logExecution(execution);
                
                // Send notification (simulated)
                sendNotification(task, execution);
                
                // Update next execution time
                long period = calculatePeriod(task.getScheduleType(), task.getScheduleValue());
                task.setNextExecution(new Date(System.currentTimeMillis() + (period * 1000)));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                task.setLastExecution(new Date());
                task.setLastExecutionDuration(duration);
                task.setLastExecutionStatus("FAILED: " + e.getMessage());
                
                TaskExecution execution = new TaskExecution(
                    task.getTaskId(), task.getTaskName(), duration, false, "Error: " + e.getMessage()
                );
                executionHistory.add(execution);
                Logger.error("SCHEDULED_TASK: Execution failed - " + task.getTaskName() + " - " + e.getMessage(), e);
                Logger.logAudit("SCHEDULED_TASK", "Execute " + task.getTaskName(), duration, false, 
                    "Error: " + e.getMessage());
                logExecution(execution);
            }
        };
    }
    
    /**
     * Calculates initial delay for task scheduling.
     */
    private long calculateInitialDelay(ScheduleType scheduleType, int scheduleValue, String scheduleTime) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        
        if (scheduleType == ScheduleType.DAILY && scheduleTime != null) {
            // Parse time (HH:mm format)
            String[] parts = scheduleTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);
            
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else if (scheduleType == ScheduleType.HOURLY) {
            target.add(Calendar.HOUR, scheduleValue);
        } else if (scheduleType == ScheduleType.WEEKLY) {
            target.add(Calendar.DAY_OF_MONTH, 7 * scheduleValue);
        }
        
        return (target.getTimeInMillis() - now.getTimeInMillis()) / 1000;
    }
    
    /**
     * Calculates period for recurring tasks.
     */
    private long calculatePeriod(ScheduleType scheduleType, int scheduleValue) {
        switch (scheduleType) {
            case DAILY:
                return 24 * 60 * 60; // 24 hours in seconds
            case HOURLY:
                return scheduleValue * 60 * 60; // hours in seconds
            case WEEKLY:
                return 7 * 24 * 60 * 60 * scheduleValue; // weeks in seconds
            default:
                return 60 * 60; // Default: 1 hour
        }
    }
    
    /**
     * Logs task execution to file.
     */
    private void logExecution(TaskExecution execution) {
        try {
            File logDir = new File("./data");
            if (!logDir.exists()) logDir.mkdirs();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String logFile = "./data/task_executions_" + sdf.format(new Date()) + ".log";
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                SimpleDateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                writer.printf("[%s] Task: %s | Status: %s | Duration: %dms | Details: %s%n",
                    logFormat.format(execution.getTimestamp()),
                    execution.getTaskName(),
                    execution.isSuccess() ? "SUCCESS" : "FAILED",
                    execution.getExecutionTime(),
                    execution.getDetails());
            }
        } catch (IOException e) {
            System.err.println("Failed to log task execution: " + e.getMessage());
        }
    }
    
    /**
     * Sends notification (simulated email).
     */
    private void sendNotification(ScheduledTask task, TaskExecution execution) {
        // Simulated notification
        System.out.println("[NOTIFICATION] Task '" + task.getTaskName() + "' " + 
                          (execution.isSuccess() ? "completed successfully" : "failed") +
                          " in " + execution.getExecutionTime() + "ms");
    }
    
    /**
     * Displays all active scheduled tasks.
     */
    public void displayActiveTasks() {
        System.out.println("\n========================================================================");
        System.out.println("                    SCHEDULED TASKS OVERVIEW                             ");
        System.out.println("==========================================================================");
        System.out.println();
        
        if (scheduledTasks.isEmpty()) {
            System.out.println("No scheduled tasks.");
            return;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (ScheduledTask task : scheduledTasks.values()) {
            System.out.println(" " + task.getTaskName() + "===================================================");
            System.out.printf("│ Task ID: %-55s │%n", task.getTaskId());
            System.out.printf("│ Schedule: %-54s │%n", 
                task.getScheduleType() + " (" + task.getScheduleValue() + ")");
            
            if (task.getLastExecution() != null) {
                System.out.printf("│ Last Execution: %-48s │%n", sdf.format(task.getLastExecution()));
                System.out.printf("│ Last Status: %-52s │%n", task.getLastExecutionStatus());
                System.out.printf("│ Last Duration: %-50dms │%n", task.getLastExecutionDuration());
            } else {
                System.out.println("│ Last Execution: Never                                              │");
            }
            
            if (task.getNextExecution() != null) {
                long timeUntilNext = task.getNextExecution().getTime() - System.currentTimeMillis();
                int secondsUntilNext = (int) (timeUntilNext / 1000);
                System.out.printf("│ Next Execution: %-48s │%n", sdf.format(task.getNextExecution()));
                System.out.printf("│ Countdown: %-54d seconds │%n", Math.max(0, secondsUntilNext));
            }
            
            System.out.println("==============================================================================");
            System.out.println();
        }
    }
    
    /**
     * Persists tasks to file.
     */
    private void persistTasks() {
        try {
            File dataDir = new File("./data");
            if (!dataDir.exists()) dataDir.mkdirs();
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(persistenceFile))) {
                oos.writeObject(new ArrayList<>(scheduledTasks.values()));
            }
        } catch (IOException e) {
            System.err.println("Failed to persist tasks: " + e.getMessage());
        }
    }
    
    /**
     * Loads persisted tasks from file.
     */
    @SuppressWarnings("unchecked")
    private void loadPersistedTasks() {
        File file = new File(persistenceFile);
        if (!file.exists()) return;
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(persistenceFile))) {
            List<ScheduledTask> tasks = (List<ScheduledTask>) ois.readObject();
            
            for (ScheduledTask task : tasks) {
                // Reschedule tasks
                scheduleTask(task.getTaskId(), task.getTaskName(), task.getTaskType(),
                           task.getScheduleType(), task.getScheduleValue(), task.getScheduleTime());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load persisted tasks: " + e.getMessage());
        }
    }
    
    /**
     * Shuts down the scheduler gracefully.
     */
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Gets execution history.
     */
    public List<TaskExecution> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    /**
     * Returns the next scheduled task by earliest nextExecution time using the priority queue (O(1) peek).
     */
    public ScheduledTask peekNextScheduledTask() {
        return taskQueue.peek();
    }
}

