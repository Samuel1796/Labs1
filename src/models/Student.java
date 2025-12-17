package models;

import services.file.GradeService;
import java.util.List;

public interface Student {
    void addGrade(double grade);
    double calculateAverage(GradeService gradeService);
    boolean isPassing(GradeService gradeService);
    boolean isHonorsEligible(GradeService gradeService);
    int getPassingGrade();
    int getGradeCount();
    void enrollSubject(Subject subject);
    String getEnrolledSubjectsString();

    String getStudentID();
    String getName();
    String getEmail();
    int getAge();
    String getPhone();
    String getStatus();
    List<Subject> getEnrolledSubjects();

    void setName(String name);
    void setAge(int age);
    void setEmail(String email);
    void setPhone(String phone);
    void setStatus(String status);
}