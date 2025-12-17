// java
// src/Grade.java

// Represents a student's grade record with all relevant data
//  Includes grade ID, student ID, subject details, numeric value, and date
package models;
import java.io.Serializable;
import java.util.Date;

public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String gradeID;
    private final String studentID;
    private final String subjectName;
    private final String subjectType;
    private double value;
    private Date date;

    public Grade(String gradeID, String studentID, String subjectName, String subjectType, double value, Date date) {
        this.gradeID = gradeID;
        this.studentID = studentID;
        this.subjectName = subjectName;
        this.subjectType = subjectType;
        this.value = value;
        this.date = new Date(date.getTime());
    }


//    Getters to get access to the attributes
    public String getGradeID() { return gradeID; }
    public String getStudentID() { return studentID; }
    public String getSubjectName() { return subjectName; }
    public String getSubjectType() { return subjectType; }
    public double getValue() { return value; }
    public Date getDate() { return new Date(date.getTime()); }


//    Setters
public void setValue(double value) {
    this.value = value;
}

    public void setDate(java.util.Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return String.format("%s: %s - %s = %.1f on %s", gradeID, studentID, subjectName, value, date);
    }
}