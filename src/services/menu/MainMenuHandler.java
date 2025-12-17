package services.menu;

import models.*;
import exceptions.*;
import services.system.TaskScheduler;
import utilities.FileIOUtils;
import utilities.ValidationUtils;
import services.student.StudentService;
import services.file.GradeService;
import services.file.GradeImportExportService;
import services.file.BatchReportTaskManager;
import services.analytics.StatisticsService;
import services.analytics.StatisticsDashboard;
import services.search.PatternSearchService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainMenuHandler {
    private final StudentService studentService;
    private final GradeService gradeService;
    private final GradeImportExportService gradeImportExportService;
    private final MenuService menuService;
    private final StatisticsService statisticsService;
    private final Scanner sc;
    private BatchReportTaskManager batchManager;

    public BatchReportTaskManager getBatchManager() {
        return batchManager;
    }

    public MainMenuHandler(StudentService studentService, GradeService gradeService, MenuService menuService, StatisticsService statisticsService, Scanner sc) {
        this.studentService = studentService;
        this.gradeService = gradeService;
        this.gradeImportExportService = new GradeImportExportService(gradeService);
        this.menuService = menuService;
        this.statisticsService = statisticsService;
        this.sc = sc;
    }

    public boolean handleMenu(int choice) {
        try {


            switch (choice) {


                case 1:
                    // Add Student
                    boolean studentAdded = false;
                    while (!studentAdded) {
                        try {
                            System.out.print("Enter student name: ");
                            String name = sc.nextLine();
                            String nameError = ValidationUtils.validateName(name);
                            if (nameError != null) {
                                System.out.println(nameError);
                                continue; // Retry name input
                            }

                            int age = -1;
                            while (age < 0) {
                                System.out.print("Enter student age: ");
                                String ageInput = sc.nextLine();
                                try {
                                    age = Integer.parseInt(ageInput);
                                    if (age < 0) {
                                        System.out.println("Age must be a positive integer.");
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input. Please enter a valid integer for age.");
                                }
                            }

                            String email;
                            while (true) {
                                System.out.print("Enter student email: ");
                                email = sc.nextLine();
                                String emailError = ValidationUtils.validateEmail(email);
                                if (emailError == null) break;
                                System.out.println(emailError);
                            }

                            String phone;
                            while (true) {
                                System.out.print("Enter student phone: ");
                                phone = sc.nextLine();
                                String phoneError = ValidationUtils.validatePhone(phone);
                                if (phoneError == null) break;
                                System.out.println(phoneError);
                            }

                            if (studentService.isDuplicateStudent(name, email)) {
                                throw new DuplicateStudentException(name, email);
                            }

                            System.out.println("Student type: ");
                            System.out.println("1. Regular Student (Passing grade: 50%)");
                            System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
                            System.out.print("Select type (1-2): ");
                            int type = -1;
                            while (type != 1 && type != 2) {
                                String typeInput = sc.nextLine();
                                try {
                                    type = Integer.parseInt(typeInput);
                                    if (type != 1 && type != 2) {
                                        System.out.println("Invalid selection. Please enter 1 or 2.");
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input. Please enter 1 or 2.");
                                }
                            }

                            Student newStudent = (type == 2)
                                    ? new HonorsStudent(name, age, email, phone)
                                    : new RegularStudent(name, age, email, phone);
                            studentService.addStudent(newStudent);
                            System.out.println("Student added successfully!");
                            studentAdded = true;
                        } catch (DuplicateStudentException e) {
                            System.out.print("Duplicate student detected. Try again? (Y/N): ");
                            String retry = sc.nextLine();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        }
                    }
                    break;

                case 2:
                    // View Students
                    studentService.viewAllStudents(gradeService);
                    break;

                case 3:
                    // Record Grade
                    while (true) {
                        try {
                            System.out.print("Enter Student ID: ");
                            String studentID = sc.nextLine();
                            Student foundStudent = studentService.findStudentById(studentID);
                            double avg = foundStudent.calculateAverage(gradeService);

                            System.out.println("Student Details: ");
                            System.out.printf("Name: %s%n", foundStudent.getName());
                            System.out.printf("Type: %s%n", (foundStudent instanceof HonorsStudent) ? "Honors Student" : "Regular Student");
                            System.out.printf("Current Average: %.1f%n", avg);

                            System.out.println("Subject type:");
                            System.out.println("1. Core Subject (Mathematics, English, Science)");
                            System.out.println("2. Elective Subject (Art, Physical Education)");
                            System.out.print("Select type (1-2): ");
                            int subjectType = sc.nextInt();
                            sc.nextLine();

                            Subject subject = null;
                            if (subjectType == 1) {
                                System.out.println("Available Core Subject:");
                                System.out.println("1. Mathematics");
                                System.out.println("2. English");
                                System.out.println("3. Science");
                                System.out.print("Select subject (1-3): ");
                                int coreSubject = sc.nextInt();
                                sc.nextLine();
                                switch (coreSubject) {
                                    case 1: subject = new CoreSubject("Mathematics", "Core Subject"); break;
                                    case 2: subject = new CoreSubject("English", "Core Subject"); break;
                                    case 3: subject = new CoreSubject("Science", "Core Subject"); break;
                                    default: throw new InvalidSubjectException("Core", String.valueOf(coreSubject));
                                }
                            } else if (subjectType == 2) {
                                System.out.println("Available Elective Subject:");
                                System.out.println("1. Art");
                                System.out.println("2. Physical Education");
                                int electiveSubject = -1;
                                while (electiveSubject < 1 || electiveSubject > 2) {
                                    System.out.print("Select subject (1-2): ");
                                    try {
                                        String input = sc.nextLine();
                                        electiveSubject = Integer.parseInt(input);
                                        if (electiveSubject < 1 || electiveSubject > 2) {
                                            System.out.println("Invalid selection. Please enter 1 or 2.");
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid input. Please enter a number (1 or 2).");
                                    }
                                }

                                switch (electiveSubject) {
                                    case 1: subject = new ElectiveSubject("Art", "Elective Subject"); break;
                                    case 2: subject = new ElectiveSubject("Physical Education", "Elective Subject"); break;
                                    default: throw new InvalidSubjectException("Elective", String.valueOf(electiveSubject));
                                }
                            } else {
                                System.out.println();
                                throw new InvalidSubjectException("Unknown", String.valueOf(subjectType));
                            }

                            System.out.print("Enter grade (0-100): ");
                            String gradeInput = sc.nextLine();
                            double gradeValue;
                            try {
                                gradeValue = Double.parseDouble(gradeInput);
                                if (!ValidationUtils.isValidGrade(gradeValue)) {
                                    System.out.println(ValidationUtils.getValidationErrorMessage("Grade", gradeInput, "grade"));
                                    continue; // Retry grade input
                                }
                            } catch (NumberFormatException e) {
                                System.out.println(ValidationUtils.getValidationErrorMessage("Grade", gradeInput, "grade"));
                                continue; // Retry grade input
                            }

                            java.util.Date date = new java.util.Date();
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
                            String nextGradeID = String.format("GRD%03d", gradeService.getGradeCount() + 1);

                            System.out.println("CONFIRMATION:");
                            System.out.printf("Grade ID: %s%n", nextGradeID);
                            System.out.printf("Student: %s - %s%n", foundStudent.getStudentID(), foundStudent.getName());
                            System.out.printf("Subject: %s (%s)%n", subject.getSubjectName(), subject.getSubjectType());
                            System.out.printf("Grade: %.1f%n", gradeValue);
                            System.out.printf("Date: %s%n", sdf.format(date));
                            System.out.print("Confirm? (Y/N): ");
                            String confirm = sc.nextLine();

                            if (confirm.equalsIgnoreCase("Y")) {
                                // Check for duplicate grade before recording
                                if (gradeService.isDuplicateGrade(foundStudent.getStudentID(), subject.getSubjectName(), subject.getSubjectType())) {
                                    System.out.printf("A grade for %s (%s) already exists for this student. Overwrite with new value? [Y/N]: ",
                                            subject.getSubjectName(), subject.getSubjectType());
                                    String overwrite = sc.nextLine();
                                    if (overwrite.equalsIgnoreCase("Y")) {
                                        gradeService.updateGrade(foundStudent.getStudentID(), subject.getSubjectName(), subject.getSubjectType(), (int) gradeValue);
                                        System.out.printf("Grade updated successfully!%n");
                                    } else {
                                        System.out.println("Grade recording canceled due to duplicate.");
                                    }
                                } else {
                                    Grade grade = new Grade(nextGradeID, foundStudent.getStudentID(), subject.getSubjectName(), subject.getSubjectType(), gradeValue, date);
                                    gradeService.recordGrade(grade, studentService); // <-- FIXED: Pass studentService here!
                                    System.out.printf("Grade recorded successfully! Grade ID: %s%n", nextGradeID);
                                }
                            } else {
                                System.out.println("Grade recording canceled.");
                            }
                            break;
                        } catch (AppExceptions e) {
                            System.out.println("ERROR: " + e.getClass().getSimpleName());
                            System.out.println(e.getMessage());
                            System.out.print("Try again? (Y/N): ");
                            String retry = sc.nextLine();
                            if (!retry.equalsIgnoreCase("Y")) break;
                        }
                    }
                    break;

                case 4:
                    // View Grade Report
                    boolean found = false;
                    while (!found) {
                        System.out.print("Enter Student ID: ");
                        String idForReport = sc.nextLine();
                        try {
                            Student studentForReport = studentService.findStudentById(idForReport);
                            gradeService.viewGradeReport(studentForReport);
                            found = true;
                        } catch (StudentNotFoundException e) {
                            System.out.println("ERROR: " + e.getMessage());
                            System.out.print("Student not found. Try again? (Y/N): ");
                            String retry = sc.nextLine();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        }
                    }
                    break;

                case 5:
                    // Export Grade Report
                    System.out.println("EXPORT GRADE REPORT (Multi-Format)");
                    System.out.println("_____________________________");
                    System.out.println();
                    boolean foundExport = false;
                    Student exportStudent = null;
                    while (!foundExport) {
                        System.out.print("Enter Student ID: ");
                        String exportStudentID = sc.nextLine();
                        try {
                            exportStudent = studentService.findStudentById(exportStudentID);
                            foundExport = true;
                        } catch (StudentNotFoundException e) {
System.out.println("ERROR: " + e.getMessage());
                            System.out.print("Student not found. Try again? (Y/N): ");
                            String retry = sc.nextLine();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        }
                    }
                    if (!foundExport || exportStudent == null) break;

                    System.out.printf("Student: %s - %s (%s)%n", exportStudent.getStudentID(), exportStudent.getName(), exportStudent.getEmail());
                    System.out.printf("Type: %s | Phone: %s%n", (exportStudent instanceof HonorsStudent) ? "Honors Student" : "Regular Student", exportStudent.getPhone());
                    System.out.println("Total Grades: " + gradeService.countGradesForStudent(exportStudent));
                    System.out.println("Export Format:");
                    System.out.println("1. CSV");
                    System.out.println("2. JSON");
                    System.out.println("3. Binary");
                    System.out.println("4. All formats");
                    int exportFormat = 0;
                    while (exportFormat < 1 || exportFormat > 4) {
                        System.out.print("Select format (1-4): ");
                        try {
                            exportFormat = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number between 1 and 4.");
                        }
                    }
                    System.out.println("Report Type:");
                    System.out.println("1. Summary Report");
                    System.out.println("2. Detailed Report");
                    System.out.println("3. Transcript Format");
                    System.out.println("4. Performance Analytics");
                    int reportType = 0;
                    while (reportType < 1 || reportType > 4) {
                        System.out.print("Select type (1-4): ");
                        try {
                            reportType = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number between 1 and 4.");
                        }
                    }
                    System.out.print("Enter filename (without extension): ");
                    String filename = sc.nextLine().trim();
                    if (filename.isEmpty()) {
                        System.out.println("Filename cannot be empty.");
                        break;
                    }
                    try {
                        if (exportFormat == 4) {
                            gradeImportExportService.exportGradeReportMultiFormat(exportStudent, reportType, filename);
} else if (exportFormat == 1) {
                            gradeImportExportService.exportGradesCSV(filename);
                        } else if (exportFormat == 2) {
                            gradeImportExportService.exportGradesJSON(filename);
                        } else if (exportFormat == 3) {
                            gradeImportExportService.exportGradesBinary(filename);
                        }
                        System.out.println("Report exported successfully!");
                    } catch (Exception e) {
                        System.out.println("Export failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;

//                    IMPORT DATA MULTI FORMAT SUPPORT
                case 6:
                    System.out.println("IMPORT STUDENTS");
                    System.out.println("1. CSV");
                    System.out.println("2. JSON");
                    System.out.println("3. Binary");
                    System.out.print("Select format (1-3): ");
                    int importFormat = Integer.parseInt(sc.nextLine());
                    System.out.print("Enter filename (without extension): ");
                    String importFilename = sc.nextLine().trim();

                    int importedCount = 0;
                    int duplicateCount = 0;
                    List<String> errorMessages = new ArrayList<>();

                    try {
                        List<Student> importedStudents = null;
                        switch (importFormat) {
                            case 1:
                                importedStudents = FileIOUtils.readStudentsFromCSV(Paths.get("./imports/" + importFilename + ".csv"));
                                break;
                            case 2:
                                importedStudents = FileIOUtils.readStudentsFromJSON(Paths.get("./imports/" + importFilename + ".json"));
                                break;
                            case 3:
                                importedStudents = FileIOUtils.readStudentsFromBinary(Paths.get("./imports/" + importFilename + ".bin"));
                                break;
                            default:
                                System.out.println("Invalid format.");
                                break;
                        }
                        if (importedStudents == null) {
                            System.out.println("No students imported. Check file format and path.");
                            break;
                        }
                        for (Student s : importedStudents) {
                            // Check for duplicates using name and email as keys
                            if (studentService.isDuplicateStudent(s.getName(), s.getEmail())) {
                                duplicateCount++;
                                errorMessages.add("Duplicate skipped: " + s.getName() + " (" + s.getEmail() + ")");
                                continue; // Skip this student
                            }
                            
                            try {
                                studentService.addStudent(s);
                                importedCount++;
                            } catch (DuplicateStudentException e) {
                                duplicateCount++;
                                errorMessages.add("Duplicate: " + s.getName() + " (" + s.getEmail() + ")");
                            } catch (Exception e) {
                                errorMessages.add("Error importing " + s.getName() + ": " + e.getMessage());
                            }
                        }
                        System.out.println("IMPORT SUMMARY");
                        System.out.println("Total Students in File: " + importedStudents.size());
                        System.out.println("Successfully Imported: " + importedCount);
                        System.out.println("Duplicates Skipped: " + duplicateCount);
                        if (duplicateCount > 0) {
                            System.out.println("\nDuplicate Details:");
                            for (String msg : errorMessages) {
                                if (msg.contains("Duplicate")) {
                                    System.out.println("  - " + msg);
                                }
                            }
                        }
                        if (!errorMessages.isEmpty() && duplicateCount == 0) {
                            System.out.println("Errors:");
                            for (String msg : errorMessages) {
                                if (!msg.contains("Duplicate")) {
                                    System.out.println("  - " + msg);
                                }
                            }
                        }
                        System.out.println("Import completed!");
                    } catch (IOException e) {
                        System.out.println("File not found or could not be read: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Import failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;

//                    BULK IMPORT
                case 7:
                    /**
                     * Bulk Grade Import Handler
                     * 
                     * This menu option allows administrators to import multiple grades
                     * from CSV or JSON files in a single operation. The import process
                     * includes comprehensive error handling, duplicate detection, and
                     * detailed reporting of success/failure statistics.
                     * 
                     * User Input Flow:
                     * 1. Select file format (CSV or JSON)
                     * 2. Enter filename (without extension)
                     * 3. System validates and processes file
                     * 4. Display import summary with success/failure counts
                     * 
                     * Error Handling:
                     * - Invalid format selection: prompts user to retry
                     * - Empty filename: prevents processing and returns to menu
                     * - File not found: caught and reported by bulkImportGrades()
                     * - Malformed records: tracked and reported in import summary
                     */
                    System.out.println("IMPORT GRADES");
                    System.out.println("1. CSV");
                    System.out.println("2. JSON");
                    System.out.print("Select format (1-2): ");
                    
                    // Input validation loop: ensures valid format selection
                    // Continues prompting until valid input (1 or 2) is received
                    int gradeImportFormat = -1;
                    while (gradeImportFormat != 1 && gradeImportFormat != 2) {
                        try {
                            gradeImportFormat = Integer.parseInt(sc.nextLine());
                            if (gradeImportFormat != 1 && gradeImportFormat != 2) {
                                System.out.println("Invalid selection. Please enter 1 or 2.");
                            }
                        } catch (NumberFormatException e) {
                            // Handle non-numeric input gracefully
                            System.out.println("Invalid input. Please enter 1 or 2.");
                        }
                    }
                    
                    // Convert numeric selection to format string for service method
                    String formatStr = (gradeImportFormat == 1) ? "csv" : "json";
                    
                    // Get filename from user (without extension - added by service)
                    System.out.print("Enter filename (without extension): ");
                    String gradeImportFilename = sc.nextLine().trim();
                    
                    // Validate filename: prevent empty input
                    if (gradeImportFilename.isEmpty()) {
                        System.out.println("Filename cannot be empty.");
                        break; // Exit menu option without processing
                    }
                    
                    // Execute bulk import with comprehensive error handling
                    // bulkImportGrades() handles file parsing, validation, and reporting
                    try {
                        gradeImportExportService.bulkImportGrades(gradeImportFilename, formatStr, studentService);
                    } catch (Exception e) {
                        // Catch-all exception handler: ensures menu doesn't crash
                        // Print error message and stack trace for debugging
                        System.out.println("Grade import failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;


                case 8:
                    // Calculate Student GPA
                    System.out.println("CALCULATE STUDENT GPA");
                    System.out.println("_____________________________");
                    Student gpaStudent = null;
                    boolean foundGPA = false;
                    while (!foundGPA) {
                        System.out.print("Enter Student ID: ");
                        String gpaId = sc.nextLine().trim();
                        try {
                            gpaStudent = studentService.findStudentById(gpaId);
                            foundGPA = true;
                        } catch (StudentNotFoundException e) {
                            System.out.println("Error: " + e.getMessage());
                            System.out.print("Try again? (Y/N): ");
                            String retry = sc.nextLine();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        }
                    }
                    if (!foundGPA || gpaStudent == null) break;
                    statisticsService.printStudentGPAReport(gpaStudent);
                    break;

//                    VIEW CLASS STATS
                case 9:
                    // STATISTICAL ANALYSIS
                    StatisticsService statsService = new StatisticsService(
                            gradeService.getGrades(),
                            gradeService.getGradeCount(),
                            studentService.getStudents(),
                            studentService.getStudentCount(),
                            gradeService
                    );
                    statsService.printStatisticsReport();
                    break;


//                    REAL TIME STATS
                case 10:
                    /**
                     * Real-Time Statistics Dashboard Handler
                     * 
                     * Implements US-5: Real-Time Statistics Dashboard with Background Thread.
                     * 
                     * Features:
                     * - Auto-refreshing dashboard every 5 seconds
                     * - Background daemon thread for statistics calculation
                     * - Interactive controls (refresh, pause/resume, quit)
                     * - Live grade distribution, top performers, system metrics
                     * - Thread-safe data handling
                     */
                    // Create StatisticsDashboard with StudentService for fresh data on each refresh
                    StatisticsDashboard dashboard = new StatisticsDashboard(
                        gradeService,
                        studentService,
                        studentService.getStudents(),
                        studentService.getStudentCount()
                    );
                    
                    // Set dashboard in menu service for status display
                    menuService.setStatisticsDashboard(dashboard);
                    
                    dashboard.start();
                    
                    // Interactive dashboard loop
                    Scanner dashboardScanner = new Scanner(System.in);
                    boolean dashboardRunning = true;
                    
                    while (dashboardRunning && dashboard.isRunning()) {
                        dashboard.displayDashboard();
                        System.out.print("Command: ");
                        String command = dashboardScanner.nextLine().trim().toUpperCase();
                        
                        switch (command) {
                            case "R":
                                // Manual refresh
                                dashboard.refresh();
                                break;
                            case "Q":
                                dashboardRunning = false;
                                dashboard.stop();
                                menuService.setStatisticsDashboard(null);
                                break;
                            default:
                                // Continue loop to refresh display
                                break;
                        }
                        
                        // Small delay to prevent excessive CPU usage
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    if (dashboard.isRunning()) {
                        dashboard.stop();
                    }
                    break;

//                    GENERATE BATCH REPORT
                case 11:
                    /**
                     * Batch Report Generation Handler
                     * 
                     * This menu option orchestrates concurrent batch report generation
                     * for multiple students. It implements a sophisticated workflow:
                     * 1. Student selection (scope filtering)
                     * 2. Format selection (single or multi-format)
                     * 3. Thread pool configuration (performance tuning)
                     * 4. Concurrent execution with progress monitoring
                     * 
                     * Design Pattern:
                     * - Strategy pattern: different scope selection strategies
                     * - Factory pattern: creates BatchReportTaskManager with configuration
                     * - Observer pattern: progress updates during execution
                     * 
                     * Performance Considerations:
                     * - Thread pool size based on available CPU cores
                     * - Concurrent execution reduces total processing time
                     * - File I/O is primary bottleneck (not CPU-bound)
                     */
                    System.out.println("GENERATE BATCH REPORTS");
                    System.out.println("Report Scope:");
                    System.out.println("1. All Students");
                    System.out.println("2. By Student Type (Regular/Honors)");
                    System.out.println("3. By Grade Range");
                    System.out.println("4. Custom Selection");
                    
                    // Scope selection with input validation
                    int scope = 0;
                    while (scope < 1 || scope > 4) {
                        System.out.print("Select scope (1-4): ");
                        try { scope = Integer.parseInt(sc.nextLine()); } catch (NumberFormatException ignored) {}
                    }

                    // Phase 1: Student Selection - Filter students based on scope
                    // Different strategies for different scopes (Strategy pattern)
                    List<Student> batchStudents = new ArrayList<>();
                    
                    if (scope == 1) {
                        // Strategy 1: All students - no filtering needed
                        batchStudents.addAll(studentService.getStudents());
                    } else if (scope == 2) {
                        // Strategy 2: Filter by student type (Regular vs Honors)
                        System.out.print("Type (1: Regular, 2: Honors): ");
                        int type = Integer.parseInt(sc.nextLine());
                        
                        // Iterate through all students and filter by instance type
                        // Uses instanceof for reliable type checking
                        for (Student s : studentService.getStudents()) {
                            if ((type == 1 && s instanceof RegularStudent) ||
                                    (type == 2 && s instanceof HonorsStudent)) {
                                batchStudents.add(s);
                            }
                        }
                    } else if (scope == 3) {
                        // Strategy 3: Filter by grade range (inclusive bounds)
                        System.out.print("Min grade: ");
                        int min = Integer.parseInt(sc.nextLine());
                        System.out.print("Max grade: ");
                        int max = Integer.parseInt(sc.nextLine());
                        
                        // Calculate average for each student and filter by range
                        // Note: This recalculates averages - consider caching for performance
                        for (Student s : studentService.getStudents()) {
                            double avg = s.calculateAverage(gradeService);
                            if (avg >= min && avg <= max) batchStudents.add(s);
                        }
                    } else if (scope == 4) {
                        // Strategy 4: Custom selection - user provides specific IDs
                        System.out.print("Enter comma-separated Student IDs: ");
                        String[] ids = sc.nextLine().split(",");
                        
                        // Parse comma-separated IDs and lookup each student
                        // Invalid IDs are silently skipped (consider reporting errors)
                        for (String id : ids) {
                            try {
                                batchStudents.add(studentService.findStudentById(id.trim()));
                            } catch (Exception ignored) {
                                // Student not found: skip and continue
                                // Consider logging or reporting invalid IDs
                            }
                        }
                    }

                    // Phase 2: Format Selection
                    System.out.println("Report Format:");
                    System.out.println("1. PDF summary");
                    System.out.println("2. Detailed Text");
                    System.out.println("3. Excel Spreadsheet");
                    System.out.println("4. All Formats");
                    
                    int format = 0;
                    while (format < 1 || format > 4) {
                        System.out.print("Select format (1-4): ");
                        try { format = Integer.parseInt(sc.nextLine()); } catch (NumberFormatException ignored) {}
                    }

                    // Phase 3: Thread Pool Configuration
                    // Determine optimal thread count based on available CPU cores
                    // For I/O-bound tasks (file writing), more threads can help
                    int processors = Runtime.getRuntime().availableProcessors();
                    System.out.println("Available Processors: " + processors);
                    System.out.println("Recommended Threads: 4-" + processors);
                    System.out.print("Enter number of threads (1-" + processors + "): ");
                    
                    // Get thread count from user with bounds checking
                    int threads = 1;
                    try {
                        threads = Integer.parseInt(sc.nextLine());
                        // Enforce bounds: minimum 1, maximum = available processors
                        // Prevents thread explosion and resource exhaustion
                        if (threads < 1) threads = 1;
                        if (threads > processors) threads = processors;
                    } catch (NumberFormatException ignored) {
                        // Invalid input: use default of 1 thread
                    }

                    // Phase 4: Directory Setup
                    // Create timestamped directory for batch reports
                    // Format: ./reports/batch_YYYY-MM-DD/
                    String batchDir = "./reports/batch_" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + "/";
                    new java.io.File(batchDir).mkdirs();

                    // Phase 5: Execute Batch Generation
                    // Create task manager with configuration and start concurrent execution
                    // BatchReportTaskManager handles thread pool, progress tracking, and error handling
                    BatchReportTaskManager manager = new BatchReportTaskManager(
                            batchStudents, gradeImportExportService, format, batchDir, threads
                    );
                    manager.startBatchExport(); // Blocks until all reports are generated
                    break;


//
                case 12:
                    System.out.println("SEARCH STUDENTS");
                    System.out.println("_____________________________");
                    System.out.println("Search options:");
                    System.out.println("1. By Student ID");
                    System.out.println("2. By Name (partial match)");
                    System.out.println("3. By Grade Range");
                    System.out.println("4. By Student Type");
                    System.out.print("Select option (1-4): ");
                    int searchOption = sc.nextInt();
                    sc.nextLine();

                    Student[] searchResults = new Student[0];

                    switch (searchOption) {
                        case 1:
                            boolean foundDetail = false;
                            while (!foundDetail) {
                                System.out.print("Enter Student ID to view details: ");
                                String detailId = sc.nextLine().trim();
                                try {
                                    Student detailStudent = studentService.findStudentById(detailId);
                                    gradeService.viewGradeReport(detailStudent);
                                    foundDetail = true;
                                } catch (StudentNotFoundException e) {
                                    System.out.println("ERROR: " + e.getMessage());
                                    System.out.print("Student not found. Try again? (Y/N): ");
                                    String retry = sc.nextLine();
                                    if (!retry.equalsIgnoreCase("Y")) {
                                        break;
                                    }
                                }
                            }
                            break;
                        case 2:
                            System.out.print("Enter name or part of name to search: ");
                            String namePart = sc.nextLine().trim();
                            searchResults = studentService.searchStudentsByName(namePart);
                            break;
                        case 3:
                            System.out.print("Enter minimum average grade: ");
                            double minGrade = sc.nextDouble();
                            System.out.print("Enter maximum average grade: ");
                            double maxGrade = sc.nextDouble();
                            sc.nextLine();
                            searchResults = studentService.searchStudentsByGradeRange(minGrade, maxGrade, gradeService);
                            break;
                        case 4:
                            System.out.print("Search for Honors students? (Y/N): ");
                            String honorsInput = sc.nextLine().trim();
                            boolean honors = honorsInput.equalsIgnoreCase("Y");
                            searchResults = studentService.searchStudentsByType(honors);
                            break;
                        default:
                            System.out.println("Invalid search option.");
                            break;
                    }

                    if (searchResults != null && searchResults.length > 0) {
                        System.out.println("Search Results:");
                        for (Student s : searchResults) {
                            System.out.printf("ID: %s | Name: %s | Type: %s | Email: %s | Phone: %s | Avg: %.1f%n",
                                    s.getStudentID(),
                                    s.getName(),
                                    (s instanceof HonorsStudent) ? "Honors" : "Regular",
                                    s.getEmail(),
                                    s.getPhone(),
                                    s.calculateAverage(gradeService));
                        }
                    }
                    break;

//                    PATTERN BASED SEARCH
                case 13:
                    /**
                     * Advanced Pattern-Based Search (US-7)
                     * 
                     * Allows searching students using regex patterns:
                     * - Email domain patterns
                     * - Phone area code patterns
                     * - Student ID patterns with wildcards
                     * - Name patterns
                     * - Custom regex patterns
                     */
                    PatternSearchService patternSearch = new PatternSearchService(studentService.getStudents());
                    
                    System.out.println("PATTERN-BASED SEARCH");
                    System.out.println("1. Search by Email Domain");
                    System.out.println("2. Search by Phone Area Code");
                    System.out.println("3. Search by Student ID Pattern");
                    System.out.println("4. Search by Name Pattern");
                    System.out.println("5. Custom Regex Pattern");
                    System.out.print("Select option (1-5): ");
                    int patternOption = Integer.parseInt(sc.nextLine());
                    
                    System.out.print("Case sensitive? (Y/N): ");
                    boolean caseSensitive = sc.nextLine().equalsIgnoreCase("Y");
                    
                    Map<String, Object> patternSearchResults = null;
                    
                    switch (patternOption) {
                        case 1:
                            System.out.print("Enter email domain pattern (e.g., @university.edu): ");
                            String domainPattern = sc.nextLine();
                            patternSearchResults = patternSearch.searchByEmailDomain(domainPattern, caseSensitive);
                            break;
                        case 2:
                            System.out.print("Enter area code pattern (e.g., 555 or 5**): ");
                            String areaCodePattern = sc.nextLine();
                            patternSearchResults = patternSearch.searchByPhoneAreaCode(areaCodePattern, caseSensitive);
                            break;
                        case 3:
                            System.out.print("Enter Student ID pattern (e.g., STU0** or STU???): ");
                            String idPattern = sc.nextLine();
                            patternSearchResults = patternSearch.searchByStudentIdPattern(idPattern, caseSensitive);
                            break;
                        case 4:
                            System.out.print("Enter name pattern (e.g., son): ");
                            String namePattern = sc.nextLine();
                            patternSearchResults = patternSearch.searchByNamePattern(namePattern, caseSensitive);
                            break;
                        case 5:
                            System.out.print("Enter custom regex pattern: ");
                            String customPattern = sc.nextLine();
                            patternSearchResults = patternSearch.searchByCustomPattern(customPattern, caseSensitive);
                            break;
                    }
                    
                    if (patternSearchResults != null && !patternSearchResults.containsKey("error")) {
                        @SuppressWarnings("unchecked")
                        List<PatternSearchService.SearchResult> results = 
                            (List<PatternSearchService.SearchResult>) patternSearchResults.get("results");
                        PatternSearchService.SearchStatistics stats = 
                            (PatternSearchService.SearchStatistics) patternSearchResults.get("statistics");
                        
                        System.out.println("\nSEARCH RESULTS");
                        System.out.println("Total Scanned: " + stats.getTotalScanned());
                        System.out.println("Matches Found: " + stats.getMatchesFound());
                        System.out.println("Search Time: " + stats.getSearchTime() + "ms");
                        System.out.println("Pattern Complexity: " + stats.getPatternComplexity());
                        System.out.println();
                        
                        for (PatternSearchService.SearchResult result : results) {
                            System.out.println(result.getStudent().getStudentID() + " - " + 
                                             result.getStudent().getName() + 
                                             " (" + result.getMatchedField() + ": " + 
                                             result.getHighlightedMatch() + ")");
                        }
                        
                        if (patternSearchResults.containsKey("distribution")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Integer> distribution =
                                (Map<String, Integer>) patternSearchResults.get("distribution");
                            System.out.println("\nDistribution:");
                            distribution.forEach((k, v) -> System.out.println("  " + k + ": " + v));
                        }
                    } else if (patternSearchResults != null && patternSearchResults.containsKey("error")) {
                        System.out.println("Error: " + patternSearchResults.get("error"));
                    }
                    break;


//                    QUERY GRADE HISTORY
                case 14:
                    break;

//                    SCHEDULE AUTOMATED TASKS
                case 15:
                    /**
                     * Scheduled Automated Tasks (US-6)
                     * 
                     * Manages recurring tasks:
                     * - Daily GPA recalculation
                     * - Hourly statistics cache refresh
                     * - Weekly batch report generation
                     * - Daily database backup
                     */
                    TaskScheduler taskScheduler = new TaskScheduler(
                        statisticsService, gradeService, studentService.getStudents()
                    );
                    
                    System.out.println("SCHEDULED AUTOMATED TASKS");
                    System.out.println("1. View Active Tasks");
                    System.out.println("2. Schedule New Task");
                    System.out.print("Select option (1-2): ");
                    int taskOption = Integer.parseInt(sc.nextLine());
                    
                    if (taskOption == 1) {
                        taskScheduler.displayActiveTasks();
                    } else if (taskOption == 2) {
                        System.out.println("Task Types:");
                        System.out.println("1. GPA Recalculation");
                        System.out.println("2. Statistics Cache Refresh");
                        System.out.println("3. Batch Report Generation");
                        System.out.println("4. Database Backup");
                        System.out.print("Select task type (1-4): ");
                        int taskType = Integer.parseInt(sc.nextLine());
                        
                        System.out.println("Schedule Type:");
                        System.out.println("1. Daily");
                        System.out.println("2. Hourly");
                        System.out.println("3. Weekly");
                        System.out.print("Select schedule (1-3): ");
                        int scheduleType = Integer.parseInt(sc.nextLine());
                        
                        String taskId = "TASK" + System.currentTimeMillis();
                        String taskName = "";
                        TaskScheduler.TaskType type = null;
                        TaskScheduler.ScheduleType schedType = null;
                        int scheduleValue = 1;
                        String scheduleTime = "00:00";
                        
                        switch (taskType) {
                            case 1: taskName = "GPA Recalculation"; type = TaskScheduler.TaskType.GPA_RECALCULATION; break;
                            case 2: taskName = "Statistics Cache Refresh"; type = TaskScheduler.TaskType.STATISTICS_CACHE_REFRESH; break;
                            case 3: taskName = "Batch Report Generation"; type = TaskScheduler.TaskType.BATCH_REPORT_GENERATION; break;
                            case 4: taskName = "Database Backup"; type = TaskScheduler.TaskType.DATABASE_BACKUP; break;
                        }
                        
                        switch (scheduleType) {
                            case 1: 
                                schedType = TaskScheduler.ScheduleType.DAILY;
                                System.out.print("Enter time (HH:mm): ");
                                scheduleTime = sc.nextLine();
                                break;
                            case 2:
                                schedType = TaskScheduler.ScheduleType.HOURLY;
                                System.out.print("Enter interval (hours): ");
                                scheduleValue = Integer.parseInt(sc.nextLine());
                                break;
                            case 3:
                                schedType = TaskScheduler.ScheduleType.WEEKLY;
                                System.out.print("Enter interval (weeks): ");
                                scheduleValue = Integer.parseInt(sc.nextLine());
                                break;
                        }
                        
                        taskScheduler.scheduleTask(taskId, taskName, type, schedType, scheduleValue, scheduleTime);
                        System.out.println("Task scheduled successfully!");
                    }
                    break;

//                    VIEW SYSTEM PERFORMANCE
                case 16:
                    System.out.println("System Performance metrics are displayed in the Real-Time Dashboard (Option 10).");
                    break;

//                    CACHE MANAGEMENT
                case 17:
                    /**
                     * Cache Management
                     * 
                     * Manages LRU cache:
                     * - View cache statistics
                     * - Clear cache
                     * - View cache contents
                     */
                    // Note: Cache would be initialized elsewhere and passed here
                    System.out.println("Cache Management - Implementation requires cache instance initialization.");
                    System.out.println("Cache features: LRU eviction, statistics, warming, invalidation");
                    break;

//                    AUDIT TRAIL VIEWER
                case 18:
                    /**
                     * Audit Trail Viewer (US-9)
                     * 
                     * Views and searches audit logs:
                     * - Recent entries
                     * - Filter by operation type
                     * - Filter by thread ID
                     * - Search by date range
                     * - View statistics
                     */
                    // Note: AuditTrailService should be initialized in Main and passed here
                    System.out.println("Audit Trail Viewer - Implementation requires AuditTrailService instance.");
                    System.out.println("Features: View recent entries, search, filter, statistics");
                    break;

                case 19:
                    System.out.println("Exiting program...");
                    return false;

                default:
                    System.out.println("Invalid menu choice.");
                    break;


            }
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }


        return true;
    }
}