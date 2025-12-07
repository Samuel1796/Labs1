package exceptions;

// Base exception for application-specific errors
public class AppExceptions extends RuntimeException {
    public AppExceptions(String message) {
        super(message);
    }
}