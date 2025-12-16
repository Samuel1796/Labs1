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

    // CSV Streaming Read for Grades
    public static List<Grade> readGradesFromCSV(Path csvPath) throws IOException {
        List<Grade> grades = new ArrayList<>();
        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.iterator();
            if (it.hasNext()) it.next(); // skip header
            while (it.hasNext()) {
                String[] parts = it.next().split(",");
                if (parts.length == 6) {
                    grades.add(new Grade(
                        parts[0], parts[1], parts[2], parts[3],
                        Double.parseDouble(parts[4]), new Date(Long.parseLong(parts[5]))
                    ));
                }
            }
        }
        return grades;
    }

    // CSV Streaming Write for Grades
    public static void writeGradesToCSV(Path csvPath, List<Grade> grades) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("gradeID,studentID,subjectName,subjectType,value,date\n");
            for (Grade g : grades) {
                writer.write(String.format("%s,%s,%s,%s,%.1f,%d\n",
                    g.getGradeID(), g.getStudentID(), g.getSubjectName(),
                    g.getSubjectType(), g.getValue(), g.getDate().getTime()));
            }
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

    // WatchService for directory monitoring (bonus)
    public static void monitorDirectory(Path dir, Runnable onChange) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        onChange.run();
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // CSV Streaming Read for Students
    public static List<Student> readStudentsFromCSV(Path csvPath) throws IOException {
        List<Student> students = new ArrayList<>();
        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.iterator();
            if (it.hasNext()) it.next(); // skip header
            while (it.hasNext()) {
                String[] parts = it.next().split(",");
                // Example CSV: studentID,name,age,email,phone,type
                if (parts.length >= 6) {
                    String studentID = parts[0].trim();
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
                    // If your Student constructor does not set ID, you may need to set it via reflection or a setter
                    // s.setStudentID(studentID); // If available
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