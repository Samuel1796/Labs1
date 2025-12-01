// java
// src/Grade.java
import java.util.Date;

public class Grade {
    private final String gradeID;
    private final String studentID;
    private final String subjectName;
    private final String subjectType;
    private final double value;
    private final Date date;

    public Grade(String gradeID, String studentID, String subjectName, String subjectType, double value, Date date) {
        this.gradeID = gradeID;
        this.studentID = studentID;
        this.subjectName = subjectName;
        this.subjectType = subjectType;
        this.value = value;
        this.date = new Date(date.getTime());
    }

    public String getGradeID() { return gradeID; }
    public String getStudentID() { return studentID; }
    public String getSubjectName() { return subjectName; }
    public String getSubjectType() { return subjectType; }
    public double getValue() { return value; }
    public Date getDate() { return new Date(date.getTime()); }

    @Override
    public String toString() {
        return String.format("%s: %s - %s = %.1f on %s", gradeID, studentID, subjectName, value, date);
    }
}
