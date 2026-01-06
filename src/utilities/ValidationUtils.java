package utilities;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Utility class for comprehensive input validation using regex patterns.
 */
public class ValidationUtils {
    
<<<<<<< HEAD
=======
    // Compiled regex patterns
>>>>>>> main
    
    /** Student ID Pattern: STU followed by exactly 3 digits (e.g., STU001, STU042, STU999) */
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^STU\\d{3}$");
    
    /** Email Pattern: Standard email format validation */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    /**
     * Phone Pattern: Ghana phone numbers.
     *
     * Supported formats:
     * - Local:        0XXXXXXXXX      (10 digits, starts with 0)
     * - International:+233XXXXXXXXX   (country code + 9 digits, no leading 0)
     *
     * Examples:
     * - 0241234567
     * - 0509876543
     * - +233241234567
     * - +233509876543
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(0\\d{9}|\\+233\\d{9})$"
    );
    
    
    /**
     * Validates a student ID against the pattern STU###.
     * 
     * @param studentId The student ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidStudentId(String studentId) {
        if (studentId == null) return false;
        return STUDENT_ID_PATTERN.matcher(studentId.trim()).matches();
    }
    
    /**
     * Validates an email address.
     * 
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validates a phone number in any supported format.
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    
    /**
     * Validates a grade value (0-100) using range check.
     * 
     * @param grade The grade value as string
     * @return true if valid, false otherwise
     */
    public static boolean isValidGrade(String grade) {
        if (grade == null) return false;
        try {
            double gradeValue = Double.parseDouble(grade.trim());
            return gradeValue >= 0 && gradeValue <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates a grade value (0-100) from numeric input.
     * 
     * @param grade The grade value as double
     * @return true if valid, false otherwise
     */
    public static boolean isValidGrade(double grade) {
        return grade >= 0 && grade <= 100;
    }
    
    /**
     * Gets validation error message with expected pattern and examples.
     * 
     * @param fieldName The name of the field being validated
     * @param value The invalid value
     * @param patternType The type of pattern (studentId, email, phone, etc.)
     * @return Detailed error message with examples
     */
    public static String getValidationErrorMessage(String fieldName, String value, String patternType) {
        StringBuilder error = new StringBuilder();
        error.append("Invalid ").append(fieldName).append(": '").append(value).append("'\n");
        error.append("Expected pattern: ");
        
        switch (patternType.toLowerCase()) {
            case "studentid":
                error.append("STU### (STU followed by exactly 3 digits)\n");
                error.append("Examples: STU001, STU042, STU999");
                break;
            case "email":
                error.append("standard email format\n");
                error.append("Examples: john.doe@example.com, user123@domain.co.uk");
                break;
            case "phone":
                error.append("Ghana phone format: 0XXXXXXXXX or +233XXXXXXXXX (9 digits after country code)\n");
                error.append("Examples: 0241234567, 0509876543, +233241234567, +233509876543");
                break;
            case "grade":
                error.append("number between 0 and 100\n");
                error.append("Examples: 0, 50, 85, 100");
                break;
            default:
                error.append("valid format for ").append(patternType);
        }
        
        return error.toString();
    }
    
    /**
     * Validates student ID and returns error message if invalid.
     * 
     * @param studentId The student ID to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateStudentId(String studentId) {
        if (!isValidStudentId(studentId)) {
            return getValidationErrorMessage("Student ID", studentId, "studentId");
        }
        return null;
    }
    
    /**
     * Validates email and returns error message if invalid.
     * 
     * @param email The email to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateEmail(String email) {
        if (!isValidEmail(email)) {
            return getValidationErrorMessage("Email", email, "email");
        }
        return null;
    }
    
    /**
     * Validates phone and returns error message if invalid.
     * 
     * @param phone The phone to validate
     * @return Error message if invalid, null if valid
     */
    public static String validatePhone(String phone) {
        if (!isValidPhone(phone)) {
            return getValidationErrorMessage("Phone", phone, "phone");
        }
        return null;
    }
    
    /**
     * Prompts user to retry after an error.
     * 
     * @param scanner The scanner to read user input
     * @return true if user wants to retry, false otherwise
     */
    private static boolean promptRetry(Scanner scanner) {
        System.out.print("Try again? (Y/N): ");
        String retry = scanner.nextLine().trim();
        return retry.equalsIgnoreCase("Y");
    }
    
    /**
     * Safely reads an integer input with validation and retry logic.
     * 
     * @param scanner The scanner to read input from
     * @param prompt The prompt message to display
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return The validated integer value
     * @throws java.util.InputMismatchException if user cancels input
     */
    public static int readIntInput(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Input cannot be empty. Please try again.");
                    continue;
                }
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
                if (!promptRetry(scanner)) {
                    throw new java.util.InputMismatchException("User cancelled input");
                }
            } catch (Exception e) {
                Logger.error("VALIDATION_UTILS: Error reading integer input - " + e.getMessage(), e);
                System.out.println("An error occurred: " + e.getMessage());
                if (!promptRetry(scanner)) {
                    throw new java.util.InputMismatchException("User cancelled input");
                }
            }
        }
    }
    
    /**
     * Safely reads a double input with validation and retry logic.
     * 
     * @param scanner The scanner to read input from
     * @param prompt The prompt message to display
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return The validated double value
     * @throws java.util.InputMismatchException if user cancels input
     */
    public static double readDoubleInput(Scanner scanner, String prompt, double min, double max) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Input cannot be empty. Please try again.");
                    continue;
                }
                double value = Double.parseDouble(input);
                if (value < min || value > max) {
                    System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                if (!promptRetry(scanner)) {
                    throw new java.util.InputMismatchException("User cancelled input");
                }
            } catch (Exception e) {
                Logger.error("VALIDATION_UTILS: Error reading double input - " + e.getMessage(), e);
                System.out.println("An error occurred: " + e.getMessage());
                if (!promptRetry(scanner)) {
                    throw new java.util.InputMismatchException("User cancelled input");
                }
            }
        }
    }
    
    /**
     * Safely reads a string input with validation.
     * 
     * @param scanner The scanner to read input from
     * @param prompt The prompt message to display
     * @param allowEmpty Whether empty input is allowed
     * @return The validated string, or null if user cancels
     */
    public static String readStringInput(Scanner scanner, String prompt, boolean allowEmpty) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (!allowEmpty && input.isEmpty()) {
                    System.out.println("Input cannot be empty. Please try again.");
                    continue;
                }
                return input;
            } catch (Exception e) {
                Logger.error("VALIDATION_UTILS: Error reading string input - " + e.getMessage(), e);
                System.out.println("An error occurred: " + e.getMessage());
                if (!promptRetry(scanner)) {
                    return null;
                }
            }
        }
    }
}

