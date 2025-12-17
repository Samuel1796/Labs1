package services.file;

import models.*;
import exceptions.*;
import services.student.StudentService;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Service class for managing grades, including recording, reporting, exporting, and importing grades.
 */
public class GradeService {
    private final Grade[] grades;
    private int gradeCount;

    /**
     * Constructs a GradeService with a specified maximum number of grades.
     * @param maxGrades Maximum number of grades that can be stored.
     */
    public GradeService(int maxGrades) {
        grades = new Grade[maxGrades];
        gradeCount = 0;
    }

    /**
     * Records a new grade in the system and ensures proper subject enrollment.
     * This method performs several critical operations:
     * 1. Validates grade capacity and value range
     * 2. Stores the grade in the array-based storage
     * 3. Automatically enrolls the student in the subject if not already enrolled
     * 4. Creates subject instances dynamically if they don't exist
     * 
     * Design Decision: Using array-based storage for O(1) insertion and predictable memory usage.
     * The grade count acts as both a counter and an index pointer.
     * 
     * @param grade Grade object to record.
     * @param studentService Service for student lookup and subject management.
     * @return true if the grade was recorded successfully.
     * @throws AppExceptions if the grade database is full (capacity exceeded).
     * @throws InvalidGradeException if grade value is outside valid range (0-100).
     */
    public boolean recordGrade(Grade grade, StudentService studentService) {
        // Capacity check: prevent array index out of bounds
        if (gradeCount >= grades.length) {
            throw new AppExceptions("Grade database full!");
        }
        
        // Range validation: grades must be between 0 and 100 (inclusive)
        if (grade.getValue() < 0 || grade.getValue() > 100) {
            throw new InvalidGradeException(grade.getValue());
        }
        
        // Store grade using gradeCount as both counter and index
        // Post-increment ensures gradeCount points to next available slot
        grades[gradeCount++] = grade;

        // Ensure the subject is enrolled for the student
        // This maintains data consistency: grades should only exist for enrolled subjects
        Student student = studentService.findStudentById(grade.getStudentID());
        Subject subject = studentService.findSubjectByNameAndType(grade.getSubjectName(), grade.getSubjectType());
        
        if (student != null) {
            // If subject doesn't exist in system, create it dynamically
            // This allows flexible subject creation without pre-registration
            if (subject == null) {
                // Factory pattern: create appropriate subject type based on classification
                if (grade.getSubjectType().equalsIgnoreCase("Core Subject")) {
                    subject = new CoreSubject(grade.getSubjectName(), grade.getSubjectType());
                } else if (grade.getSubjectType().equalsIgnoreCase("Elective Subject")) {
                    subject = new ElectiveSubject(grade.getSubjectName(), grade.getSubjectType());
                }
            }
            
            // Check if student is already enrolled to avoid duplicate enrollment
            // Using stream API for concise predicate matching
            boolean alreadyEnrolled = student.getEnrolledSubjects().stream()
                .anyMatch(s -> s.getSubjectName().equalsIgnoreCase(grade.getSubjectName())
                            && s.getSubjectType().equalsIgnoreCase(grade.getSubjectType()));
            
            // Only enroll if not already enrolled and subject was successfully created/found
            if (!alreadyEnrolled && subject != null) {
                student.enrollSubject(subject);
            }
        }
        return true;
    }

    /**
     * Optionally, keep the old method for backward compatibility
     * public boolean recordGrade(Grade grade) {
     *     throw new UnsupportedOperationException("Use recordGrade(Grade grade, StudentService studentService) instead.");
     * }
     */
    public boolean recordGrade(Grade grade) {
        throw new UnsupportedOperationException("Use recordGrade(Grade grade, StudentService studentService) instead.");
    }
/**
     * Returns the array of all grades.
     */
    public Grade[] getGrades() {
        return grades;
    }

    /**
     * Returns the current count of recorded grades.
     */
    public int getGradeCount() {
        return gradeCount;
    }

    /**
     * Sets the grade count.
     * @param count New grade count.
     */
    public void setGradeCount(int count) {
        this.gradeCount = count;
    }

    /**
     * Displays a detailed grade report for a given student.
     * @param student Student whose grades are to be reported.
     */
    public void viewGradeReport(Student student) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        System.out.printf("Student: %s - %s%n", student.getStudentID(), student.getName());
        System.out.printf("Type: %s%n", (student instanceof HonorsStudent) ? "Honors Student" : "Regular Student");

        // Collect grades for this student from the central grades array
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

            // Print each grade and accumulate statistics
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

    /**
     * Counts the number of grades recorded for a specific student.
     * @param student Student whose grades are to be counted.
     * @return Number of grades for the student.
     */
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

    // Import/Export methods moved to services.file.GradeImportExportService
    // This service now focuses on core grade management operations only

    /**
     * Checks for duplicate grades for a student and subject.
     * @param studentId Student ID.
     * @param subjectName Subject name.
     * @param subjectType Subject type.
     * @return true if a duplicate grade exists, false otherwise.
     */
    public boolean isDuplicateGrade(String studentId, String subjectName, String subjectType) {
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

    /**
     * Updates the grade value for a duplicate grade entry.
     * @param studentId Student ID.
     * @param subjectName Subject name.
     * @param subjectType Subject type.
     * @param newValue New grade value to set.
     */
    public void updateGrade(String studentId, String subjectName, String subjectType, int newValue) {
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