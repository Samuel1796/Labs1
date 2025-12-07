import models.*;
import services.StudentService;
import services.GradeService;
import services.MenuService;
import exceptions.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Scanner;


//  Main application class for Student Grade Management System.
// Delegates operations to service classes.

public class Main {
    public static void main(String[] args) {
        StudentService studentService = new StudentService(50);
        GradeService gradeService = new GradeService(500);
        MenuService menuService = new MenuService();

        // Initialize sample students and grades
        Student[] students = studentService.getStudents();
        Grade[] grades = gradeService.getGrades();
        int[] studentCountRef = {studentService.getStudentCount()};
        int[] gradeCountRef = {gradeService.getGradeCount()};
        StudentService.initializeSampleStudents(students, grades, studentCountRef, gradeCountRef);

        // Update the counts in the services
        studentService.setStudentCount(studentCountRef[0]);
        gradeService.setGradeCount(gradeCountRef[0]);

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            menuService.displayMainMenu();
            int choice = sc.nextInt();
            sc.nextLine();

            try {
                switch (choice) {
                    case 1:
                        // Add Student
                        System.out.print("Enter student name: ");
                        String name = sc.nextLine();
                        System.out.print("Enter student age: ");
                        int age = sc.nextInt();
                        sc.nextLine();
                        String email;
                        while (true) {
                            System.out.print("Enter student email: ");
                            email = sc.nextLine();
                            if (studentService.isValidEmail(email)) break;
                            System.out.println("Invalid email format. Try again.");
                        }
                        System.out.print("Enter student phone: ");
                        String phone = sc.nextLine();
                        if (studentService.isDuplicateStudent(name, email)) {
                            throw new DuplicateStudentException(name, email);
                        }
                        System.out.println("Student type: ");
                        System.out.println("1. Regular Student (Passing grade: 50%)");
                        System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
                        System.out.print("Select type (1-2): ");
                        int type = sc.nextInt();
                        sc.nextLine();
                        Student newStudent = (type == 2)
                                ? new HonorsStudent(name, age, email, phone)
                                : new RegularStudent(name, age, email, phone);
                        studentService.addStudent(newStudent);
                        System.out.println("Student added successfully!");
                        break;

                    case 2:
                        // View Students
                        studentService.viewAllStudents(gradeService);
                        break;
                    case 3:
                        // RECORD GRADE
                        // Complex logic: Handles subject selection, grade validation, and duplicate grade detection
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
                                        case 1: subject = new CoreSubject("Mathematics", "MATH101"); break;
                                        case 2: subject = new CoreSubject("English", "ENG101"); break;
                                        case 3: subject = new CoreSubject("Science", "SCI101"); break;
                                        default: throw new InvalidSubjectException("Core", String.valueOf(coreSubject));
                                    }
                                } else if (subjectType == 2) {
                                    System.out.println("Available Elective Subject:");
                                    System.out.println("1. Art");
                                    System.out.println("2. Physical Education");
                                    System.out.print("Select subject (1-2): ");
                                    int electiveSubject = sc.nextInt();
                                    sc.nextLine();
                                    switch (electiveSubject) {
                                        case 1: subject = new ElectiveSubject("Art", "ART101"); break;
                                        case 2: subject = new ElectiveSubject("Physical Education", "PE101"); break;
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
                                    Grade grade = new Grade(nextGradeID, foundStudent.getStudentID(), subject.getSubjectName(), subject.getSubjectType(), gradeValue, date);
                                    gradeService.recordGrade(grade);
                                    System.out.printf("Grade recorded successfully! Grade ID: %s%n", nextGradeID);
                                } else {
                                    System.out.println("Grade recording canceled.");
                                }
                                break; // Exit loop on success
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
                                found = true; // Exit loop if found
                            } catch (StudentNotFoundException e) {
                                System.out.println("ERROR: " + e.getMessage());
                                System.out.print("Student not found. Try again? (Y/N): ");
                                String retry = sc.nextLine();
                                if (!retry.equalsIgnoreCase("Y")) {
                                    break; // Exit loop if user doesn't want to retry
                                }
                            }
                        }
                        break;
                    case 5:
                        System.out.println("EXPORT GRADE REPORT");
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

                        System.out.printf("Student: %s - %s%n", exportStudent.getStudentID(), exportStudent.getName());
                        System.out.printf("Type: %s%n", (exportStudent instanceof HonorsStudent) ? "Honors Student" : "Regular Student");
                        System.out.println("Total Grades: " + gradeService.countGradesForStudent(exportStudent));
                        System.out.println("Export options:");
                        System.out.println("1. Summary Report (overview only)");
                        System.out.println("2. Detailed Report (all grades)");
                        System.out.println("3. Both");
                        int exportOption = 0;
                        while (exportOption < 1 || exportOption > 3) {
                            System.out.print("Select option (1-3): ");
                            try {
                                exportOption = Integer.parseInt(sc.nextLine());
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Please enter a number between 1 and 3.");
                            }
                        }
                        System.out.print("Enter filename (without extension): ");
                        String filename = sc.nextLine().trim();
                        if (filename.isEmpty()) {
                            System.out.println("Filename cannot be empty.");
                            break;
                        }
                        try {
                            String exportPath = gradeService.exportGradeReport(exportStudent, exportOption, filename);
                            System.out.println("Report exported successfully!");
                            System.out.println("File: txt");
                            System.out.println("Location: ./reports/");
                            java.io.File file = new java.io.File(exportPath);
                            double sizeKB = file.length() / 1024.0;
                            System.out.printf("Size: %.1f KB%n", sizeKB);
                            System.out.printf("Contains: %d grades, averages, performance summary%n", gradeService.countGradesForStudent(exportStudent));
                        } catch (Exception e) {
                            System.out.println("Export failed: " + e.getMessage());
                        }
                        break;

                    case 7:
                        System.out.println("BULK IMPORT GRADES");
                        System.out.println("_______________________________");
                        System.out.println("Place your CSV file in: ./imports/");
                        System.out.println("CSV Format Required:");
                        System.out.println("StudentID,SubjectName,SubjectType,Grade");
                        System.out.println("Example: STU001,Mathematics,Core,85");
                        System.out.print("Enter filename (without extension): ");
                        String importFilename = sc.nextLine().trim();
                        gradeService.bulkImportGrades(importFilename, studentService);
                    break;

                    case 9:
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
                                System.out.print("Enter name (partial or full): ");
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
                                System.out.println("Select type:");
                                System.out.println("1. Regular");
                                System.out.println("2. Honors");
                                int typeChoice = sc.nextInt();
                                sc.nextLine();
                                searchResults = studentService.searchStudentsByType(typeChoice == 2);
                                break;
                            default:
                                System.out.println("Invalid search option.");
                                break;
                        }

                        // Display results
                        System.out.println();
                        System.out.printf("SEARCH RESULTS (%d found)%n", searchResults.length);
                        System.out.println("_________________________________________________________________________________________");
                        System.out.println("| STU ID   | NAME             | TYPE     | AVG     |");
                        System.out.println("|__________________________________________________|");
                        for (Student s : searchResults) {
                            String typeStr = (s instanceof HonorsStudent) ? "Honors" : "Regular";
                             double avg = s.calculateAverage(gradeService);
                            System.out.printf("| %-8s | %-16s | %-8s | %-7.1f |\n",
                                    s.getStudentID(), s.getName(), typeStr, avg);
                        }
                        System.out.println("|__________________________________________________|");

                        // Actions
                        boolean searchMenu = true;
                        while (searchMenu) {
                            System.out.println("Actions:");
                            System.out.println("1. View full details for a student");
                            System.out.println("2. Export search results");
                            System.out.println("3. Return to main menu");
                            System.out.print("Select action (1-4): ");
                            int action = sc.nextInt();
                            sc.nextLine();

                            switch (action) {
                                case 1:
                                    System.out.print("Enter Student ID to view details: ");
                                    String detailId = sc.nextLine().trim();
                                    Student detailStudent = studentService.findStudentById(detailId);
                                    if (detailStudent != null) {
                                        gradeService.viewGradeReport(detailStudent);
                                    } else {
                                        System.out.println("Student not found.");
                                    }
                                    break;
                                case 2:
                                    System.out.print("Enter filename for export (without extension): ");
                                    String exportName = sc.nextLine().trim();
                                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("./reports/" + exportName + ".txt"))) {
                                        writer.write("SEARCH RESULTS\n");
                                        writer.write("STU ID\tNAME\tTYPE\tAVG\n");
                                        for (Student s : searchResults) {
                                            String typeStr = (s instanceof HonorsStudent) ? "Honors" : "Regular";
                                             double avg = s.calculateAverage(gradeService);
                                            writer.write(String.format("%s\t%s\t%s\t%.1f\n", s.getStudentID(), s.getName(), typeStr, avg));
                                        }
                                        System.out.println("Search results exported to ./reports/" + exportName + ".txt");
                                    } catch (Exception e) {
                                        System.out.println("Export failed: " + e.getMessage());
                                    }
                                    break;

                                case 3:
                                    searchMenu = false;
                                    break;
                                default:
                                    System.out.println("Invalid action.");
                            }
                            if (action == 4) break;
                        }
                        break;
                    case 10:
                        System.out.println("Thank you for using Student Grade Management System. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice, Try again ");
                }
                System.out.println();
            } catch (DuplicateStudentException e) {
                throw new RuntimeException(e);
            }
//            sc.close();
        }
    }
}