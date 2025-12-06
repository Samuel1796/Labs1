// Student.java
// Base class representing a student with personal info, grades, and subjects
//  Contains core student management functionality

package models;
public abstract class Student {
    protected String studentID;
    protected String name;
    protected int age;
    protected String email;
    protected String phone;
    protected String status;

    protected int passingGrade;
    protected boolean honorsEligible;

    protected double[] grades;
    protected int gradeCount;

    protected Subject[] enrolledSubjects;
    protected int subjectCount;

//    Tracks total students for ID generation
    private static int studentCounter = 0;


//    public Student(String name, int age, String email, String phone) {
//        this(name, age, email, phone, 50, false);
//    }

    public Student(String name, int age, String email, String phone, int passingGrade, boolean honorsEligible) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.passingGrade = passingGrade;
        this.honorsEligible = honorsEligible;

        studentCounter++;
        this.studentID = String.format("STU%03d", studentCounter);
        this.status = "Active";

        this.grades = new double[10];
        this.gradeCount = 0;

        this.enrolledSubjects = new Subject[10];
        this.subjectCount = 0;
    }

//      Adds a grade to the student's record.

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

//      Calculates the average grade for the student.
    public double calculateAverage() {
        if (gradeCount == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < gradeCount; i++) {
            sum += grades[i];
        }
        return sum / gradeCount;
    }


    public boolean isPassing() {
        return calculateAverage() >= passingGrade;
    }


    public boolean isHonorsEligible() {
        return honorsEligible;
    }


    public int getPassingGrade() {
        return passingGrade;
    }


    public int getGradeCount() {
        return gradeCount;
    }


    public void enrollSubject(Subject subject) {
        if (subjectCount < enrolledSubjects.length) {
            enrolledSubjects[subjectCount++] = subject;
        }
    }

//     Returns a comma-separated string of enrolled subjects.
    public String getEnrolledSubjectsString() {
        if (subjectCount == 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subjectCount; i++) {
            sb.append(enrolledSubjects[i].getSubjectName());
            if (i < subjectCount - 1) sb.append(", ");
        }
        return sb.toString();
    }



    // GETTERS
    public String getStudentID() {
        return studentID;
    }

    public String getName() {
        return name;
    }


    public String getEmail() {
        return email;
    }


    public int getAge() {
        return age;
    }


    public String getPhone() {
        return phone;
    }


    public String getStatus() {
        return status;
    }

    // SETTERS

    public void setName(String name) {
        this.name = name;
    }


    public void setAge(int age) {
        this.age = age;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public void setPhone(String phone) {
        this.phone = phone;
    }


    public void setStatus(String status) {
        this.status = status;
    }
}