package exceptions;

public class InvalidSubjectException extends AppExceptions {
    public InvalidSubjectException(String subjectName, String subjectType) {
        super("Invalid subject: " + subjectName + " (" + subjectType + ")");
    }
}