package utilities;

/**
 * Thread-safe ID generator for creating unique student identifiers.
 * 
 * This utility class implements a singleton counter pattern that generates
 * sequential student IDs in the format "STU###" where ### is a zero-padded
 * 3-digit number (e.g., STU001, STU002, STU010, STU100).
 * 
 * Thread Safety:
 * - Uses synchronized method to ensure atomic counter increment
 * - Prevents race conditions in multi-threaded environments
 * - Guarantees unique IDs even with concurrent student creation
 * 
 * ID Format:
 * - Prefix: "STU" (Student identifier)
 * - Number: Sequential counter starting at 1
 * - Padding: Zero-padded to 3 digits (001, 002, ..., 999)
 * - Maximum: 999 students before counter wraps (consider long for larger systems)
 * 
 * Design Considerations:
 * - Static counter: persists across all instances
 * - Synchronized: thread-safe but may create contention under high load
 * - Format: fixed format ensures consistent ID structure
 * 
 * Alternative Approaches:
 * - UUID: for globally unique IDs (longer, not sequential)
 * - Database sequence: for persistent, distributed systems
 * - AtomicInteger: for lock-free counter (better performance)
 * 
 * Usage:
 * Called during Student object construction to assign unique identifier.
 */
public class StudentIdGenerator {
    // Static counter: shared across all instances
    // Starts at 0, first student gets STU001
    private static int studentCounter = 0;

    /**
     * Generates the next unique student ID in sequence.
     * 
     * This method is thread-safe using synchronized keyword:
     * - Only one thread can execute this method at a time
     * - Prevents race conditions where two threads get the same ID
     * - Ensures sequential ID generation
     * 
     * ID Generation Process:
     * 1. Increment counter atomically (thread-safe)
     * 2. Format as "STU" + zero-padded 3-digit number
     * 3. Return formatted string
     * 
     * Performance:
     * - Synchronized method: slight performance overhead for thread safety
     * - For high-throughput scenarios, consider AtomicInteger for lock-free operation
     * 
     * Limitations:
     * - Counter wraps at 1000 (STU1000 would be invalid with current format)
     * - Not persistent: counter resets on application restart
     * - Single JVM: doesn't work across distributed systems
     * 
     * @return Next student ID in format "STU###" (e.g., "STU001", "STU042")
     */
    public static synchronized String nextId() {
        // Atomic increment: thread-safe counter update
        // Synchronized ensures only one thread can increment at a time
        studentCounter++;
        
        // Format: "STU" prefix + zero-padded 3-digit number
        // %03d: pad with zeros to 3 digits (1 -> 001, 42 -> 042)
        return String.format("STU%03d", studentCounter);
    }
}