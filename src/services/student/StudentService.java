package services.student;

import models.*;
import exceptions.DuplicateStudentException;
import exceptions.StudentNotFoundException;

import java.util.*;

/**
 * Service class for managing student data.
 */

public class StudentService {
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
     * Finds a subject by its name and type across all students in the system.
     * 
     * @param name The subject name to search for (case-insensitive)
     * @param type The type ("Core", "Elective", "Core Subject", "Elective Subject")
     * @return The matching Subject instance, or null if not found
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
     * Searches for students by partial or full name match using case-insensitive substring matching.
     * 
     * - Converts student collection to stream
     * - Filters students whose name contains the search term (case-insensitive)
     * - Collects results into an array
     * 
     * Search Characteristics:
     * - Case-insensitive: "john" matches "John", "JOHN", "johnny"
     * - Substring matching: "john" matches "John Doe", "Johnny", "Little John"
     * - Partial matching: finds students even if search term is part of full name

     * Limitations:
     * - No ranking: results are in arbitrary order
     * - No exact match prioritization: "John" and "Johnny" treated equally
     *
     * @param namePart Search term (partial or full name, case-insensitive)
     * @return Array of Student objects whose names contain the search term
     */
    public Student[] searchStudentsByName(String namePart) {
        return studentMap.values().stream()
                .filter(s -> s.getName().toLowerCase().contains(namePart.toLowerCase()))
                .toArray(Student[]::new);
    }

    /**
     * Searches for students whose average grade falls within a specified range (inclusive).
     * 
     * @param min Minimum average grade (inclusive, 0-100)
     * @param max Maximum average grade (inclusive, 0-100)
     * @param gradeService Service for calculating student averages
     * @return Array of Student objects whose average grade is within the specified range
     */
    public Student[] searchStudentsByGradeRange(double min, double max, services.file.GradeService gradeService) {
        return studentMap.values().stream()
                .filter(s -> {
                    double avg = s.calculateAverage(gradeService);
                    return avg >= min && avg <= max;
                })
                .toArray(Student[]::new);
    }


    /**
     * Searches for students by type (Honors or Regular) using instanceof check.
     * 
     * This method filters students based on their class hierarchy:
     * - Honors students: instances of HonorsStudent class
     * - Regular students: instances of RegularStudent class
     * 
     * Type Detection:
     * - Uses instanceof operator for reliable class hierarchy checking
     * - More reliable than string comparison or flag checking
     * - Works correctly with inheritance and polymorphism
     * 

     * Use Cases:
     * - Filter honors students: honors=true
     * - Filter regular students: honors=false
     * - Generate type-specific reports
     * - Apply type-specific policies
     * 
     * @param honors If true, returns Honors students; if false, returns Regular students
     * @return Array of Student objects matching the specified type
     */
    public Student[] searchStudentsByType(boolean honors) {
        return studentMap.values().stream()
                .filter(s -> honors == (s instanceof HonorsStudent))
                .toArray(Student[]::new);
    }

    /**
     * Displays all students with their basic information and average grade.
     **/
    public void viewAllStudents(services.file.GradeService gradeService) {
        System.out.println("________________________________________________________________________________________________________________________________________________");
        System.out.println("| STUDENT ID  | NAME                  | TYPE            | AVG GRADE | STATUS       | ENROLLED SUBJECTS    | PASSING GRADE | Honors Eligible     |");
        System.out.println("|_______________________________________________________________________________________________________________________________________________|");
    
        List<Student> sortedStudents = new ArrayList<>(studentMap.values());
        sortedStudents.sort(Comparator.comparing(Student::getStudentID, String.CASE_INSENSITIVE_ORDER));

        if (sortedStudents.isEmpty()) {
            System.out.println("| No students found. Please add students first.                                                                                               |");
            System.out.println("|=============================================================================================================================================|");
            return;
        }
    
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










}