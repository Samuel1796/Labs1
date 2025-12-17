package services;

import models.*;
import exceptions.DuplicateStudentException;
import exceptions.StudentNotFoundException;

import java.io.IOException;
import java.util.*;

import utilities.FileIOUtils;
import java.nio.file.Paths;

/**
 * Service class for managing student data.
 */

public class StudentService {
    // HashMap for fast student lookup by ID
    private final HashMap<String, Student> studentMap = new HashMap<>();

    /**
     * Adds a new student to the system.
     * Throws DuplicateStudentException if a student with the same ID already exists.
     * @param student The student to add
     */
    public void addStudent(Student student) throws DuplicateStudentException {
        String normalizedId = student.getStudentID().toUpperCase();
        if (studentMap.containsKey(normalizedId)) {
            throw new DuplicateStudentException(student.getName(), student.getEmail());
        }
        studentMap.put(normalizedId, student);
    }

    public Student findStudentById(String studentID) throws StudentNotFoundException {
        String normalizedId = studentID.toUpperCase();
        Student student = studentMap.get(normalizedId);
        if (student == null) {
            throw new StudentNotFoundException(studentID);
        }
        return student;
    }


    /**
     * Finds a subject by its name and type across all students.
     * @param name The subject name to search for
     * @param type The type ("Core", "Elective", etc.)
     * @return The matching Subject, or null if not found
     */
    public Subject findSubjectByNameAndType(String name, String type) {
        for (Student s : getStudents()) {
            List<Subject> enrolledSubjects = s.getEnrolledSubjects();
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
     * Returns a collection of all students.
     */
    public Collection<Student> getStudents() {
        return studentMap.values();
    }

    /**
     * Returns the total number of students.
     */
    public int getStudentCount() {
        return studentMap.size();
    }

    /**
     * Checks if a student with the same name and email already exists.
     * @return true if duplicate exists, false otherwise
     */
    public boolean isDuplicateStudent(String name, String email) {
        for (Student s : studentMap.values()) {
            if (s.getName().equalsIgnoreCase(name) && s.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the format of an email address.
     * @return true if valid, false otherwise
     */
    public boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    /**
     * Searches for students by partial or full name match.
     * @return Array of matching Student objects
     */
    public Student[] searchStudentsByName(String namePart) {
        return studentMap.values().stream()
                .filter(s -> s.getName().toLowerCase().contains(namePart.toLowerCase()))
                .toArray(Student[]::new);
    }

    /**
     * Searches for students whose average grade falls within a specified range.
     * @param min          Minimum average grade
     * @param max          Maximum average grade
     * @param gradeService GradeService instance for calculating averages
     * @return Array of matching Student objects
     */
    public Student[] searchStudentsByGradeRange(double min, double max, services.GradeService gradeService) {
        return studentMap.values().stream()
                .filter(s -> {
                    double avg = s.calculateAverage(gradeService);
                    return avg >= min && avg <= max;
                })
                .toArray(Student[]::new);
    }

    /**
     * Searches for students by type (Honors or Regular).
     * @return Array of matching Student objects
     */
    public Student[] searchStudentsByType(boolean honors) {
        return studentMap.values().stream()
                .filter(s -> honors == (s instanceof HonorsStudent))
                .toArray(Student[]::new);
    }

    /**
     * Displays all students with their basic information and average grade.
     **/
    public void viewAllStudents(services.GradeService gradeService) {
        System.out.println("________________________________________________________________________________________________________________________________________________");
        System.out.println("| STUDENT ID  | NAME                  | TYPE            | AVG GRADE | STATUS       | ENROLLED SUBJECTS    | PASSING GRADE | Honors Eligible     |");
        System.out.println("|_______________________________________________________________________________________________________________________________________________|");
    
        // Sort students by Student ID
        List<Student> sortedStudents = new ArrayList<>(studentMap.values());
        sortedStudents.sort(Comparator.comparing(Student::getStudentID, String.CASE_INSENSITIVE_ORDER));
    
        for (Student student : sortedStudents) {
            String typeStr = (student instanceof HonorsStudent) ? "Honors" : "Regular";
            double avg = student.calculateAverage(gradeService);
            System.out.printf("| %-10s | %-20s | %-15s | %-9.1f | %-12s | %-20s | %-13d | %-18s |%n",
                    student.getStudentID(),
                    student.getName(),
                    typeStr,
                    avg,
                    student.getStatus(),
                    (student.getEnrolledSubjects() != null ? student.getEnrolledSubjects().size() : 0),
                    student.getPassingGrade(),
                    (student instanceof HonorsStudent ? ((HonorsStudent)student).isHonorsEligible(gradeService) : student.isHonorsEligible(gradeService)) ? "Yes" : "No");
        }
    
        System.out.println("|=============================================================================================================================================|");
    }

    /**
     * Returns a string representation of the subjects a student is enrolled in.
     * @return Comma-separated string of subjects
     */
    public String getEnrolledSubjectsString(Student student) {
        return student.getEnrolledSubjectsString();
    }

    public void exportStudentsCSV(String filename) throws IOException {
        FileIOUtils.writeStudentsToCSV(Paths.get("./reports/" + filename + ".csv"), getStudents());
    }

    public void importStudentsCSV(String filename) throws IOException {
        List<Student> imported = FileIOUtils.readStudentsFromCSV(Paths.get("./imports/" + filename + ".csv"));
        int importedCount = 0, duplicateCount = 0;
        for (Student s : imported) {
            String normalizedId = s.getStudentID().toUpperCase();
            if (studentMap.containsKey(normalizedId) || isDuplicateStudent(s.getName(), s.getEmail())) {
                duplicateCount++;
                continue;
            }
            addStudent(s);
            importedCount++;
        }
        System.out.printf("Import completed: %d students imported, %d duplicates skipped.%n", importedCount, duplicateCount);
    }

    public void exportStudentsJSON(String filename) throws IOException {
        FileIOUtils.writeStudentsToJSON(Paths.get("./reports/" + filename + ".json"), getStudents());
    }

    public void importStudentsJSON(String filename) throws IOException {
        List<Student> imported = FileIOUtils.readStudentsFromJSON(Paths.get("./imports/" + filename + ".json"));
        int importedCount = 0, duplicateCount = 0;
        for (Student s : imported) {
            String normalizedId = s.getStudentID().toUpperCase();
            if (studentMap.containsKey(normalizedId) || isDuplicateStudent(s.getName(), s.getEmail())) {
                duplicateCount++;
                continue;
            }
            addStudent(s);
            importedCount++;
        }
        System.out.printf("Import completed: %d students imported, %d duplicates skipped.%n", importedCount, duplicateCount);
    }

    public void exportStudentsBinary(String filename) throws IOException {
        FileIOUtils.writeStudentsToBinary(Paths.get("./reports/" + filename + ".bin"), getStudents());
    }

    public void importStudentsBinary(String filename) throws IOException, ClassNotFoundException {
        List<Student> imported = FileIOUtils.readStudentsFromBinary(Paths.get("./imports/" + filename + ".bin"));
        int importedCount = 0, duplicateCount = 0;
        for (Student s : imported) {
            String normalizedId = s.getStudentID().toUpperCase();
            if (studentMap.containsKey(normalizedId) || isDuplicateStudent(s.getName(), s.getEmail())) {
                duplicateCount++;
                continue;
            }
            addStudent(s);
            importedCount++;
        }
        System.out.printf("Import completed: %d students imported, %d duplicates skipped.%n", importedCount, duplicateCount);
    }
}