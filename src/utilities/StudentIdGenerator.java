package utilities;

/**
 * Thread-safe ID generator for creating unique student identifiers.
 */
public class StudentIdGenerator {
    private static int studentCounter = 0;

    /**
     * Generates the next unique student ID in sequence.
     * 
     * @return Next student ID in format "STU###" (e.g., "STU001", "STU042")
     */
    public static synchronized String nextId() {
        studentCounter++;
        return String.format("STU%03d", studentCounter);
    }
}