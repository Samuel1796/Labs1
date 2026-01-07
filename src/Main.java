import models.*;
import services.student.StudentService;
import services.file.GradeService;
import services.file.BatchReportTaskManager;
import services.menu.MenuService;
import services.menu.MainMenuHandler;
import services.analytics.StatisticsService;
import java.io.IOException;
import java.util.*;

import utilities.FileIOUtils;
import utilities.Logger;
import services.system.AuditTrailService;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        // Initialize Logger first
        Logger.initialize();
        Logger.info("=== Student Grade Management System Started ===");
        Logger.info("Application initialized at: " + new java.util.Date());
        
        // Initialize services
        StudentService studentService = new StudentService();
        GradeService gradeService = new GradeService(500);
        MenuService menuService = new MenuService();
        AuditTrailService auditTrailService = new AuditTrailService();

        // Initialize sample students and grades
        int[] gradeCountRef = {gradeService.getGradeCount()};
        gradeService.setGradeCount(gradeCountRef[0]);

        StatisticsService statisticsService = new StatisticsService(
                gradeService.getGrades(),
                gradeService.getGradeCount(),
                studentService.getStudents(),
                studentService.getStudentCount(),
                gradeService
        );

        FileIOUtils.monitorDirectory(Paths.get("./imports"), () -> {
            System.out.println("New file detected in imports directory!");
            // Optionally trigger import logic here
        });

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        MainMenuHandler menuHandler = new MainMenuHandler(studentService, gradeService, menuService, statisticsService, sc, auditTrailService);
        List<Student> studentList = new ArrayList<>(studentService.getStudents());
        int format = 1; // 1: CSV, 2: JSON, 3: Binary, 4: All formats
        String outputDir = "./reports/batch_2025-12-17/";
        int threadCount = 4;
        
        // Note: BatchReportTaskManager requires GradeImportExportService
        services.file.GradeImportExportService gradeImportExportService = new services.file.GradeImportExportService(gradeService);
        // BatchReportTaskManager initialized for potential batch operations
        @SuppressWarnings("unused")
        BatchReportTaskManager batchManager = new BatchReportTaskManager(studentList, gradeImportExportService, format, outputDir, threadCount);

        // Add shutdown hook to properly close logger
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.shutdown();
            auditTrailService.shutdown();
        }));

        try {
            while (running) {
                try {
                    menuService.displayMainMenu();
                    int choice = sc.nextInt();
                    sc.nextLine();
                    running = menuHandler.handleMenu(choice);
                } catch (java.util.InputMismatchException e) {
                    Logger.warn("APPLICATION: Invalid input format - " + e.getMessage());
                    System.out.println("Invalid input. Please enter a number.");
                    sc.nextLine();
                } catch (Exception e) {
                    Logger.error("APPLICATION: Error in menu handler - " + e.getMessage(), e);
                    System.out.println("An error occurred: " + e.getMessage());
                    System.out.println("Please try again.");
                }
            }
        } catch (Exception e) {
            Logger.error("APPLICATION: Fatal error in main loop - " + e.getMessage(), e);
            System.out.println("A fatal error occurred: " + e.getMessage());
        } finally {
            if (!running) {
                System.out.println("\nExiting program...");
                Logger.info("APPLICATION: Exiting - exporting logs");
                Logger.shutdown();
                System.out.println("Logs exported successfully.");
                System.exit(0);
            }
        }
        
        // Cleanup on exit
        Logger.shutdown();
        auditTrailService.shutdown();
    }
}
