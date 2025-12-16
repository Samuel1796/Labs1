package services;

import models.*;
import exceptions.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class MainMenuHandler {
    private final StudentService studentService;
    private final GradeService gradeService;
    private final MenuService menuService;
    private final StatisticsService statisticsService;
    private final Scanner sc;

    public MainMenuHandler(StudentService studentService, GradeService gradeService, MenuService menuService, StatisticsService statisticsService, Scanner sc) {
        this.studentService = studentService;
        this.gradeService = gradeService;
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
                                if (studentService.isValidEmail(email)) break;
                                System.out.println("Invalid email format. Try again.");
                            }

                            String phone;
                            while (true) {
                                System.out.print("Enter student phone: ");
                                phone = sc.nextLine();
                                if (phone.matches("\\d{10}")) {
                                    break;
                                } else {
                                    System.out.println("Invalid phone number. Please enter digits only (10 digits).");
                                }
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
                            double gradeValue = sc.nextDouble();
                            sc.nextLine();
                            if (gradeValue < 0 || gradeValue > 100) {
                                throw new InvalidGradeException(gradeValue);
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
                            gradeService.exportGradeReportMultiFormat(exportStudent, reportType, filename);
                        } else if (exportFormat == 1) {
                            gradeService.exportGradesCSV(filename);
                        } else if (exportFormat == 2) {
                            gradeService.exportGradesJSON(filename);
                        } else if (exportFormat == 3) {
                            gradeService.exportGradesBinary(filename);
                        }
                        System.out.println("Report exported successfully!");
                    } catch (Exception e) {
                        System.out.println("Export failed: " + e.getMessage());
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
                    try {
                        switch (importFormat) {
                            case 1:
                                studentService.importStudentsCSV(importFilename);
                                System.out.println("Students imported from CSV.");
                                break;
                            case 2:
                                studentService.importStudentsJSON(importFilename);
                                System.out.println("Students imported from JSON.");
                                break;
                            case 3:
                                studentService.importStudentsBinary(importFilename);
                                System.out.println("Students imported from binary.");
                                break;
                            default:
                                System.out.println("Invalid format.");
                        }
                    } catch (Exception e) {
                        System.out.println("Import failed: " + e.getMessage());
                    }
                    break;

//                    BULK IMPORT
                case 7:
                    System.out.println("BULK IMPORT GRADES");
                    System.out.println("_______________________________");
                    System.out.println("Place your CSV file in: ./imports/");
                    System.out.println("CSV Format Required:");
                    System.out.println("StudentID,SubjectName,SubjectType,Grade");
                    System.out.println("Example: STU001,Mathematics,Core,85");
                    System.out.print("Enter filename (without extension): ");
                     importFilename = sc.nextLine().trim();
                    gradeService.bulkImportGrades(importFilename, studentService);
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
                    break;

//                    GENERATE BATCH REPORT
                case 11:
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

                    break;


//                    QUERY GRADE HISTORY
                case 14:
                    break;



                case 0:
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