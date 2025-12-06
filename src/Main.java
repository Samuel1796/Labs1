import services.StudentService;
import services.GradeService;
import services.MenuService;

import models.Student;
import models.HonorsStudent;
import models.RegularStudent;
import models.Grade;

import java.util.Scanner;

/**
 * Main application class for Student Grade Management System.
 * Delegates operations to service classes.
 */
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
                        System.out.println("Duplicate student found.");
                        break;
                    }
                    System.out.println("Student type: ");
                    System.out.println("1. Regular Student (Passing grade: 50%)");
                    System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
                    System.out.print("Select type (1-2): ");                    int type = sc.nextInt();
                    sc.nextLine();
                    Student newStudent = (type == 2)
                            ? new HonorsStudent(name, age, email, phone)
                            : new RegularStudent(name, age, email, phone);
                    if (studentService.addStudent(newStudent)) {
                        System.out.println("Student added successfully!");
                    } else {
                        System.out.println("Student database full!");
                    }
                    break;
                case 2:
                    // View Students
                    studentService.viewAllStudents();
                    break;
                case 3:
                    // Record Grade
                    System.out.print("Enter Student ID: ");
                    String studentID = sc.nextLine();
                    Student foundStudent = studentService.findStudentById(studentID);
                    if (foundStudent == null) {
                        System.out.println("Student not found!");
                        break;
                    }
                    System.out.print("Enter Subject Name: ");
                    String subjectName = sc.nextLine();
                    System.out.print("Enter Subject Type: ");
                    String subjectType = sc.nextLine();
                    System.out.print("Enter Grade Value: ");
                    double value = sc.nextDouble();
                    sc.nextLine();
                    Grade grade = new Grade("G" + (gradeService.getGradeCount() + 1), studentID, subjectName, subjectType, value, new java.util.Date());
                    if (gradeService.recordGrade(grade)) {
                        System.out.println("Grade recorded successfully!");
                    } else {
                        System.out.println("Grade database full!");
                    }
                    break;
                case 4:
                    // View Grade Report
                    System.out.print("Enter Student ID: ");
                    String idForReport = sc.nextLine();
                    Student studentForReport = studentService.findStudentById(idForReport);
                    if (studentForReport == null) {
                        System.out.println("Student not found!");
                        break;
                    }
                    gradeService.viewGradeReport(studentForReport);
                    break;
                case 5:
                    System.out.println("EXPORT GRADE REPORT");
                    System.out.println("_____________________________");

                    System.out.println();
                    System.out.print("Enter Student ID: ");
                    String exportStudentID = sc.nextLine();
                    Student exportStudent = studentService.findStudentById(exportStudentID);
                    if (exportStudent == null) {
                        System.out.println("Student not found!");
                        break;
                    }
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
                case 10:
                    System.out.println("Thank you for using Student Grade Management System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice, Try again ");
            }
            System.out.println();
        }
        sc.close();
    }
}