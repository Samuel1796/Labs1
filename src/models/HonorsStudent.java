package models;

import services.file.GradeService;
import utilities.StudentIdGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HonorsStudent implements Student, Serializable {
    private static final long serialVersionUID = 1L;
    // Fields from previous Student class
    private String studentID;
    private String name;
    private int age;
    private String email;
    private String phone;
    private String status;
    private int passingGrade;
    private boolean honorsEligible;
    private double[] grades;
    private int gradeCount;
    private List<Subject> enrolledSubjects;
    private int subjectCount;
    private static int studentCounter = 0;

    public HonorsStudent(String name, int age, String email, String phone) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.passingGrade = 60;
        this.honorsEligible = true;
        this.studentID = StudentIdGenerator.nextId();
        this.status = "Active";
        this.grades = new double[10];
        this.gradeCount = 0;
        this.enrolledSubjects = new ArrayList<>();
        this.subjectCount = 0;
    }

    @Override
    public void addGrade(double grade) {
        if (grade < 0 || grade > 100) {
            System.out.println("Invalid grade, must be between 0 and 100");
            return;
        }
        if (gradeCount < grades.length) {
            grades[gradeCount] = grade;
            gradeCount++;
            System.out.println("Grade added successfully");
        } else {
            System.out.println("Cannot add more grades, limit reached");
        }
    }

    /**
     * Calculates the average grade for this honors student by aggregating all grades from GradeService.
     * 
     * This method implements the same algorithm as RegularStudent but applies to Honors students.
     * Honors students may have different grading standards (higher passing grade), but the
     * average calculation itself is identical.
     * 
     * Algorithm:
     * 1. Retrieves all grades from centralized GradeService
     * 2. Filters grades belonging to this specific student (by student ID)
     * 3. Sums all grade values
     * 4. Calculates mean (average) by dividing sum by count
     * 
     * Design Note:
     * Honors students use the same calculation logic as regular students.
     * The difference lies in passing grade thresholds (60% vs 50%) and eligibility criteria,
     * not in how the average is computed.
     * 
     * Performance Characteristics:
     * - Time Complexity: O(n) where n is total number of grades in system
     * - Must scan all grades to find this student's grades
     * - Consider indexing by student ID for better performance with many students
     * 
     * Edge Cases:
     * - Returns 0.0 if student has no grades (prevents division by zero)
     * - Case-insensitive ID matching ensures consistency
     * - Null-safe: handles null grades in array
     * 
     * @param gradeService Service containing all grades in the system
     * @return Average grade (0-100), or 0.0 if student has no grades
     */
    @Override
    public double calculateAverage(GradeService gradeService) {
        // Initialize accumulator variables
        double total = 0.0;
        int count = 0;
        
        // Retrieve all grades from centralized storage
        // GradeService maintains a single array of all grades
        Grade[] gradesArr = gradeService.getGrades();
        int gradeCountArr = gradeService.getGradeCount();
        
        // Linear search: iterate through all grades to find this student's grades
        // This is O(n) where n is total grades - acceptable for typical class sizes
        for (int i = 0; i < gradeCountArr; i++) {
            Grade g = gradesArr[i];
            
            // Filter: only process grades belonging to this student
            // Case-insensitive matching ensures ID consistency
            if (g != null && g.getStudentID().equalsIgnoreCase(this.getStudentID())) {
                // Accumulate grade values for average calculation
                total += g.getValue();
                count++;
            }
        }
        
        // Calculate mean: sum divided by count
        // Division by zero protection: returns 0.0 if no grades found
        return count > 0 ? total / count : 0.0;
    }

    @Override
    public boolean isPassing(GradeService gradeService) {
        return calculateAverage(gradeService) >= passingGrade;
    }

    @Override
    public boolean isHonorsEligible(GradeService gradeService) {
        return calculateAverage(gradeService) >= passingGrade;
    }

    @Override
    public int getPassingGrade() {
        return passingGrade;
    }

    @Override
    public int getGradeCount() {
        return gradeCount;
    }

    @Override
    public void enrollSubject(Subject subject) {
        enrolledSubjects.add(subject);
    }

    @Override
    public String getEnrolledSubjectsString() {
        if (enrolledSubjects.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enrolledSubjects.size(); i++) {
            sb.append(enrolledSubjects.get(i).getSubjectName());
            if (i < enrolledSubjects.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public String getStudentID() {
        return studentID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public List<Subject> getEnrolledSubjects() {
        return enrolledSubjects;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }
}