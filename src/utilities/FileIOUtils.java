package utilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.JsonNode;
import models.Grade;
import models.Student;
import models.RegularStudent;
import models.HonorsStudent;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class FileIOUtils {

    /**
     * Reads grades from a CSV file.
     * 
     * @param csvPath Path to the CSV file
     * @return List of Grade objects parsed from the CSV file
     * @throws IOException If file cannot be read (not found, permissions, etc.)
     */
    public static List<Grade> readGradesFromCSV(Path csvPath) throws IOException {
        List<Grade> grades = new ArrayList<>();
        
        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.iterator();
            
            if (it.hasNext()) it.next();
            
            while (it.hasNext()) {
                String line = it.next();
                String[] parts = line.split(",");
                
                if (parts.length == 6) {
                    grades.add(new Grade(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        Double.parseDouble(parts[4]),
                        new Date(Long.parseLong(parts[5]))
                    ));
                }
            }
        }
        return grades;
    }

    /**
     * Writes grades to a CSV file.
     * 
     * @param csvPath Path where CSV file will be written
     * @param grades List of Grade objects to write
     * @throws IOException If file cannot be written (permissions, disk full, etc.)
     */
    public static void writeGradesToCSV(Path csvPath, List<Grade> grades) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("gradeID,studentID,subjectName,subjectType,value,date\n");
            
            for (Grade g : grades) {
                writer.write(String.format("%s,%s,%s,%s,%.1f,%d\n",
                    g.getGradeID(),
                    g.getStudentID(),
                    g.getSubjectName(),
                    g.getSubjectType(),
                    g.getValue(),
                    g.getDate().getTime()));
            }
        }
    }

    // JSON Export/Import for Grades
    public static void writeGradesToJSON(Path jsonPath, List<Grade> grades) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedWriter writer = Files.newBufferedWriter(jsonPath)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, grades);
        }
    }

    public static List<Grade> readGradesFromJSON(Path jsonPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, Grade.class);
        try (BufferedReader reader = Files.newBufferedReader(jsonPath)) {
            return mapper.readValue(reader, collectionType);
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
     * Monitors a directory for file creation and modification events.
     * 
     * @param dir Directory path to monitor
     * @param onChange Callback function to execute when file events occur
     * @throws IOException If WatchService cannot be created or directory cannot be registered
     */
    public static void monitorDirectory(Path dir, Runnable onChange) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        
        new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = watchService.take();
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        // Event processed - trigger callback
                        onChange.run();
                    }
                    
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Reads students from a CSV file.
     * 
     * @param csvPath Path to the CSV file
     * @return List of Student objects parsed from the CSV file
     * @throws IOException If file cannot be read (not found, permissions, etc.)
     * @throws NumberFormatException If age field cannot be parsed as integer
     */
    public static List<Student> readStudentsFromCSV(Path csvPath) throws IOException {
        List<Student> students = new ArrayList<>();
        
        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.iterator();
            
            if (it.hasNext()) it.next();
            
            while (it.hasNext()) {
                String line = it.next();
                String[] parts = line.split(",");
                
                if (parts.length >= 6) {
                    // studentID parsed but not used directly - student ID is auto-generated
                    String name = parts[1].trim();
                    int age = Integer.parseInt(parts[2].trim());
                    String email = parts[3].trim();
                    String phone = parts[4].trim();
                    String type = parts[5].trim();
                    
                    Student s;
                    if ("Honors".equalsIgnoreCase(type) || "Honors Student".equalsIgnoreCase(type)) {
                        s = new HonorsStudent(name, age, email, phone);
                    } else {
                        s = new RegularStudent(name, age, email, phone);
                    }
                    
                    students.add(s);
                }
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
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, students);
        }
    }

    public static List<Student> readStudentsFromJSON(Path jsonPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(jsonPath)) {
            JsonNode rootNode = mapper.readTree(reader);
            List<Student> students = new ArrayList<>();
            
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    Student student = deserializeStudent(node, mapper);
                    if (student != null) {
                        students.add(student);
                    }
                }
            }
            return students;
        }
    }
    
    /**
     * Helper method to deserialize a Student from JSON, determining the correct type
     * (RegularStudent or HonorsStudent) based on the passingGrade field.
     */
    private static Student deserializeStudent(JsonNode node, ObjectMapper mapper) throws IOException {
        try {
            // Check passingGrade to determine student type
            // RegularStudent has passingGrade = 50, HonorsStudent has passingGrade = 60
            JsonNode passingGradeNode = node.get("passingGrade");
            int passingGrade = passingGradeNode != null ? passingGradeNode.asInt() : 50;
            
            if (passingGrade == 60) {
                return mapper.treeToValue(node, HonorsStudent.class);
            } else {
                return mapper.treeToValue(node, RegularStudent.class);
            }
        } catch (Exception e) {
            throw new IOException("Failed to deserialize student: " + e.getMessage(), e);
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