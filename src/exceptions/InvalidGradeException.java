package exceptions;

public class InvalidGradeException extends AppExceptions {
    public InvalidGradeException(double grade) {
        super("Grade must be between 0 and 100. You entered: " + grade);
    }
}