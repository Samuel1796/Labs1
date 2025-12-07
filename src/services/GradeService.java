package services;
import java.io.*;
import java.util.Date;
import models.Grade;
import models.Student;
import models.HonorsStudent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import models.Subject;
import exceptions.*;


//Needed for the file writing

//Exception
class DuplicateGradeException extends Exception {
    public DuplicateGradeException(String message) {
        super(message);
    }
}


public class GradeService {
    private final Grade[] grades;
    private int gradeCount;

    public GradeService(int maxGrades) {
        grades = new Grade[maxGrades];
        gradeCount = 0;
    }

    public boolean recordGrade(Grade grade) {
        if (gradeCount >= grades.length) {
            throw new AppExceptions("Grade database full!");
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

            System.out.println("GRADE HISTORY");
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
            System.out.printf("Status: %s%n", (student.isPassing(this) ? "PASSING" : "FAILING"));

            System.out.println("\nTotal Grades: " + count);
            if (coreCount > 0) {
                System.out.printf("Core Subjects Average: %.1f%%%n", (coreTotal / coreCount));
            }
            if (electiveCount > 0) {
                System.out.printf("Elective Subjects Average: %.1f%%%n", (electiveTotal / electiveCount));
            }

            System.out.println("\nPerformance Summary:");
            if (student.isPassing(this)) {
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
                writer.write("Average: " + String.format("%.1f", student.calculateAverage(this)) + "%\n");
                writer.write("Status: " + (student.isPassing(this) ? "PASSING" : "FAILING") + "\n");
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
            if (student.isPassing(this)) {
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


    public void bulkImportGrades(String filename, StudentService studentService) {
        String dirPath = "./imports/";
        String filePath = dirPath + filename + ".csv";
        File file = new File(filePath);
    
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return;
        }
    
        System.out.println("Validating file...");
        int totalRows = 0;
        int successCount = 0;
        int failCount = 0;
        List<String> failedRecords = new ArrayList<>();
        List<String> failReasons = new ArrayList<>();
    
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Skip the header row
            String header = br.readLine();

            String line;
            int rowNum = 2;
            while ((line = br.readLine()) != null) {
                totalRows++;
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": Invalid format");
                    rowNum++;
                    continue;
                }
                String studentId = parts[0].trim();
                String subjectName = parts[1].trim();
                String subjectType = parts[2].trim();
                String gradeStr = parts[3].trim();
    
                Student student;
                try {
                    student = studentService.findStudentById(studentId);
                } catch (StudentNotFoundException e) {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": " + e.getMessage());
                    rowNum++;
                    continue;
                }
    
                Subject subject = studentService.findSubjectByNameAndType(subjectName, subjectType);
                if (subject == null) {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": Invalid subject (" + subjectName + ", " + subjectType + ")");
                    rowNum++;
                    continue;
                }
    
                int gradeValue;
                try {
                    gradeValue = Integer.parseInt(gradeStr);
                    if (gradeValue < 0 || gradeValue > 100) {
                        throw new InvalidGradeException(gradeValue);
                    }
                } catch (Exception e) {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": " + e.getMessage());
                    rowNum++;
                    continue;
                }
    
                if (isDuplicateGrade(studentId, subjectName, subjectType)) {
                    System.out.printf("ROW %d: Duplicate grade found for student %s and subject %s (%s). Overwrite with new value? [Y/N]: ", rowNum, studentId, subjectName, subjectType);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String response = reader.readLine();
                    if (response.equalsIgnoreCase("Y")) {
                        updateGrade(studentId, subjectName, subjectType, gradeValue);
                        successCount++;
                        System.out.println("Grade updated with new value.");
                    } else {
                        failCount++;
                        failedRecords.add("ROW " + rowNum + ": Duplicate grade not updated.");
                    }
                } else {
                    Grade grade = new Grade(
                            "GRD0" + (getGradeCount() + 1),
                            studentId,
                            subjectName,
                            subjectType,
                            gradeValue,
                            new java.util.Date()
                    );
                    recordGrade(grade);
                    successCount++;
                }
                rowNum++;
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }
    
        // Import summary
        System.out.println("IMPORT SUMMARY");
        System.out.println("Total Rows: " + totalRows);
        System.out.println("Successfully Imported: " + successCount);
        System.out.println("Failed: " + failCount);
        if (failCount > 0) {
            System.out.println("Failed Records:");
            for (String fail : failedRecords) {
                System.out.println(fail);
            }
        }
        System.out.println("Import completed!");
        System.out.println(successCount + " grades added to system");
    }

    //Check for grade duplicates when importing
    private boolean isDuplicateGrade(String studentId, String subjectName, String subjectType) {
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null &&
                    g.getStudentID().equalsIgnoreCase(studentId) &&
                    g.getSubjectName().equalsIgnoreCase(subjectName) &&
                    g.getSubjectType().equalsIgnoreCase(subjectType)) {
                return true;
            }
        }
        return false;
    }







    // Helper method to update the grade value for a duplicate
    private void updateGrade(String studentId, String subjectName, String subjectType, int newValue) {
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null &&
                g.getStudentID().equalsIgnoreCase(studentId) &&
                g.getSubjectName().equalsIgnoreCase(subjectName) &&
                g.getSubjectType().equalsIgnoreCase(subjectType)) {
                g.setValue(newValue);
                g.setDate(new java.util.Date()); // Optionally update the date to now
                break;
            }
        }
    }
}