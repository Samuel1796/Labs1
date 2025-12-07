package exceptions;

public class StudentNotFoundException extends AppExceptions {
    public StudentNotFoundException(String studentId) {
        super("Student with ID '" + studentId + "' not found in the system.");
    }
}