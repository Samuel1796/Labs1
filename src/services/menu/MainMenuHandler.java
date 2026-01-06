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
<<<<<<< HEAD
import utilities.Logger;
=======
import services.system.CacheManagementService;
import services.system.AuditTrailService;
import utilities.Logger;
import utilities.CacheUtils;
>>>>>>> main

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Scanner;

public class MainMenuHandler {
    private final StudentService studentService;
    private final GradeService gradeService;
    private final GradeImportExportService gradeImportExportService;
    private final MenuService menuService;
    private final StatisticsService statisticsService;
    private final Scanner sc;
    private final AuditTrailService auditTrailService;
    private BatchReportTaskManager batchManager;

    public BatchReportTaskManager getBatchManager() {
        return batchManager;
    }

    public MainMenuHandler(StudentService studentService, GradeService gradeService, MenuService menuService, StatisticsService statisticsService, Scanner sc, AuditTrailService auditTrailService) {
        this.studentService = studentService;
        this.gradeService = gradeService;
        this.gradeImportExportService = new GradeImportExportService(gradeService);
        this.menuService = menuService;
        this.statisticsService = statisticsService;
        this.sc = sc;
        this.auditTrailService = auditTrailService;
    }
    

    /**
     * Helper method to log any operation with timing.
     */
    private void logOperation(String operationType, String action, long executionTime, boolean success, String details) {
        Logger.info(operationType + ": " + action);
        auditTrailService.logOperation(operationType, action, executionTime, success, details);
    }
    
    public boolean handleMenu(int choice) {
        // Log menu access
        Logger.debug("Menu option selected: " + choice);
        
        try {
            switch (choice) {


                case 1:
                    // Add Student
                    boolean studentAdded = false;
                    while (!studentAdded) {
                        try {
                            String name = ValidationUtils.readStringInput(sc, "Enter student name: ", false);
                            if (name == null) break;
                            
                            if (name.trim().isEmpty()) {
                                System.out.println("Name cannot be empty. Please try again.");
                                System.out.print("Try again? (Y/N): ");
                                String retry = sc.nextLine().trim();
                                if (!retry.equalsIgnoreCase("Y")) {
                                    break;
                                }
                                continue;
                            }

                            int age = ValidationUtils.readIntInput(sc, "Enter student age: ", 1, 150);

                            String email;
                            while (true) {
                                email = ValidationUtils.readStringInput(sc, "Enter student email: ", false);
                                if (email == null) {
                                    studentAdded = false;
                                    break;
                                }
                                String emailError = ValidationUtils.validateEmail(email);
                                if (emailError == null) break;
                                System.out.println(emailError);
                                System.out.print("Try again? (Y/N): ");
                                String retry = sc.nextLine().trim();
                                if (!retry.equalsIgnoreCase("Y")) {
                                    studentAdded = false;
                                    break;
                                }
                            }
                            if (email == null) break;

                            String phone;
                            while (true) {
                                phone = ValidationUtils.readStringInput(sc, "Enter student phone: ", false);
                                if (phone == null) {
                                    studentAdded = false;
                                    break;
                                }
                                String phoneError = ValidationUtils.validatePhone(phone);
                                if (phoneError == null) break;
                                System.out.println(phoneError);
                                System.out.print("Try again? (Y/N): ");
                                String retry = sc.nextLine().trim();
                                if (!retry.equalsIgnoreCase("Y")) {
                                    studentAdded = false;
                                    break;
                                }
                            }
                            if (phone == null) break;

                            if (studentService.isDuplicateStudent(name, email)) {
                                throw new DuplicateStudentException(name, email);
                            }

                            System.out.println("Student type: ");
                            System.out.println("1. Regular Student (Passing grade: 50%)");
                            System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
                            int type = ValidationUtils.readIntInput(sc, "Select type (1-2): ", 1, 2);

                            Student newStudent = (type == 2)
                                    ? new HonorsStudent(name, age, email, phone)
                                    : new RegularStudent(name, age, email, phone);
                            
                            long startTime = System.currentTimeMillis();
                            studentService.addStudent(newStudent);
                            long executionTime = System.currentTimeMillis() - startTime;
                            
                            // Log operation
                            Logger.info("Student added: " + newStudent.getStudentID() + " - " + newStudent.getName());
                            auditTrailService.logOperation("ADD_STUDENT", "Added student " + newStudent.getStudentID(), executionTime, true, "Student: " + newStudent.getName());
                            
                            // Cache student data
                            CacheUtils.getStudentCache().put("student:" + newStudent.getStudentID(), newStudent);
                            
                            System.out.println("Student added successfully!");
                            studentAdded = true;
                        } catch (DuplicateStudentException e) {
                            Logger.warn("ADD_STUDENT: Duplicate student detected - " + e.getMessage());
                            System.out.println("Duplicate student detected: " + e.getMessage());
                            System.out.print("Try again? (Y/N): ");
                            String retry = sc.nextLine().trim();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        } catch (java.util.InputMismatchException e) {
                            System.out.println("Input cancelled by user.");
                            break;
                        } catch (Exception e) {
                            Logger.error("ADD_STUDENT: Error adding student - " + e.getMessage(), e);
                            System.out.println("An error occurred: " + e.getMessage());
                            System.out.print("Try again? (Y/N): ");
                            String retry = sc.nextLine().trim();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        }
                    }
                    break;

                case 2:
                    // View Students
                    long startTime = System.currentTimeMillis();
                    studentService.viewAllStudents(gradeService);
                    long executionTime = System.currentTimeMillis() - startTime;
                    
                    Logger.info("Viewed all students - Total: " + studentService.getStudentCount());
                    auditTrailService.logOperation("VIEW_STUDENTS", "Viewed all students", executionTime, true, 
                        "Total students: " + studentService.getStudentCount());
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
                                        long updateStartTime = System.currentTimeMillis();
                                        gradeService.updateGrade(foundStudent.getStudentID(), subject.getSubjectName(), subject.getSubjectType(), (int) gradeValue);
                                        long updateExecutionTime = System.currentTimeMillis() - updateStartTime;
                                        
                                        // Log operation
                                        Logger.info("Grade updated for student " + foundStudent.getStudentID());
                                        auditTrailService.logOperation("UPDATE_GRADE", "Updated grade", updateExecutionTime, true, 
                                            "Student: " + foundStudent.getStudentID() + ", Subject: " + subject.getSubjectName() + ", New Grade: " + gradeValue);
                                        
                                        // Update cache
                                        String cacheKey = "grade:" + foundStudent.getStudentID() + ":" + subject.getSubjectName();
                                        CacheUtils.getPerformanceCache().put(cacheKey, gradeValue);
                                        
                                        System.out.printf("Grade updated successfully!%n");
                                    } else {
                                        System.out.println("Grade recording canceled due to duplicate.");
                                    }
                                } else {
                                    Grade grade = new Grade(nextGradeID, foundStudent.getStudentID(), subject.getSubjectName(), subject.getSubjectType(), gradeValue, date);
                                    
                                    long recordStartTime = System.currentTimeMillis();
                                    gradeService.recordGrade(grade, studentService);
                                    long recordExecutionTime = System.currentTimeMillis() - recordStartTime;
                                    
                                    // Log operation
                                    Logger.info("Grade recorded: " + nextGradeID + " for student " + foundStudent.getStudentID());
                                    auditTrailService.logOperation("RECORD_GRADE", "Recorded grade " + nextGradeID, recordExecutionTime, true, 
                                        "Student: " + foundStudent.getStudentID() + ", Subject: " + subject.getSubjectName() + ", Grade: " + gradeValue);
                                    
                                    // Cache grade data
                                    String cacheKey = "grade:" + foundStudent.getStudentID() + ":" + subject.getSubjectName();
                                    CacheUtils.getPerformanceCache().put(cacheKey, grade);
                                    
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
                            long viewReportStartTime = System.currentTimeMillis();
                            Student studentForReport = studentService.findStudentById(idForReport);
                            gradeService.viewGradeReport(studentForReport);
                            long viewReportExecutionTime = System.currentTimeMillis() - viewReportStartTime;
                            
                            logOperation("VIEW_GRADE_REPORT", "Viewed grade report for " + idForReport, viewReportExecutionTime, true, "Student: " + studentForReport.getName());
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
                    long exportStartTime = System.currentTimeMillis();
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
                        long exportExecutionTime = System.currentTimeMillis() - exportStartTime;
                        
                        String formatName = exportFormat == 1 ? "CSV" : exportFormat == 2 ? "JSON" : exportFormat == 3 ? "Binary" : "All formats";
                        logOperation("EXPORT_GRADE_REPORT", "Exported grade report in " + formatName + " format", exportExecutionTime, true, 
                            "Student: " + exportStudent.getStudentID() + ", File: " + filename);
                        
                        System.out.println("Report exported successfully!");
                    } catch (Exception e) {
                        long exportErrorTime = System.currentTimeMillis() - exportStartTime;
                        Logger.error("Export failed: " + e.getMessage());
                        auditTrailService.logOperation("EXPORT_GRADE_REPORT", "Export failed", exportErrorTime, false, "Error: " + e.getMessage());
                        System.out.println("Export failed: " + e.getMessage());
//                        e.printStackTrace();
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
                                long importStudentStartTime = System.currentTimeMillis();
                                studentService.addStudent(s);
                                long importStudentExecutionTime = System.currentTimeMillis() - importStudentStartTime;
                                
                                Logger.info("Student imported: " + s.getStudentID() + " - " + s.getName());
                                auditTrailService.logOperation("IMPORT_STUDENT", "Imported student " + s.getStudentID(), importStudentExecutionTime, true, "Student: " + s.getName());
                                
                                CacheUtils.getStudentCache().put("student:" + s.getStudentID(), s);
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

//                      Bulk Grade Import Handler

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
                    long bulkImportStartTime = System.currentTimeMillis();
                    try {
                        gradeImportExportService.bulkImportGrades(gradeImportFilename, formatStr, studentService);
                        long bulkImportExecutionTime = System.currentTimeMillis() - bulkImportStartTime;
                        
                        Logger.info("Bulk grade import completed from file: " + gradeImportFilename);
                        auditTrailService.logOperation("BULK_IMPORT_GRADES", "Bulk imported grades from " + gradeImportFilename, bulkImportExecutionTime, true, "Format: " + formatStr);
                    } catch (Exception e) {
                        long bulkImportErrorTime = System.currentTimeMillis() - bulkImportStartTime;
                        Logger.error("Grade import failed: " + e.getMessage());
                        auditTrailService.logOperation("BULK_IMPORT_GRADES", "Bulk import failed", bulkImportErrorTime, false, "Error: " + e.getMessage());
                        System.out.println("Grade import failed: " + e.getMessage());
                    }
                    break;


                case 8:
                    long gpaStartTime = System.currentTimeMillis();
                    Logger.info("CALCULATE_STUDENT_GPA: Starting");
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
                            Logger.warn("CALCULATE_STUDENT_GPA: Student not found - " + gpaId);
                            System.out.println("Error: " + e.getMessage());
                            System.out.print("Try again? (Y/N): ");
                            String retry = sc.nextLine();
                            if (!retry.equalsIgnoreCase("Y")) {
                                break;
                            }
                        }
                    }
<<<<<<< HEAD
                    if (!foundGPA || gpaStudent == null) {
                        long gpaDuration = System.currentTimeMillis() - gpaStartTime;
                        Logger.logAudit("CALCULATE_STUDENT_GPA", "Calculate GPA", gpaDuration, false, "Student not found or operation cancelled");
                        break;
                    }
                    statisticsService.printStudentGPAReport(gpaStudent);
                    long gpaDuration = System.currentTimeMillis() - gpaStartTime;
                    Logger.logAudit("CALCULATE_STUDENT_GPA", "Calculate GPA for " + gpaStudent.getStudentID(), gpaDuration, true, "GPA calculated successfully");
=======
                    if (!foundGPA || gpaStudent == null) break;
                    
                    long gpaStartTime = System.currentTimeMillis();
                    statisticsService.printStudentGPAReport(gpaStudent);
                    long gpaExecutionTime = System.currentTimeMillis() - gpaStartTime;
                    
                    logOperation("CALCULATE_GPA", "Calculated GPA for " + gpaStudent.getStudentID(), gpaExecutionTime, true, "Student: " + gpaStudent.getName());
>>>>>>> main
                    break;

//                    VIEW CLASS STATS
                case 9:
<<<<<<< HEAD
                    long statsStartTime = System.currentTimeMillis();
                    Logger.info("VIEW_CLASS_STATISTICS: Starting");
=======
                    // STATISTICAL ANALYSIS
                    long statsStartTime = System.currentTimeMillis();
>>>>>>> main
                    StatisticsService statsService = new StatisticsService(
                            gradeService.getGrades(),
                            gradeService.getGradeCount(),
                            studentService.getStudents(),
                            studentService.getStudentCount(),
                            gradeService
                    );
                    statsService.printStatisticsReport();
<<<<<<< HEAD
                    long statsDuration = System.currentTimeMillis() - statsStartTime;
                    Map<String, Object> statsMetrics = new java.util.HashMap<>();
                    statsMetrics.put("studentCount", studentService.getStudentCount());
                    statsMetrics.put("gradeCount", gradeService.getGradeCount());
                    Logger.logPerformance("VIEW_CLASS_STATISTICS", statsDuration, statsMetrics);
                    Logger.logAudit("VIEW_CLASS_STATISTICS", "View class statistics", statsDuration, true, "Statistics report generated");
=======
                    long statsExecutionTime = System.currentTimeMillis() - statsStartTime;
                    
                    logOperation("VIEW_STATISTICS", "Viewed class statistics", statsExecutionTime, true, 
                        "Students: " + studentService.getStudentCount() + ", Grades: " + gradeService.getGradeCount());
>>>>>>> main
                    break;


//                    REAL TIME STATS
                case 10:
                    long dashboardStartTime = System.currentTimeMillis();
                    Logger.info("REAL_TIME_STATISTICS_DASHBOARD: Starting");
                    StatisticsDashboard dashboard = new StatisticsDashboard(
                        gradeService,
                        studentService.getStudents(),
                        studentService.getStudentCount()
                    );
                    
                    menuService.setStatisticsDashboard(dashboard);
                    dashboard.start();
                    Logger.info("REAL_TIME_STATISTICS_DASHBOARD: Dashboard started");
                    
                    // Interactive dashboard loop
                    Scanner dashboardScanner = new Scanner(System.in);
                    boolean dashboardRunning = true;
                    
                    while (dashboardRunning && dashboard.isRunning()) {
                        dashboard.displayDashboard();
                        System.out.print("Command: ");
                        String command = dashboardScanner.nextLine().trim().toUpperCase();
                        
                        switch (command) {
                            case "R":
                                // Manual refresh (forces immediate recalculation)
                                dashboard.refresh();
                                break;
                            case "P":
                                // Pause or resume background auto-refresh
                                dashboard.togglePause();
                                break;
                            case "Q":
                                dashboardRunning = false;
                                dashboard.stop();
                                menuService.setStatisticsDashboard(null);
                                break;
                            default:
                                // Any other key just re-draws the dashboard
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
                    long dashboardDuration = System.currentTimeMillis() - dashboardStartTime;
                    Logger.logAudit("REAL_TIME_STATISTICS_DASHBOARD", "Real-Time Dashboard session", dashboardDuration, true, "Dashboard closed");
                    break;

//                    GENERATE BATCH REPORT
                case 11:

//                    Batch Report Generation Handler

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

                    List<Student> batchStudents = new ArrayList<>();
                    
                    if (scope == 1) {
                        batchStudents.addAll(studentService.getStudents());
                    } else if (scope == 2) {
                        System.out.print("Type (1: Regular, 2: Honors): ");
                        int type = Integer.parseInt(sc.nextLine());
                        
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

                    long batchStartTime = System.currentTimeMillis();
                    Logger.info("GENERATE_BATCH_REPORTS: Starting - Scope: " + scope + ", Format: " + format + ", Threads: " + threads);
                    BatchReportTaskManager manager = new BatchReportTaskManager(
                            batchStudents, gradeImportExportService, format, batchDir, threads
                    );
                    manager.startBatchExport();
                    long batchDuration = System.currentTimeMillis() - batchStartTime;
                    Map<String, Object> batchMetrics = new java.util.HashMap<>();
                    batchMetrics.put("studentCount", batchStudents.size());
                    batchMetrics.put("format", format);
                    batchMetrics.put("threads", threads);
                    Logger.logPerformance("GENERATE_BATCH_REPORTS", batchDuration, batchMetrics);
                    Logger.logAudit("GENERATE_BATCH_REPORTS", "Generate batch reports", batchDuration, true, 
                        "Reports generated for " + batchStudents.size() + " students");
                    break;


//
                case 12:
                    long searchStartTime = System.currentTimeMillis();
                    Logger.info("SEARCH_STUDENTS: Starting");
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

                    long searchDuration = System.currentTimeMillis() - searchStartTime;
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
                        Map<String, Object> searchMetrics = new java.util.HashMap<>();
                        searchMetrics.put("resultCount", searchResults.length);
                        searchMetrics.put("searchOption", searchOption);
                        Logger.logPerformance("SEARCH_STUDENTS", searchDuration, searchMetrics);
                        Logger.logAudit("SEARCH_STUDENTS", "Search students (Option: " + searchOption + ")", searchDuration, true, 
                            "Found " + searchResults.length + " results");
                    } else {
                        Logger.logAudit("SEARCH_STUDENTS", "Search students (Option: " + searchOption + ")", searchDuration, true, "No results found");
                    }
                    break;

//                    PATTERN BASED SEARCH
                case 13:
                    long patternStartTime = System.currentTimeMillis();
                    Logger.info("PATTERN_BASED_SEARCH: Starting");
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
//                        case 2:
//                            System.out.print("Enter area code pattern (e.g., 555 or 5**): ");
//                            String areaCodePattern = sc.nextLine();
//                            patternSearchResults = patternSearch.searchByPhoneAreaCode(areaCodePattern, caseSensitive);
//                            break;
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
                    
                    long patternDuration = System.currentTimeMillis() - patternStartTime;
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
                        
                        Map<String, Object> patternMetrics = new java.util.HashMap<>();
                        patternMetrics.put("patternOption", patternOption);
                        patternMetrics.put("caseSensitive", caseSensitive);
                        patternMetrics.put("resultCount", results.size());
                        Logger.logPerformance("PATTERN_BASED_SEARCH", patternDuration, patternMetrics);
                        Logger.logAudit("PATTERN_BASED_SEARCH", "Pattern search (Option: " + patternOption + ")", patternDuration, true, 
                            "Found " + results.size() + " matches");
                    } else if (patternSearchResults != null && patternSearchResults.containsKey("error")) {
                        Logger.logAudit("PATTERN_BASED_SEARCH", "Pattern search (Option: " + patternOption + ")", patternDuration, false, 
                            "Error: " + patternSearchResults.get("error"));
                        System.out.println("Error: " + patternSearchResults.get("error"));
                    } else {
                        Logger.logAudit("PATTERN_BASED_SEARCH", "Pattern search (Option: " + patternOption + ")", patternDuration, true, "No results found");
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
                    CacheManagementService.displayCacheManagement();
                    break;

//                    AUDIT TRAIL VIEWER
                case 18:
<<<<<<< HEAD
                    System.out.println("\n=========================================================================");
                    System.out.println("                      AUDIT TRAIL VIEWER                                ");
                    System.out.println("=========================================================================");
                    System.out.println("1. View All Logs");
                    System.out.println("2. View Recent Logs (Last 50)");
                    System.out.println("3. View Error Logs Only");
                    System.out.println("4. View Warning Logs Only");
                    System.out.println("5. View Info Logs Only");
                    System.out.println("6. View Audit Logs Only");
                    System.out.print("Select option (1-6): ");
                    
                    try {
                        int auditChoice = sc.nextInt();
                        sc.nextLine();
                        
                        List<utilities.Logger.LogEntry> logs = new ArrayList<>();
                        switch (auditChoice) {
                            case 1:
                                logs = utilities.Logger.getAllLogs();
                                break;
                            case 2:
                                logs = utilities.Logger.getRecentLogs(50);
                                break;
                            case 3:
                                logs = utilities.Logger.getLogsByLevel("ERROR");
                                break;
                            case 4:
                                logs = utilities.Logger.getLogsByLevel("WARN");
                                break;
                            case 5:
                                logs = utilities.Logger.getLogsByLevel("INFO");
                                break;
                            case 6:
                                logs = utilities.Logger.getAllLogs();
                                logs = logs.stream()
                                    .filter(log -> log.getMessage().contains("AUDIT:"))
                                    .collect(Collectors.toList());
                                break;
                            default:
                                System.out.println("Invalid option.");
                                break;
                        }
                        
                        if (auditChoice >= 1 && auditChoice <= 6) {
                            System.out.println("\nTotal entries: " + logs.size());
                            System.out.println("=========================================================================");
                            for (utilities.Logger.LogEntry entry : logs) {
                                System.out.println(entry.toString());
                            }
                            System.out.println("=========================================================================");
                        }
                    } catch (Exception e) {
                        System.out.println("Error viewing audit trail: " + e.getMessage());
                    }
=======
                    auditTrailService.displayAuditTrailViewer();
>>>>>>> main
                    break;

                case 19:
                    System.out.println("Exiting program...");
                    return false;

                default:
                    System.out.println("Invalid menu choice.");
                    break;


            }
        } catch (java.util.InputMismatchException e) {
            Logger.warn("MENU_HANDLER: Input mismatch - " + e.getMessage());
            System.out.println("Invalid input detected. Please try again.");
            return true;
        } catch (NumberFormatException e) {
            Logger.warn("MENU_HANDLER: Number format error - " + e.getMessage());
            System.out.println("Invalid number format. Please enter a valid number and try again.");
            return true;
        } catch (IllegalArgumentException e) {
            Logger.warn("MENU_HANDLER: Illegal argument - " + e.getMessage());
            System.out.println("Invalid argument: " + e.getMessage());
            System.out.println("Please check your input and try again.");
            return true;
        } catch (Exception e) {
            Logger.error("MENU_HANDLER: Unexpected error - " + e.getMessage(), e);
            System.out.println("An unexpected error occurred: " + e.getMessage());
            System.out.println("The application will continue. Please try again or contact support if the problem persists.");
            return true;
        }

        return true;
    }
}