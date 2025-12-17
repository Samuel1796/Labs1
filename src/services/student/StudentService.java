package services.student;

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
     * Finds a subject by its name and type across all students in the system.
     * 
     * This method implements a linear search algorithm that:
     * 1. Iterates through all students
     * 2. Checks each student's enrolled subjects
     * 3. Matches subject name (case-insensitive) and type (by class instance)
     * 4. Returns first matching subject found
     * 
     * Search Strategy:
     * - Name matching: case-insensitive comparison for flexibility
     * - Type matching: uses instanceof to check class hierarchy
     * - Supports multiple type string formats for backward compatibility
     * 
     * Performance Characteristics:
     * - Time Complexity: O(n * m) where n = students, m = average subjects per student
     * - Space Complexity: O(1) - no additional data structures
     * - Early termination: returns immediately when match is found
     * 
     * Design Considerations:
     * - Returns first match (assumes subject instances are equivalent)
     * - Null-safe: handles null subjects and null lists gracefully
     * - Type checking uses instanceof for reliable class hierarchy detection
     * 
     * Alternative Approaches:
     * For better performance with frequent lookups, consider:
     * - Maintaining a separate subject index (HashMap)
     * - Caching subject lookups
     * - Using a subject registry service
     * 
     * @param name The subject name to search for (case-insensitive)
     * @param type The type ("Core", "Elective", "Core Subject", "Elective Subject")
     * @return The matching Subject instance, or null if not found
     */
    public Subject findSubjectByNameAndType(String name, String type) {
        // Linear search: iterate through all students
        // This approach is simple but O(n*m) - acceptable for typical class sizes
        for (Student s : getStudents()) {
            List<Subject> enrolledSubjects = s.getEnrolledSubjects();
            
            // Null-safe check: handle students with no enrolled subjects
            if (enrolledSubjects != null) {
                // Check each subject enrolled by this student
                for (Subject subj : enrolledSubjects) {
                    // Null-safe check: skip null subjects (defensive programming)
                    if (subj != null && subj.getSubjectName().equalsIgnoreCase(name)) {
                        // Type matching: check class hierarchy using instanceof
                        // Supports multiple string formats for backward compatibility
                        if ((type.equalsIgnoreCase("Core") && subj instanceof CoreSubject) ||
                                (type.equalsIgnoreCase("Elective") && subj instanceof ElectiveSubject) ||
                                (type.equalsIgnoreCase("Core Subject") && subj instanceof CoreSubject) ||
                                (type.equalsIgnoreCase("Elective Subject") && subj instanceof ElectiveSubject)) {
                            // Match found: return first matching subject
                            // Early termination: exits as soon as match is found
                            return subj;
                        }
                    }
                }
            }
        }
        // No match found: return null
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
     * Searches for students by partial or full name match using case-insensitive substring matching.
     * 
     * This method uses Java Stream API for functional-style filtering:
     * - Converts student collection to stream
     * - Filters students whose name contains the search term (case-insensitive)
     * - Collects results into an array
     * 
     * Search Characteristics:
     * - Case-insensitive: "john" matches "John", "JOHN", "johnny"
     * - Substring matching: "john" matches "John Doe", "Johnny", "Little John"
     * - Partial matching: finds students even if search term is part of full name
     * 
     * Performance:
     * - Time Complexity: O(n) where n is number of students
     * - Space Complexity: O(m) where m is number of matches
     * - Stream operations are lazy and efficient
     * 
     * Use Cases:
     * - Quick name lookup: "Find students with 'Smith' in name"
     * - Fuzzy search: "Find students with 'john' anywhere in name"
     * - Autocomplete: "Find students starting with 'A'"
     * 
     * Limitations:
     * - No ranking: results are in arbitrary order
     * - No exact match prioritization: "John" and "Johnny" treated equally
     * - No fuzzy matching: typos won't be caught
     * 
     * @param namePart Search term (partial or full name, case-insensitive)
     * @return Array of Student objects whose names contain the search term
     */
    public Student[] searchStudentsByName(String namePart) {
        // Stream API approach: functional-style filtering
        // More readable and concise than traditional loops
        return studentMap.values().stream()
                // Filter: keep only students whose name contains search term
                // Case-insensitive: convert both to lowercase for comparison
                // contains() performs substring matching (not just prefix/suffix)
                .filter(s -> s.getName().toLowerCase().contains(namePart.toLowerCase()))
                // Collect results into array
                // toArray() creates new array with exact size needed
                .toArray(Student[]::new);
    }

    /**
     * Searches for students whose average grade falls within a specified range (inclusive).
     * 
     * This method implements range-based filtering:
     * - Calculates each student's average grade using GradeService
     * - Filters students whose average is within [min, max] range
     * - Returns all matching students
     * 
     * Range Matching:
     * - Inclusive bounds: students with exactly min or max are included
     * - Range validation: assumes min <= max (caller's responsibility)
     * - Handles edge cases: min = max finds students with exact average
     * 
     * Performance Considerations:
     * - Time Complexity: O(n) where n is number of students
     * - Each student requires average calculation: O(g) where g is grades per student
     * - Overall: O(n * g) - can be expensive for large datasets
     * 
     * Optimization Opportunities:
     * - Cache average grades to avoid recalculation
     * - Pre-compute averages during grade updates
     * - Use indexed data structure for range queries
     * 
     * Use Cases:
     * - Find high performers: min=90, max=100
     * - Find students needing support: min=0, max=60
     * - Find average students: min=70, max=80
     * 
     * @param min Minimum average grade (inclusive, 0-100)
     * @param max Maximum average grade (inclusive, 0-100)
     * @param gradeService Service for calculating student averages
     * @return Array of Student objects whose average grade is within the specified range
     */
    public Student[] searchStudentsByGradeRange(double min, double max, services.file.GradeService gradeService) {
        // Stream API with range filtering
        return studentMap.values().stream()
                .filter(s -> {
                    // Calculate average for this student
                    // This is called for every student - consider caching for performance
                    double avg = s.calculateAverage(gradeService);
                    
                    // Range check: inclusive bounds
                    // Returns true if average is within [min, max]
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
     * Performance:
     * - Time Complexity: O(n) where n is number of students
     * - Space Complexity: O(m) where m is number of matches
     * - instanceof is a fast operation (O(1) per check)
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
        // Stream API with type filtering
        return studentMap.values().stream()
                // Type check: compare desired type with actual instance type
                // honors == (s instanceof HonorsStudent) ensures boolean match
                // If honors=true, keeps only HonorsStudent instances
                // If honors=false, keeps only RegularStudent instances
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