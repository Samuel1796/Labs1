package services;

import models.Student;
import models.HonorsStudent;
import models.RegularStudent;
import models.Grade;
import models.Subject;
import models.CoreSubject;
import models.ElectiveSubject;
import exceptions.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles student-related operations such as adding, searching, and listing students.
 */
public class StudentService {
    private final Student[] students;
    private int studentCount;

//    REGULAR EXPRESSION FOR EMAIL VALIDATION
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"
    );

    /**
     * Constructs a StudentService with a specified maximum number of students.
     * @param maxStudents Maximum number of students that can be stored.
     */
    public StudentService(int maxStudents) {
        students = new Student[maxStudents];
        studentCount = 0;
    }

    /**
     * Validates the format of an email address.
     * @param email Email address to validate.
     * @return true if the email is valid, false otherwise.
     */
    public boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if a student with the given name and email already exists.
     * @param name Student name.
     * @param email Student email.
     * @return true if duplicate exists, false otherwise.
     */
    public boolean isDuplicateStudent(String name, String email) {
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            if (s.getName().equalsIgnoreCase(name) && s.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new student to the system.
     * @param student Student object to add.
     * @return true if added successfully.
     * @throws AppExceptions if the student database is full.
     */
    public boolean addStudent(Student student) {
        if (studentCount >= students.length) {
            throw new AppExceptions("Student database full!");
        }
        if (isDuplicateStudent(student.getName(), student.getEmail())) {
            throw new DuplicateStudentException(student.getName(), student.getEmail());
        }
        students[studentCount++] = student;
        return true;
    }

    /**
     * Finds a student by their ID.
     * @param studentID Student ID to search for.
     * @return Student object if found.
     * @throws StudentNotFoundException if not found.
     */
    public Student findStudentById(String studentID) {
        for (int i = 0; i < studentCount; i++) {
            String currentId = students[i].getStudentID();
            if (currentId != null && currentId.equalsIgnoreCase(studentID)) {
                return students[i];
            }
        }
        throw new StudentNotFoundException(studentID);
    }

    /**
     * Searches for students by partial or full name.
     * @param namePart Name or part of name to search.
     * @return Array of matching Student objects.
     */
    public Student[] searchStudentsByName(String namePart) {
        List<Student> results = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            if (s.getName().toLowerCase().contains(namePart.toLowerCase())) {
                results.add(s);
            }
        }
        return results.toArray(new Student[0]);
    }

    /**
     * Searches for students whose average grade falls within a specified range.
     * @param min Minimum average grade.
     * @param max Maximum average grade.
     * @param gradeService GradeService instance for grade lookup.
     * @return Array of matching Student objects.
     */
    public Student[] searchStudentsByGradeRange(double min, double max, GradeService gradeService) {
        List<Student> results = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            double avg = s.calculateAverage(gradeService);
            if (avg >= min && avg <= max) {
                results.add(s);
            }
        }
        return results.toArray(new Student[0]);
    }

    /**
     * Searches for students by type (Honors or Regular).
     * @param honors true for Honors students, false for Regular students.
     * @return Array of matching Student objects.
     */
    public Student[] searchStudentsByType(boolean honors) {
        List<Student> results = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            if ((honors && s instanceof HonorsStudent) || (!honors && s instanceof RegularStudent)) {
                results.add(s);
            }
        }
        return results.toArray(new Student[0]);
    }

    /**
     * Finds a subject by its name and type for any enrolled student.
     * @param name Subject name.
     * @param type Subject type ("Core", "Elective", etc.).
     * @return Subject object if found, null otherwise.
     */
    public Subject findSubjectByNameAndType(String name, String type) {
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            Subject[] enrolledSubjects = s.getEnrolledSubjects();
            if (enrolledSubjects != null) {
                for (Subject subj : enrolledSubjects) {
                    if (subj != null && subj.getSubjectName().equalsIgnoreCase(name)) {
                        if ((type.equalsIgnoreCase("Core") && subj instanceof CoreSubject) ||
                                (type.equalsIgnoreCase("Elective") && subj instanceof ElectiveSubject) ||
                                (type.equalsIgnoreCase("Core Subject") && subj instanceof CoreSubject) ||
                                (type.equalsIgnoreCase("Elective Subject") && subj instanceof ElectiveSubject)) {
                            return subj;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the current count of students.
     * @return Number of students.
     */
    public int getStudentCount() {
        return studentCount;
    }

    /**
     * Gets the array of all students.
     * @return Array of Student objects.
     */
    public Student[] getStudents() {
        return students;
    }

    /**
     * Displays a formatted list of all students and their statistics.
     * @param gradeService GradeService instance for grade lookup.
     */
    public void viewAllStudents(GradeService gradeService) {
        if (studentCount == 0) {
            System.out.println("No students available.");
            return;
        }
        System.out.println();
        System.out.println("STUDENT LISTING");
        System.out.println("________________________________________________________________________________________________________________________________________________");
        System.out.println("| STUDENT ID  | NAME                  | TYPE            | AVG GRADE | STATUS       | ENROLLED SUBJECTS    | PASSING GRADE | Honors Eligible     |");
        System.out.println("|_______________________________________________________________________________________________________________________________________________|");
        for (int i = 0; i < studentCount; i++) {
            Student student = students[i];
            double avg = student.calculateAverage(gradeService);
            System.out.printf("| %-10s | %-20s | %-15s | %-9.1f | %-12s | %-20s | %-13d | %-18s |%n",
                    student.getStudentID(),
                    student.getName(),
                    (student instanceof HonorsStudent) ? "Honors" : "Regular",
                    avg,
                    student.getStatus(),
                    student.getEnrolledSubjectsString().length(),
                    student.getPassingGrade(),
                    (student instanceof HonorsStudent ? ((HonorsStudent)student).isHonorsEligible(gradeService) : student.isHonorsEligible()) ? "Yes" : "No");
        }
        System.out.println("|=============================================================================================================================================|");
    }

    /**
     * Static method to initialize sample students and grades for demonstration or testing.
     * @param students Array to store students.
     * @param grades Array to store grades.
     * @param studentCountRef Reference to student count (as single-element array).
     * @param gradeCountRef Reference to grade count (as single-element array).
     */
    public static void initializeSampleStudents(Student[] students, Grade[] grades, int[] studentCountRef, int[] gradeCountRef) {
        // Core subjects
        Subject math = new CoreSubject("Mathematics", "MATH101");
        Subject english = new CoreSubject("English", "ENG101");
        Subject science = new CoreSubject("Science", "SCI101");
        // Elective subjects
        Subject art = new ElectiveSubject("Art", "ART101");
        Subject pe = new ElectiveSubject("Physical Education", "PE101");

        Date now = new Date();

        // Add 3 regular students
        Student s1 = new RegularStudent("Kofi Mensah", 20, "john@example.com", "1234567890");
        s1.enrollSubject(math); s1.enrollSubject(english); s1.enrollSubject(science);
        s1.addGrade(100); grades[gradeCountRef[0]++] = new Grade("GRD001", s1.getStudentID(), "Mathematics", "Core Subject", 100, now);
        s1.addGrade(100); grades[gradeCountRef[0]++] = new Grade("GRD002", s1.getStudentID(), "English", "Core Subject", 100, now);
        s1.addGrade(100); grades[gradeCountRef[0]++] = new Grade("GRD003", s1.getStudentID(), "Science", "Core Subject", 100, now);

        Student s2 = new RegularStudent("Yaa Agyei", 21, "jane@example.com", "2345678901");
        s2.enrollSubject(math); s2.enrollSubject(english); s2.enrollSubject(art);
        s2.addGrade(60); grades[gradeCountRef[0]++] = new Grade("GRD004", s2.getStudentID(), "Mathematics", "Core Subject", 60, now);
        s2.addGrade(55); grades[gradeCountRef[0]++] = new Grade("GRD005", s2.getStudentID(), "English", "Core Subject", 55, now);
        s2.addGrade(70); grades[gradeCountRef[0]++] = new Grade("GRD006", s2.getStudentID(), "Art", "Elective Subject", 70, now);

        Student s3 = new RegularStudent("John Cena", 22, "mike@example.com", "3456789012");
        s3.enrollSubject(science); s3.enrollSubject(art); s3.enrollSubject(pe);
        s3.addGrade(40); grades[gradeCountRef[0]++] = new Grade("GRD007", s3.getStudentID(), "Science", "Core Subject", 40, now);
        s3.addGrade(50); grades[gradeCountRef[0]++] = new Grade("GRD008", s3.getStudentID(), "Art", "Elective Subject", 50, now);
        s3.addGrade(60); grades[gradeCountRef[0]++] = new Grade("GRD009", s3.getStudentID(), "Physical Education", "Elective Subject", 60, now);

        // Add 2 honors students
        Student s4 = new HonorsStudent("Afia Oduro", 20, "sarah@example.com", "4567890123");
        s4.enrollSubject(math); s4.enrollSubject(english); s4.enrollSubject(pe);
        s4.addGrade(85); grades[gradeCountRef[0]++] = new Grade("GRD010", s4.getStudentID(), "Mathematics", "Core Subject", 85, now);
        s4.addGrade(90); grades[gradeCountRef[0]++] = new Grade("GRD011", s4.getStudentID(), "English", "Core Subject", 90, now);
        s4.addGrade(88); grades[gradeCountRef[0]++] = new Grade("GRD012", s4.getStudentID(), "Physical Education", "Elective Subject", 88, now);

        Student s5 = new HonorsStudent("David Goliath", 21, "david@example.com", "5678901234");
        s5.enrollSubject(science); s5.enrollSubject(art); s5.enrollSubject(pe);
        s5.addGrade(70); grades[gradeCountRef[0]++] = new Grade("GRD013", s5.getStudentID(), "Science", "Core Subject", 70, now);
        s5.addGrade(65); grades[gradeCountRef[0]++] = new Grade("GRD014", s5.getStudentID(), "Art", "Elective Subject", 65, now);
        s5.addGrade(75); grades[gradeCountRef[0]++] = new Grade("GRD015", s5.getStudentID(), "Physical Education", "Elective Subject", 75, now);

        students[studentCountRef[0]++] = s1;
        students[studentCountRef[0]++] = s2;
        students[studentCountRef[0]++] = s3;
        students[studentCountRef[0]++] = s4;
        students[studentCountRef[0]++] = s5;
    }

    /**
     * Sets the student count (used for administrative purposes).
     * @param count New student count.
     */
    public void setStudentCount(int count) {
        this.studentCount = count;
    }
}