package models;

import services.file.GradeService;
import utilities.StudentIdGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegularStudent implements Student, Serializable {
    private static final long serialVersionUID = 1L;
    private String studentID;
    private String name;
    private int age;
    private String email;
    private String phone;
    private String status;
    private int passingGrade;
    private double[] grades;
    private int gradeCount;
    private List<Subject> enrolledSubjects;

    public RegularStudent(String name, int age, String email, String phone) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.passingGrade = 50;
        this.studentID = StudentIdGenerator.nextId();
        this.status = "Active";
        this.grades = new double[10];
        this.gradeCount = 0;
        this.enrolledSubjects = new ArrayList<>();
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
     * Calculates the average grade for this student by aggregating all grades from GradeService.
     * 
     * @param gradeService Service containing all grades in the system
     * @return Average grade (0-100), or 0.0 if student has no grades
     */
    @Override
    public double calculateAverage(GradeService gradeService) {
        double total = 0.0;
        int count = 0;
        
        Grade[] gradesArr = gradeService.getGrades();
        int gradeCountArr = gradeService.getGradeCount();
        
        for (int i = 0; i < gradeCountArr; i++) {
            Grade g = gradesArr[i];
            
            if (g != null && g.getStudentID().equalsIgnoreCase(this.getStudentID())) {
                total += g.getValue();
                count++;
            }
        }
        
        return count > 0 ? total / count : 0.0;
    }

    @Override
    public boolean isPassing(GradeService gradeService) {
        return calculateAverage(gradeService) >= passingGrade;
    }

    @Override
    public boolean isHonorsEligible(GradeService gradeService) {
        return false;
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