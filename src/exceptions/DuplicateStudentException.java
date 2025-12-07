package exceptions;

public class DuplicateStudentException extends AppExceptions {
    public DuplicateStudentException(String name, String email) {
        super("Duplicate student found: " + name + " (" + email + ")");
    }
}