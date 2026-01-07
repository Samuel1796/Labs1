package services.file;

import models.*;
import exceptions.*;
import utilities.FileIOUtils;
import services.student.StudentService;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.text.SimpleDateFormat;
// iText PDF dependencies - uncomment when iText library is added to classpath
// import com.itextpdf.text.Document;
// import com.itextpdf.text.Paragraph;
// import com.itextpdf.text.pdf.PdfPTable;
// import com.itextpdf.text.pdf.PdfWriter;
// Apache POI Excel dependencies - uncomment when POI library is added to classpath
// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Service for importing and exporting grade data in multiple formats.
 * 
 * Handles all file I/O operations for grades:
 * - CSV import/export
 * - JSON import/export
 * - Binary import/export
 * - PDF export
 * - Excel export
 * - Multi-format batch export
 * - Bulk import with validation
 */
public class GradeImportExportService {
    
    private final GradeService gradeService;
    
    public GradeImportExportService(GradeService gradeService) {
        this.gradeService = gradeService;
    }
    
    /**
     * Gets the GradeService instance (for access in BatchReportTaskManager).
     */
    public GradeService getGradeService() {
        return gradeService;
    }
    
    /**
     * Gets all grades for a specific student.
     */
    public List<Grade> getStudentGrades(Student student) {
        List<Grade> studentGrades = new ArrayList<>();
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
        for (int j = 0; j < gradeCount; j++) {
            Grade g = grades[j];
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                studentGrades.add(g);
            }
        }
        return studentGrades;
    }
    
    /**
     * Exports a student's grade report in multiple formats (CSV, JSON, Binary) simultaneously.
     */
    public void exportGradeReportMultiFormat(Student student, int reportType, String baseFilename) throws IOException {
        String[] formats = {"csv", "json", "binary"};
        String[] extensions = {".csv", ".json", ".dat"};
        String[] subdirs = {"csv", "json", "binary"};
    
        for (int i = 0; i < formats.length; i++) {
            String dirPath = baseFilename.substring(0, baseFilename.lastIndexOf('/')) + "/" + subdirs[i];
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();
    
            String filePath = dirPath + "/" + baseFilename.substring(baseFilename.lastIndexOf('/') + 1) + extensions[i];
    
            List<Grade> studentGrades = new ArrayList<>();
            Grade[] grades = gradeService.getGrades();
            int gradeCount = gradeService.getGradeCount();
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
     */
    public String exportGradeReport(Student student, int option, String filename) throws IOException {
        File reportsDir = new File("./reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdir();
        }
        String filePath = "./reports/" + filename + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            if (option == 1 || option == 3) {
                writer.write("GRADE REPORT SUMMARY\n");
                writer.write("====================\n");
                writer.write("Student: " + student.getStudentID() + " - " + student.getName() + "\n");
                writer.write("Type: " + ((student instanceof HonorsStudent) ? "Honors Student" : "Regular Student") + "\n");
                writer.write("Total Grades: " + gradeService.countGradesForStudent(student) + "\n");
                writer.write("Average: " + String.format("%.1f", student.calculateAverage(gradeService)) + "%\n");
                writer.write("Status: " + (student.isPassing(gradeService) ? "PASSING" : "FAILING") + "\n");
                writer.write("\n");
            }
            if (option == 2 || option == 3) {
                writer.write("GRADE HISTORY\n");
                writer.write("=============\n");
                writer.write(String.format("%-8s %-12s %-15s %-15s %-8s\n", "GRD ID", "DATE", "SUBJECT", "TYPE", "GRADE"));
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Grade[] grades = gradeService.getGrades();
                int gradeCount = gradeService.getGradeCount();
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
            writer.write("Performance Summary:\n");
            if (student.isPassing(gradeService)) {
                writer.write("Passing all Core subjects\n");
                writer.write("Meeting passing grade requirement (" + student.getPassingGrade() + "%)\n");
            } else {
                writer.write("Not meeting passing grade requirement (" + student.getPassingGrade() + "%)\n");
            }
            writer.write(((student instanceof HonorsStudent) ?
                    "Honors Student - higher standards (passing grade: 60%, eligible for honors recognition)\n" :
                    "Regular Student - standard grading (passing grade: 50%)\n"));
        }
        return filePath;
    }
    
    /**
     * Bulk imports grades from CSV or JSON files.
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
                    String header = br.readLine();
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
                StringBuilder jsonContent = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        jsonContent.append(line);
                    }
                }
                // Note: JSON parsing requires org.json library
                // For now, using simple string parsing as fallback
                throw new UnsupportedOperationException("JSON import requires org.json library. Please add org.json:json to classpath.");
                /* Uncomment when org.json is available:
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
                */
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
    
            if (gradeService.isDuplicateGrade(studentId, subjectName, subjectType)) {
                System.out.printf("ROW %d: Duplicate grade found for student %s and subject %s (%s). Overwrite with new value? [Y/N]: ", rowNum, studentId, subjectName, subjectType);
                Scanner scanner = new Scanner(System.in);
                String response = scanner.nextLine();
                if (response.equalsIgnoreCase("Y")) {
                    gradeService.updateGrade(studentId, subjectName, subjectType, gradeValue);
                    successCount++;
                    System.out.println("Grade updated with new value.");
                } else {
                    failCount++;
                    failedRecords.add("ROW " + rowNum + ": Duplicate grade not updated.");
                }
            } else {
                Grade grade = new Grade(
                        "GRD0" + (gradeService.getGradeCount() + 1),
                        studentId,
                        subjectName,
                        subjectType,
                        gradeValue,
                        new java.util.Date()
                );
                gradeService.recordGrade(grade, studentService);
                successCount++;
            }
            rowNum++;
        }
    
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
     * Exports all grades to CSV format.
     */
    public void exportGradesCSV(String filename) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
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
    
    /**
     * Exports all grades to JSON format.
     */
    public void exportGradesJSON(String filename) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
        List<Grade> gradeList = Arrays.asList(grades).subList(0, gradeCount);
        List<Map<String, Object>> formattedGrades = new ArrayList<>();
        for (Grade g : gradeList) {
            Map<String, Object> map = new HashMap<>();
            map.put("gradeID", g.getGradeID());
            map.put("studentID", g.getStudentID());
            map.put("subjectName", g.getSubjectName());
            map.put("subjectType", g.getSubjectType());
            map.put("value", g.getValue());
            map.put("date", sdf.format(g.getDate()));
            formattedGrades.add(map);
        }
        // Note: JSON export requires Jackson library
        throw new UnsupportedOperationException("JSON export requires Jackson library. Please add com.fasterxml.jackson.core:jackson-databind to classpath.");
        /* Uncomment when Jackson is available:
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(java.nio.file.Paths.get("./reports/json/" + filename + ".json"))) {
            writer.write(mapper.writeValueAsString(formattedGrades));
        }
        */
    }
    
    /**
     * Imports grades from CSV file.
     */
    public void importGradesCSV(String filename) throws IOException {
        List<Grade> imported = FileIOUtils.readGradesFromCSV(Paths.get("./imports/" + filename + ".csv"));
        for (Grade g : imported) {
            gradeService.recordGrade(g, null); // Note: StudentService needed for full functionality
        }
    }
    
    /**
     * Imports grades from JSON file.
     */
    public void importGradesJSON(String filename) throws IOException {
        List<Grade> imported = FileIOUtils.readGradesFromJSON(Paths.get("./imports/" + filename + ".json"));
        for (Grade g : imported) {
            gradeService.recordGrade(g, null); // Note: StudentService needed for full functionality
        }
    }
    
    /**
     * Exports all grades to binary format.
     */
    public void exportGradesBinary(String filename) throws IOException {
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
        FileIOUtils.writeGradesToBinary(Paths.get("./reports/" + filename + ".bin"), Arrays.asList(grades).subList(0, gradeCount));
    }
    
    /**
     * Imports grades from binary file.
     */
    public void importGradesBinary(String filename) throws IOException, ClassNotFoundException {
        List<Grade> imported = FileIOUtils.readGradesFromBinary(Paths.get("./imports/" + filename + ".bin"));
        for (Grade g : imported) {
            gradeService.recordGrade(g, null); // Note: StudentService needed for full functionality
        }
    }
    
    /**
     * Exports grade report to PDF format.
     * Note: Requires iText library (com.itextpdf:itextpdf)
     */
    public void exportGradeReportPDF(Student student, String filename) throws Exception {
        throw new UnsupportedOperationException("PDF export requires iText library. Please add com.itextpdf:itextpdf to classpath.");
        /* Uncomment when iText is available:
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
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
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
        */
    }
    
    /**
     * Exports grade report to Excel format.
     * Note: Requires Apache POI library (org.apache.poi:poi-ooxml)
     */
    public void exportGradeReportExcel(Student student, String filename) throws Exception {
        throw new UnsupportedOperationException("Excel export requires Apache POI library. Please add org.apache.poi:poi-ooxml to classpath.");
        /* Uncomment when Apache POI is available:
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Grades");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Grade ID");
        header.createCell(1).setCellValue("Subject");
        header.createCell(2).setCellValue("Type");
        header.createCell(3).setCellValue("Value");
        header.createCell(4).setCellValue("Date");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
        int rowIdx = 1;
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
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
        */
    }
}

