import models.*;
import services.*;
import exceptions.*;
import java.io.IOException;
import java.util.*;

import utilities.FileIOUtils;
import java.nio.file.Paths;



// Main application class for Student Grade Management System.
public class Main {
    public static void main(String[] args) throws IOException {
        // Initialize services
        StudentService studentService = new StudentService();
        GradeService gradeService = new GradeService(500);
        MenuService menuService = new MenuService();

        // Initialize sample students and grades
        Collection<Student> students = studentService.getStudents();
        Grade[] grades = gradeService.getGrades();
        int[] studentCountRef = {studentService.getStudentCount()};
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

        MainMenuHandler menuHandler = new MainMenuHandler(studentService, gradeService, menuService, statisticsService, sc);
        List<Student> studentList = new ArrayList<>(studentService.getStudents());
        int format = 1; // 1: CSV, 2: JSON, 3: Binary, 4: All formats
        String outputDir = "./reports/batch_2025-12-17/";
        int threadCount = 4;
        
        BatchReportTaskManager batchManager = new BatchReportTaskManager(studentList, gradeService, format, outputDir, threadCount);
        batchManager.startBatchExport();
        while (batchManager.isRunning()) {
            System.out.println("Background tasks running: " + batchManager.getActiveTasks());
            try {
                Thread.sleep(500); // Poll every 0.5s
            } catch (InterruptedException e) {
                // Optionally handle interruption (e.g., log or break)
                System.out.println("Polling interrupted: " + e.getMessage());
                break;
            }
        }
        while (running) {
            // Show background task status before menu
            if (menuHandler.getBatchManager() != null && menuHandler.getBatchManager().isRunning()) {
                System.out.println("Background Tasks: " + menuHandler.getBatchManager().getActiveTasks() + " active");
            } else {
                System.out.println("No background tasks are running.");
            }

            menuService.displayMainMenu();

            int choice = sc.nextInt();
            sc.nextLine();
            running = menuHandler.handleMenu(choice);
        }
    }
}