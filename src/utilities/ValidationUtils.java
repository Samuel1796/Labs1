package utilities;

import java.util.regex.Pattern;

/**
 * Utility class for comprehensive input validation using regex patterns.
 */
public class ValidationUtils {
    
    // Compiled regex patterns
    
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
    
    /** Name Pattern: Letters, spaces, hyphens, apostrophes (e.g., John O'Brien, Mary-Jane) */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]+(['-\\s][a-zA-Z]+)*$");
    
    /** Date Pattern: YYYY-MM-DD format (e.g., 2025-12-17) */
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    /** Course Code Pattern: 3 uppercase letters followed by 3 digits (e.g., MAT101, ENG203) */
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("^[A-Z]{3}\\d{3}$");
    
    /** Grade Pattern: 0-100 inclusive (supports 0, 1-9, 10-99, 100) */
    private static final Pattern GRADE_PATTERN = Pattern.compile("^(100|[1-9]?\\d)$");
    
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
     * Validates a name (allows letters, spaces, hyphens, apostrophes).
     * 
     * @param name The name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return NAME_PATTERN.matcher(name.trim()).matches();
    }
    
    /**
     * Validates a date in YYYY-MM-DD format.
     * 
     * @param date The date string to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidDate(String date) {
        if (date == null) return false;
        return DATE_PATTERN.matcher(date.trim()).matches();
    }
    
    /**
     * Validates a course code (3 uppercase letters + 3 digits).
     * 
     * @param courseCode The course code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCourseCode(String courseCode) {
        if (courseCode == null) return false;
        return COURSE_CODE_PATTERN.matcher(courseCode.trim()).matches();
    }
    
    /**
     * Validates a grade value (0-100).
     * 
     * @param grade The grade value as string
     * @return true if valid, false otherwise
     */
    public static boolean isValidGrade(String grade) {
        if (grade == null) return false;
        if (!GRADE_PATTERN.matcher(grade.trim()).matches()) return false;
        
        // Additional range check after pattern match
        try {
            int gradeValue = Integer.parseInt(grade.trim());
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
            case "name":
                error.append("letters, spaces, hyphens, and apostrophes only\n");
                error.append("Examples: John Smith, Mary-Jane Watson, O'Brien");
                break;
            case "date":
                error.append("YYYY-MM-DD format\n");
                error.append("Examples: 2025-12-17, 2024-01-01");
                break;
            case "coursecode":
                error.append("3 uppercase letters followed by 3 digits\n");
                error.append("Examples: MAT101, ENG203, CS450");
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
     * Validates name and returns error message if invalid.
     * 
     * @param name The name to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateName(String name) {
        if (!isValidName(name)) {
            return getValidationErrorMessage("Name", name, "name");
        }
        return null;
    }
}

