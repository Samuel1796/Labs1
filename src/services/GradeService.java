package services;
import java.util.Date;
import models.Grade;
import models.Student;
import models.HonorsStudent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

//Needed for the file writing
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

//Exception
import java.io.IOException;

public class GradeService {
    private final Grade[] grades;
    private int gradeCount;

    public GradeService(int maxGrades) {
        grades = new Grade[maxGrades];
        gradeCount = 0;
    }

    public boolean recordGrade(Grade grade) {
        if (gradeCount >= grades.length) {
            return false;
        }
        grades[gradeCount++] = grade;
        return true;
    }

    public Grade[] getGrades() {
        return grades;
    }

    public int getGradeCount() {
        return gradeCount;
    }

    public void setGradeCount(int count) {
        this.gradeCount = count;
    }

    public void viewGradeReport(Student student) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        System.out.printf("Student: %s - %s%n", student.getStudentID(), student.getName());
        System.out.printf("Type: %s%n", (student instanceof HonorsStudent) ? "Honors Student" : "Regular Student");

        // collect grades for this student from the central grades array
        List<Grade> studentGrades = new ArrayList<>();
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                studentGrades.add(g);
            }
        }

        if (studentGrades.isEmpty()) {
            System.out.printf("Passing Grade: %d%%%n", student.getPassingGrade());
            System.out.println("No grades recorded for this student.");
        } else {
            double total = 0.0;
            int count = 0;
            double coreTotal = 0;
            int coreCount = 0;
            double electiveTotal = 0;
            int electiveCount = 0;

            System.out.println("\nGRADE HISTORY");
            System.out.println("_________________________________________________________________________");
            System.out.println("| GRD ID   | DATE        | SUBJECT         | TYPE            | GRADE    |");
            System.out.println("|________________________________________________________________________|");

            for (Grade gr : studentGrades) {
                System.out.printf("| %-8s | %-10s | %-15s | %-15s | %-8.1f |%n",
                        gr.getGradeID(),
                        sdf.format(gr.getDate()),
                        gr.getSubjectName(),
                        gr.getSubjectType(),
                        gr.getValue());
                total += gr.getValue();
                count++;

                if ("Core Subject".equals(gr.getSubjectType())) {
                    coreTotal += gr.getValue();
                    coreCount++;
                } else {
                    electiveTotal += gr.getValue();
                    electiveCount++;
                }
            }
            System.out.println("|________________________________________________________________________|");

            double average = (count > 0) ? (total / count) : 0.0;
            System.out.printf("%nCurrent Average: %.1f%%%n", average);
            System.out.printf("Status: %s%n", (student.isPassing() ? "PASSING" : "FAILING"));

            System.out.println("\nTotal Grades: " + count);
            if (coreCount > 0) {
                System.out.printf("Core Subjects Average: %.1f%%%n", (coreTotal / coreCount));
            }
            if (electiveCount > 0) {
                System.out.printf("Elective Subjects Average: %.1f%%%n", (electiveTotal / electiveCount));
            }

            System.out.println("\nPerformance Summary:");
            if (student.isPassing()) {
                System.out.println("Passing all Core subjects");
                System.out.printf("Meeting passing grade requirement (%d%%)%n", student.getPassingGrade());
            }

            System.out.printf("%s - %s%n",
                    (student instanceof HonorsStudent) ? "Honors Student" : "Regular Student",
                    (student instanceof HonorsStudent ?
                            "higher standards (passing grade: 60%, eligible for honors recognition)" :
                            "standard grading (passing grade: 50%)"));
        }
    }

    // Additional methods for export, GPA, bulk import, statistics, etc. can be added here.

    public int countGradesForStudent(Student student) {
        int count = 0;
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                count++;
            }
        }
        return count;
    }

    public String exportGradeReport(Student student, int option, String filename) throws IOException {
        // Ensure reports directory exists
        File reportsDir = new File("./reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdir();
        }
        String filePath = "./reports/" + filename + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Summary
            if (option == 1 || option == 3) {
                writer.write("GRADE REPORT SUMMARY\n");
                writer.write("====================\n");
                writer.write("Student: " + student.getStudentID() + " - " + student.getName() + "\n");
                writer.write("Type: " + ((student instanceof models.HonorsStudent) ? "Honors Student" : "Regular Student") + "\n");
                writer.write("Total Grades: " + countGradesForStudent(student) + "\n");
                writer.write("Average: " + String.format("%.1f", student.calculateAverage()) + "%\n");
                writer.write("Status: " + (student.isPassing() ? "PASSING" : "FAILING") + "\n");
                writer.write("\n");
            }
            // Detailed
            if (option == 2 || option == 3) {
                writer.write("GRADE HISTORY\n");
                writer.write("=============\n");
                writer.write(String.format("%-8s %-12s %-15s %-15s %-8s\n", "GRD ID", "DATE", "SUBJECT", "TYPE", "GRADE"));
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
                for (int i = 0; i < gradeCount; i++) {
                    Grade gr = grades[i];
                    if (gr != null && gr.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                        writer.write(String.format("%-8s %-12s %-15s %-15s %-8.1f\n",
                                gr.getGradeID(),
                                sdf.format(gr.getDate()),
                                gr.getSubjectName(),
                                gr.getSubjectType(),
                                gr.getValue()));
                    }
                }
                writer.write("\n");
            }
            // Performance summary
            writer.write("Performance Summary:\n");
            if (student.isPassing()) {
                writer.write("Passing all Core subjects\n");
                writer.write("Meeting passing grade requirement (" + student.getPassingGrade() + "%)\n");
            } else {
                writer.write("Not meeting passing grade requirement (" + student.getPassingGrade() + "%)\n");
            }
            writer.write(((student instanceof models.HonorsStudent) ?
                    "Honors Student - higher standards (passing grade: 60%, eligible for honors recognition)\n" :
                    "Regular Student - standard grading (passing grade: 50%)\n"));
        }
        return filePath;
    }
}