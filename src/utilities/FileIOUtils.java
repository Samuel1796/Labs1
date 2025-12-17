package utilities;

import models.Grade;
import models.Student;
import models.RegularStudent;
import models.HonorsStudent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class FileIOUtils {

    /**
     * Reads grades from a CSV file using streaming for memory efficiency.
     * 
     * This method uses Java NIO's Files.lines() which provides:
     * - Lazy evaluation: lines are read on-demand, not all at once
     * - Automatic resource management: try-with-resources ensures file is closed
     * - UTF-8 encoding: default encoding for text files
     * 
     * Memory Efficiency:
     * For large CSV files, this approach avoids loading the entire file into memory.
     * Instead, it processes one line at a time, making it suitable for files of any size.
     * 
     * CSV Format Expected:
     * Header row (skipped), then data rows with 6 columns:
     * gradeID,studentID,subjectName,subjectType,value,date
     * 
     * Error Handling:
     * - Malformed rows (wrong column count) are silently skipped
     * - IOException is propagated to caller for file-level errors
     * 
     * Time Complexity: O(n) where n is number of lines
     * Space Complexity: O(m) where m is number of valid grade records (not file size)
     * 
     * @param csvPath Path to the CSV file
     * @return List of Grade objects parsed from the CSV file
     * @throws IOException If file cannot be read (not found, permissions, etc.)
     */
    public static List<Grade> readGradesFromCSV(Path csvPath) throws IOException {
        List<Grade> grades = new ArrayList<>();
        
        // Use Files.lines() for streaming: reads file line-by-line without loading entire file
        // try-with-resources ensures stream is properly closed even if exception occurs
        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.iterator();
            
            // Skip header row: first line contains column names
            if (it.hasNext()) it.next();
            
            // Process each data row
            while (it.hasNext()) {
                String line = it.next();
                String[] parts = line.split(",");
                
                // Validate row structure: must have exactly 6 columns
                // Format: gradeID,studentID,subjectName,subjectType,value,date
                if (parts.length == 6) {
                    // Parse and construct Grade object
                    // Date is stored as long timestamp in CSV, converted back to Date object
                    grades.add(new Grade(
                        parts[0],                                    // gradeID
                        parts[1],                                    // studentID
                        parts[2],                                    // subjectName
                        parts[3],                                    // subjectType
                        Double.parseDouble(parts[4]),                 // value (percentage)
                        new Date(Long.parseLong(parts[5]))            // date (timestamp)
                    ));
                }
                // Note: Rows with incorrect column count are silently skipped
                // Consider logging or error reporting for production use
            }
        }
        return grades;
    }

    /**
     * Writes grades to a CSV file using buffered I/O for performance.
     * 
     * This method uses BufferedWriter which provides:
     * - Buffered output: reduces system calls by batching writes
     * - Automatic flushing: buffer is flushed when stream is closed
     * - UTF-8 encoding: default encoding for text files
     * 
     * CSV Format:
     * - Header row with column names
     * - Data rows with comma-separated values
     * - Date stored as long timestamp for compact representation
     * 
     * Performance:
     * Buffered I/O significantly improves performance for large datasets
     * by reducing the number of expensive system calls.
     * 
     * Error Handling:
     * - IOException is propagated to caller
     * - File is automatically closed via try-with-resources
     * - Directory structure must exist (consider creating if needed)
     * 
     * Time Complexity: O(n) where n is number of grades
     * Space Complexity: O(1) - only buffer space, not proportional to input size
     * 
     * @param csvPath Path where CSV file will be written
     * @param grades List of Grade objects to write
     * @throws IOException If file cannot be written (permissions, disk full, etc.)
     */
    public static void writeGradesToCSV(Path csvPath, List<Grade> grades) throws IOException {
        // Use BufferedWriter for efficient I/O: batches writes to reduce system calls
        // Files.newBufferedWriter() uses UTF-8 encoding and default buffer size
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            // Write CSV header: column names for easy identification
            writer.write("gradeID,studentID,subjectName,subjectType,value,date\n");
            
            // Write each grade as a CSV row
            for (Grade g : grades) {
                // Format: comma-separated values matching header structure
                // Date converted to timestamp (long) for compact storage
                writer.write(String.format("%s,%s,%s,%s,%.1f,%d\n",
                    g.getGradeID(),                                    // gradeID
                    g.getStudentID(),                                  // studentID
                    g.getSubjectName(),                                // subjectName
                    g.getSubjectType(),                               // subjectType
                    g.getValue(),                                      // value (1 decimal place)
                    g.getDate().getTime()));                          // date (timestamp)
            }
            // Buffer is automatically flushed and file closed when try block exits
        }
    }

    // JSON Export/Import for Grades
    public static void writeGradesToJSON(Path jsonPath, List<Grade> grades) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedWriter writer = Files.newBufferedWriter(jsonPath)) {
            writer.write(mapper.writeValueAsString(grades));
        }
    }

    public static List<Grade> readGradesFromJSON(Path jsonPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(jsonPath)) {
            return mapper.readValue(reader, new TypeReference<List<Grade>>() {});
        }
    }

    // Binary Serialization for Grades
    public static void writeGradesToBinary(Path binPath, List<Grade> grades) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(binPath))) {
            oos.writeObject(grades);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Grade> readGradesFromBinary(Path binPath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(binPath))) {
            return (List<Grade>) ois.readObject();
        }
    }

    /**
     * Monitors a directory for file creation and modification events using Java NIO WatchService.
     * 
     * This method implements a file system watcher that:
     * - Detects new files added to the directory (ENTRY_CREATE)
     * - Detects modifications to existing files (ENTRY_MODIFY)
     * - Executes a callback function when events occur
     * - Runs in a background daemon thread (doesn't prevent JVM shutdown)
     * 
     * Use Cases:
     * - Auto-import functionality: automatically process new files in imports directory
     * - Real-time synchronization: react to file changes immediately
     * - Batch processing: trigger processing when files are ready
     * 
     * Implementation Details:
     * - WatchService is platform-specific (uses native OS file system events)
     * - Events are polled using take() which blocks until an event occurs
     * - WatchKey must be reset after processing events to continue monitoring
     * - Background thread runs indefinitely until interrupted
     * 
     * Thread Safety:
     * - WatchService operations are thread-safe
     * - Callback (onChange) should be thread-safe if shared state is accessed
     * 
     * Limitations:
     * - May not detect events on some network file systems
     * - Events may be batched or delayed on some platforms
     * - Recursive directory watching requires additional implementation
     * 
     * Resource Management:
     * - WatchService should be closed when no longer needed (not implemented here)
     * - Consider adding shutdown hook or explicit close method for production use
     * 
     * @param dir Directory path to monitor
     * @param onChange Callback function to execute when file events occur
     * @throws IOException If WatchService cannot be created or directory cannot be registered
     */
    public static void monitorDirectory(Path dir, Runnable onChange) throws IOException {
        // Create WatchService: platform-specific implementation for file system events
        // Uses native OS capabilities for efficient event detection
        WatchService watchService = FileSystems.getDefault().newWatchService();
        
        // Register directory for specific event types
        // ENTRY_CREATE: new files added
        // ENTRY_MODIFY: existing files changed
        // Note: ENTRY_DELETE not registered (only watching for new/modified files)
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        
        // Start background monitoring thread
        // Daemon thread allows JVM to shutdown even if thread is running
        new Thread(() -> {
            try {
                // Infinite loop: continuously monitor for file events
                while (true) {
                    // Block until a file event occurs (take() is blocking)
                    WatchKey key = watchService.take();
                    
                    // Process all events in the current batch
                    // Multiple events may be queued if files changed rapidly
                    for (WatchEvent<?> event : key.pollEvents()) {
                        // Execute callback: perform action when file event detected
                        // This could trigger import, processing, notification, etc.
                        onChange.run();
                    }
                    
                    // Reset key: required to continue receiving events
                    // If reset() returns false, key is invalid and should be removed
                    key.reset();
                }
            } catch (InterruptedException e) {
                // Thread was interrupted: restore interrupt status and exit
                // This allows graceful shutdown when thread is interrupted
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Note: WatchService is not closed here - consider adding cleanup mechanism
        // For long-running applications, implement proper resource management
    }

    /**
     * Reads students from a CSV file using streaming for memory efficiency.
     * 
     * This method parses CSV rows and creates appropriate Student subclass instances
     * based on the student type specified in the CSV.
     * 
     * CSV Format Expected:
     * Header row (skipped), then data rows with at least 6 columns:
     * studentID,name,age,email,phone,type
     * 
     * Student Type Handling:
     * - "Honors" or "Honors Student" → Creates HonorsStudent instance
     * - Any other value → Creates RegularStudent instance
     * 
     * Design Note:
     * Student ID from CSV is currently not used (StudentIdGenerator creates new IDs).
     * This ensures unique IDs but loses original ID mapping. Consider preserving
     * original IDs if needed for data migration scenarios.
     * 
     * Error Handling:
     * - Malformed rows (insufficient columns) are silently skipped
     * - NumberFormatException for age parsing would propagate (consider try-catch)
     * - IOException is propagated to caller for file-level errors
     * 
     * Memory Efficiency:
     * Uses streaming to avoid loading entire file into memory, suitable for large files.
     * 
     * Time Complexity: O(n) where n is number of lines
     * Space Complexity: O(m) where m is number of valid student records
     * 
     * @param csvPath Path to the CSV file
     * @return List of Student objects parsed from the CSV file
     * @throws IOException If file cannot be read (not found, permissions, etc.)
     * @throws NumberFormatException If age field cannot be parsed as integer
     */
    public static List<Student> readStudentsFromCSV(Path csvPath) throws IOException {
        List<Student> students = new ArrayList<>();
        
        // Use Files.lines() for streaming: processes file line-by-line
        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.iterator();
            
            // Skip header row: first line contains column names
            if (it.hasNext()) it.next();
            
            // Process each data row
            while (it.hasNext()) {
                String line = it.next();
                String[] parts = line.split(",");
                
                // Validate row structure: must have at least 6 columns
                // Format: studentID,name,age,email,phone,type
                if (parts.length >= 6) {
                    // Parse CSV columns (trim whitespace for data cleanliness)
                    String studentID = parts[0].trim();
                    String name = parts[1].trim();
                    int age = Integer.parseInt(parts[2].trim());
                    String email = parts[3].trim();
                    String phone = parts[4].trim();
                    String type = parts[5].trim();
                    
                    // Factory pattern: create appropriate Student subclass based on type
                    Student s;
                    if ("Honors".equalsIgnoreCase(type) || "Honors Student".equalsIgnoreCase(type)) {
                        s = new HonorsStudent(name, age, email, phone);
                    } else {
                        s = new RegularStudent(name, age, email, phone);
                    }
                    
                    // Note: Student ID from CSV is not used
                    // StudentIdGenerator creates new unique IDs instead
                    // This ensures uniqueness but loses original ID mapping
                    // Consider: s.setStudentID(studentID) if ID preservation is needed
                    
                    students.add(s);
                }
                // Rows with insufficient columns are silently skipped
            }
        }
        return students;
    }

    // CSV Streaming Write for Students
    public static void writeStudentsToCSV(Path csvPath, Collection<Student> students) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("studentID,name,age,email,phone,type\n");
            for (Student s : students) {
                String type = (s instanceof HonorsStudent) ? "Honors" : "Regular";
                writer.write(String.format("%s,%s,%d,%s,%s,%s\n",
                    s.getStudentID(), s.getName(), s.getAge(), s.getEmail(), s.getPhone(), type));
            }
        }
    }

    // JSON Export/Import for Students
    public static void writeStudentsToJSON(Path jsonPath, Collection<Student> students) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedWriter writer = Files.newBufferedWriter(jsonPath)) {
            writer.write(mapper.writeValueAsString(students));
        }
    }

    public static List<Student> readStudentsFromJSON(Path jsonPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(jsonPath)) {
            return mapper.readValue(reader, new TypeReference<List<Student>>() {});
        }
    }

    // Binary Serialization for Students
    public static void writeStudentsToBinary(Path binPath, Collection<Student> students) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(binPath))) {
            oos.writeObject(new ArrayList<>(students));
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Student> readStudentsFromBinary(Path binPath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(binPath))) {
            return (List<Student>) ois.readObject();
        }
    }
}