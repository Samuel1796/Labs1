package services;

import java.io.*;
import java.util.*;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import models.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.text.SimpleDateFormat;

import exceptions.*;
import utilities.FileIOUtils;
import java.nio.file.Paths;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Service class for managing grades, including recording, reporting, exporting, and importing grades.
 */
public class GradeService {
    private final Grade[] grades;
    private int gradeCount;

    /**
     * Constructs a GradeService with a specified maximum number of grades.
     * @param maxGrades Maximum number of grades that can be stored.
     */
    public GradeService(int maxGrades) {
        grades = new Grade[maxGrades];
        gradeCount = 0;
    }

    /**
     * Records a new grade in the system.
     * @param grade Grade object to record.
     * @return true if the grade was recorded successfully.
     * @throws AppExceptions if the grade database is full.
     */
    public boolean recordGrade(Grade grade, StudentService studentService) {
        if (gradeCount >= grades.length) {
            throw new AppExceptions("Grade database full!");
        }
        if (grade.getValue() < 0 || grade.getValue() > 100) {
            throw new InvalidGradeException(grade.getValue());
        }
        grades[gradeCount++] = grade;

        // Ensure the subject is enrolled for the student
        Student student = studentService.findStudentById(grade.getStudentID());
        Subject subject = studentService.findSubjectByNameAndType(grade.getSubjectName(), grade.getSubjectType());
        if (student != null) {
            if (subject == null) {
                // Create a new subject instance if not found
                if (grade.getSubjectType().equalsIgnoreCase("Core Subject")) {
                    subject = new CoreSubject(grade.getSubjectName(), grade.getSubjectType());
                } else if (grade.getSubjectType().equalsIgnoreCase("Elective Subject")) {
                    subject = new ElectiveSubject(grade.getSubjectName(), grade.getSubjectType());
                }
            }
            boolean alreadyEnrolled = student.getEnrolledSubjects().stream()
                .anyMatch(s -> s.getSubjectName().equalsIgnoreCase(grade.getSubjectName())
                            && s.getSubjectType().equalsIgnoreCase(grade.getSubjectType()));
            if (!alreadyEnrolled && subject != null) {
                student.enrollSubject(subject);
            }
        }
        return true;
    }

    /**
     * Optionally, keep the old method for backward compatibility
     * public boolean recordGrade(Grade grade) {
     *     throw new UnsupportedOperationException("Use recordGrade(Grade grade, StudentService studentService) instead.");
     * }
     */
    public boolean recordGrade(Grade grade) {
        throw new UnsupportedOperationException("Use recordGrade(Grade grade, StudentService studentService) instead.");
    }
/**
     * Returns the array of all grades.
     */
    public Grade[] getGrades() {
        return grades;
    }

    /**
     * Returns the current count of recorded grades.
     */
    public int getGradeCount() {
        return gradeCount;
    }

    /**
     * Sets the grade count.
     * @param count New grade count.
     */
    public void setGradeCount(int count) {
        this.gradeCount = count;
    }

    /**
     * Displays a detailed grade report for a given student.
     * @param student Student whose grades are to be reported.
     */
    public void viewGradeReport(Student student) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        System.out.printf("Student: %s - %s%n", student.getStudentID(), student.getName());
        System.out.printf("Type: %s%n", (student instanceof HonorsStudent) ? "Honors Student" : "Regular Student");

        // Collect grades for this student from the central grades array
        List<Grade> studentGrades = new ArrayList<>();
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                studentGrades.add(g);
            }
        }
if (studentGrades.isEmpty()) {
            System.out.printf("Passing Grade: %d%%%n", student.getPassingGrade());
            System.out.println("No grades recorded for this student.");
        } else {
            double total = 0.0;
            int count = 0;
            double coreTotal = 0;
            int coreCount = 0;
            double electiveTotal = 0;
            int electiveCount = 0;

            System.out.println("GRADE HISTORY");
            System.out.println("_________________________________________________________________________");
            System.out.println("| GRD ID   | DATE        | SUBJECT         | TYPE            | GRADE    |");
            System.out.println("|________________________________________________________________________|");

            // Print each grade and accumulate statistics
            for (Grade gr : studentGrades) {
                System.out.printf("| %-8s | %-10s | %-15s | %-15s | %-8.1f |%n",
                        gr.getGradeID(),
                        sdf.format(gr.getDate()),
                        gr.getSubjectName(),
                        gr.getSubjectType(),
                        gr.getValue());
                total += gr.getValue();
                count++;

                if ("Core Subject".equals(gr.getSubjectType())) {
                    coreTotal += gr.getValue();
                    coreCount++;
                } else {
                    electiveTotal += gr.getValue();
                    electiveCount++;
                }
            }
            System.out.println("|________________________________________________________________________|");

            double average = (count > 0) ? (total / count) : 0.0;
            System.out.printf("%nCurrent Average: %.1f%%%n", average);
            System.out.printf("Status: %s%n", (student.isPassing(this) ? "PASSING" : "FAILING"));

            System.out.println("\nTotal Grades: " + count);
            if (coreCount > 0) {
                System.out.printf("Core Subjects Average: %.1f%%%n", (coreTotal / coreCount));
            }
            if (electiveCount > 0) {
                System.out.printf("Elective Subjects Average: %.1f%%%n", (electiveTotal / electiveCount));
            }

            System.out.println("\nPerformance Summary:");
            if (student.isPassing(this)) {
                System.out.println("Passing all Core subjects");
                System.out.printf("Meeting passing grade requirement (%d%%)%n", student.getPassingGrade());
            }

            System.out.printf("%s - %s%n",
                    (student instanceof HonorsStudent) ? "Honors Student" : "Regular Student",
                    (student instanceof HonorsStudent ?
                            "higher standards (passing grade: 60%, eligible for honors recognition)" :
                            "standard grading (passing grade: 50%)"));
        }
    }

    /**
     * Counts the number of grades recorded for a specific student.
     * @param student Student whose grades are to be counted.
     * @return Number of grades for the student.
     */
    public int countGradesForStudent(Student student) {
        int count = 0;
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Exports a student's grade report in multiple formats (CSV, JSON, Binary) in parallel.
     * @param student Student whose report is to be exported.
     * @param reportType Type of report (1: Summary, 2: Detailed, 3: Transcript, 4: Analytics).
     * @param baseFilename Base name for output files (without extension).
     * @throws IOException If file writing fails.
     */
    public void exportGradeReportMultiFormat(Student student, int reportType, String baseFilename) throws IOException {
        // baseFilename should be something like "./reports/batch_2025-12-17/STU032"
        String[] formats = {"csv", "json", "binary"};
        String[] extensions = {".csv", ".json", ".dat"};
        String[] subdirs = {"csv", "json", "binary"};
    
        for (int i = 0; i < formats.length; i++) {
            String dirPath = baseFilename.substring(0, baseFilename.lastIndexOf('/')) + "/" + subdirs[i];
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();
    
            String filePath = dirPath + "/" + baseFilename.substring(baseFilename.lastIndexOf('/') + 1) + extensions[i];
    
            // Collect grades for the student
            List<Grade> studentGrades = new ArrayList<>();
            for (int j = 0; j < gradeCount; j++) {
                Grade g = grades[j];
                if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                    studentGrades.add(g);
                }
            }
    
            switch (formats[i]) {
                case "csv":
                    FileIOUtils.writeGradesToCSV(Paths.get(filePath), studentGrades);
                    break;
                case "json":
                    FileIOUtils.writeGradesToJSON(Paths.get(filePath), studentGrades);
                    break;
                case "binary":
                    FileIOUtils.writeGradesToBinary(Paths.get(filePath), studentGrades);
                    break;
            }
        }
    }

    /**
     * Exports a student's grade report to a text file.
     * @param student Student whose report is to be exported.
     * @param option 1 for summary, 2 for detailed, 3 for both.
     * @param filename Name of the output file (without extension).
     * @return Path to the exported file.
     * @throws IOException If file writing fails.
     */
    public String exportGradeReport(Student student, int option, String filename) throws IOException {
        // Ensure reports directory exists
        File reportsDir = new File("./reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdir();
        }
        String filePath = "./reports/" + filename + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write summary if requested
            if (option == 1 || option == 3) {
                writer.write("GRADE REPORT SUMMARY\n");
                writer.write("====================\n");
                writer.write("Student: " + student.getStudentID() + " - " + student.getName() + "\n");
                writer.write("Type: " + ((student instanceof models.HonorsStudent) ? "Honors Student" : "Regular Student") + "\n");
                writer.write("Total Grades: " + countGradesForStudent(student) + "\n");
                writer.write("Average: " + String.format("%.1f", student.calculateAverage(this)) + "%\n");
                writer.write("Status: " + (student.isPassing(this) ? "PASSING" : "FAILING") + "\n");
                writer.write("\n");
            }
            // Write detailed report if requested
            if (option == 2 || option == 3) {
                writer.write("GRADE HISTORY\n");
                writer.write("=============\n");
                writer.write(String.format("%-8s %-12s %-15s %-15s %-8s\n", "GRD ID", "DATE", "SUBJECT", "TYPE", "GRADE"));
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
                for (int i = 0; i < gradeCount; i++) {
                    Grade gr = grades[i];
                    if (gr != null && gr.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                        writer.write(String.format("%-8s %-12s %-15s %-15s %-8.1f\n",
                                gr.getGradeID(),
                                sdf.format(gr.getDate()),
                                gr.getSubjectName(),
                                gr.getSubjectType(),
                                gr.getValue()));
                    }
                }
                writer.write("\n");
            }
            // Write performance summary
            writer.write("Performance Summary:\n");
            if (student.isPassing(this)) {
                writer.write("Passing all Core subjects\n");
                writer.write("Meeting passing grade requirement (" + student.getPassingGrade() + "%)\n");
            } else {
                writer.write("Not meeting passing grade requirement (" + student.getPassingGrade() + "%)\n");
            }
            writer.write(((student instanceof models.HonorsStudent) ?
                    "Honors Student - higher standards (passing grade: 60%, eligible for honors recognition)\n" :
                    "Regular Student - standard grading (passing grade: 50%)\n"));
        }
        return filePath;
    }

    /**
     * Bulk imports grades from a CSV file.
     * @param filename Name of the CSV file (without extension).
     * @param studentService StudentService instance for student lookup.
     */
    public void bulkImportGrades(String filename, String format, StudentService studentService) {
        String dirPath = "./imports/";
        String filePath = dirPath + filename + "." + format.toLowerCase();
        File file = new File(filePath);
    
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return;
        }
    
        int totalRows = 0;
        int successCount = 0;
        int failCount = 0;
        List<String> failedRecords = new ArrayList<>();
    
        List<Map<String, String>> gradeRecords = new ArrayList<>();
    
        try {
            if (format.equalsIgnoreCase("csv")) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String header = br.readLine(); // skip header
                    String line;
                    int rowNum = 2;
                    while ((line = br.readLine()) != null) {
                        totalRows++;
                        String[] parts = line.split(",");
                        if (parts.length != 4) {
                            failCount++;
                            failedRecords.add("ROW " + rowNum + ": Invalid format");
                            rowNum++;
                            continue;
                        }
                        Map<String, String> record = new HashMap<>();
                        record.put("studentId", parts[0].trim());
                        record.put("subjectName", parts[1].trim());
                        record.put("subjectType", parts[2].trim());
                        record.put("gradeStr", parts[3].trim());
                        gradeRecords.add(record);
                        rowNum++;
                    }
                }
            } else if (format.equalsIgnoreCase("json")) {
                // Assuming JSON is an array of objects with keys: studentId, subjectName, subjectType, gradeStr
                StringBuilder jsonContent = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        jsonContent.append(line);
                    }
                }
                org.json.JSONArray arr = new org.json.JSONArray(jsonContent.toString());
                totalRows = arr.length();
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    Map<String, String> record = new HashMap<>();
                    record.put("studentId", obj.optString("studentId", "").trim());
                    record.put("subjectName", obj.optString("subjectName", "").trim());
                    record.put("subjectType", obj.optString("subjectType", "").trim());
                    record.put("gradeStr", obj.optString("gradeStr", "").trim());
                    gradeRecords.add(record);
                }
            } else {
                System.out.println("Unsupported format: " + format);
                return;
            }
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }
    
        int rowNum = 2;
        for (Map<String, String> record : gradeRecords) {
            String studentId = record.get("studentId");
            String subjectName = record.get("subjectName");
            String subjectType = record.get("subjectType");
            String gradeStr = record.get("gradeStr");
    
            Student student;
            try {
                student = studentService.findStudentById(studentId);
            } catch (StudentNotFoundException e) {
                failCount++;
                failedRecords.add("ROW " + rowNum + ": " + e.getMessage());
                rowNum++;
                continue;
            }
    
            Subject subject = studentService.findSubjectByNameAndType(subjectName, subjectType);
            if (subject == null) {
                // Create and enroll subject for the student
                if (subjectType.equalsIgnoreCase("Core Subject")) {
                    subject = new CoreSubject(subjectName, subjectType);
                } else if (subjectType.equalsIgnoreCase("Elective Subject")) {
                    subject = new ElectiveSubject(subjectName, subjectType);
                } else {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": Invalid subject type (" + subjectType + ")");
                    rowNum++;
                    continue;
                }
                student.enrollSubject(subject);
            }
    
            int gradeValue;
            try {
                gradeValue = Integer.parseInt(gradeStr);
                if (gradeValue < 0 || gradeValue > 100) {
                    throw new InvalidGradeException(gradeValue);
                }
            } catch (Exception e) {
                failCount++;
                failedRecords.add("ROW " + rowNum + ": " + e.getMessage());
                rowNum++;
                continue;
            }
    
            // Check for duplicate grade and prompt user for overwrite
            if (isDuplicateGrade(studentId, subjectName, subjectType)) {
                System.out.printf("ROW %d: Duplicate grade found for student %s and subject %s (%s). Overwrite with new value? [Y/N]: ", rowNum, studentId, subjectName, subjectType);
                Scanner scanner = new Scanner(System.in);
                String response = scanner.nextLine();
                if (response.equalsIgnoreCase("Y")) {
                    updateGrade(studentId, subjectName, subjectType, gradeValue);
                    successCount++;
                    System.out.println("Grade updated with new value.");
                } else {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": Duplicate grade not updated.");
                }
            } else {
                Grade grade = new Grade(
                        "GRD0" + (getGradeCount() + 1),
                        studentId,
                        subjectName,
                        subjectType,
                        gradeValue,
                        new java.util.Date()
                );
                recordGrade(grade, studentService);
                successCount++;
            }
            rowNum++;
        }
    
        // Print import summary
        System.out.println("IMPORT SUMMARY");
        System.out.println("Total Rows: " + totalRows);
        System.out.println("Successfully Imported: " + successCount);
        System.out.println("Failed: " + failCount);
        if (failCount > 0) {
            System.out.println("Failed Records:");
            for (String fail : failedRecords) {
                System.out.println(fail);
            }
        }
        System.out.println("Import completed!");
        System.out.println(successCount + " grades added to system");
    }

    /**
     * Checks for duplicate grades for a student and subject.
     * @param studentId Student ID.
     * @param subjectName Subject name.
     * @param subjectType Subject type.
     * @return true if a duplicate grade exists, false otherwise.
     */
    public boolean isDuplicateGrade(String studentId, String subjectName, String subjectType) {
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null &&
                    g.getStudentID().equalsIgnoreCase(studentId) &&
                    g.getSubjectName().equalsIgnoreCase(subjectName) &&
                    g.getSubjectType().equalsIgnoreCase(subjectType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the grade value for a duplicate grade entry.
     * @param studentId Student ID.
     * @param subjectName Subject name.
     * @param subjectType Subject type.
     * @param newValue New grade value to set.
     */
    public void updateGrade(String studentId, String subjectName, String subjectType, int newValue) {
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null &&
                    g.getStudentID().equalsIgnoreCase(studentId) &&
                    g.getSubjectName().equalsIgnoreCase(subjectName) &&
                    g.getSubjectType().equalsIgnoreCase(subjectType)) {
                g.setValue(newValue);
                g.setDate(new java.util.Date()); // Optionally update the date to now
                break;
            }
        }
    }

    public void exportGradesCSV(String filename) throws IOException {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy");
        List<Grade> gradeList = Arrays.asList(grades).subList(0, gradeCount);
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(java.nio.file.Paths.get("./reports/csv/" + filename + ".csv"))) {
            writer.write("gradeID,studentID,subjectName,subjectType,value,date\n");
            for (Grade g : gradeList) {
                writer.write(String.format("%s,%s,%s,%s,%.1f,%s\n",
                    g.getGradeID(), g.getStudentID(), g.getSubjectName(),
                    g.getSubjectType(), g.getValue(), sdf.format(g.getDate())));
            }
        }
    }

    public void exportGradesJSON(String filename) throws IOException {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy");
        List<Grade> gradeList = Arrays.asList(grades).subList(0, gradeCount);
        List<Map<String, Object>> formattedGrades = new ArrayList<>();
        for (Grade g : gradeList) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("gradeID", g.getGradeID());
            map.put("studentID", g.getStudentID());
            map.put("subjectName", g.getSubjectName());
            map.put("subjectType", g.getSubjectType());
            map.put("value", g.getValue());
            map.put("date", sdf.format(g.getDate()));
            formattedGrades.add(map);
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(java.nio.file.Paths.get("./reports/json/" + filename + ".json"))) {
            writer.write(mapper.writeValueAsString(formattedGrades));
        }
    }

    public void importGradesCSV(String filename) throws IOException {
        List<Grade> imported = FileIOUtils.readGradesFromCSV(Paths.get("./imports/" + filename + ".csv"));
        for (Grade g : imported) {
            recordGrade(g);
        }
    }


    public void importGradesJSON(String filename) throws IOException {
        List<Grade> imported = FileIOUtils.readGradesFromJSON(Paths.get("./imports/" + filename + ".json"));
        for (Grade g : imported) {
            recordGrade(g);
        }
    }

    public void exportGradesBinary(String filename) throws IOException {
        FileIOUtils.writeGradesToBinary(Paths.get("./reports/" + filename + ".bin"), Arrays.asList(grades).subList(0, gradeCount));
    }

    public void importGradesBinary(String filename) throws IOException, ClassNotFoundException {
        List<Grade> imported = FileIOUtils.readGradesFromBinary(Paths.get("./imports/" + filename + ".bin"));
        for (Grade g : imported) {
            recordGrade(g);
        }
    }


    public void exportGradeReportPDF(Student student, String filename) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filename + ".pdf"));
        document.open();
        document.add(new Paragraph("Grade Report for " + student.getName()));
        PdfPTable table = new PdfPTable(5);
        table.addCell("Grade ID");
        table.addCell("Subject");
        table.addCell("Type");
        table.addCell("Value");
        table.addCell("Date");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy");
        for (Grade g : getGrades()) {
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                table.addCell(g.getGradeID());
                table.addCell(g.getSubjectName());
                table.addCell(g.getSubjectType());
                table.addCell(String.valueOf(g.getValue()));
                table.addCell(sdf.format(g.getDate()));
            }
        }
        document.add(table);
        document.close();
    }



    public void exportGradeReportExcel(Student student, String filename) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Grades");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Grade ID");
        header.createCell(1).setCellValue("Subject");
        header.createCell(2).setCellValue("Type");
        header.createCell(3).setCellValue("Value");
        header.createCell(4).setCellValue("Date");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy");
        int rowIdx = 1;
        for (Grade g : getGrades()) {
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(g.getGradeID());
                row.createCell(1).setCellValue(g.getSubjectName());
                row.createCell(2).setCellValue(g.getSubjectType());
                row.createCell(3).setCellValue(g.getValue());
                row.createCell(4).setCellValue(sdf.format(g.getDate()));
            }
        }
        try (FileOutputStream fos = new FileOutputStream(filename + ".xlsx")) {
            workbook.write(fos);
        }
        workbook.close();
    }



}