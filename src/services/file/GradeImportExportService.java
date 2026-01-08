package services.file;

import models.*;
import exceptions.*;
import utilities.FileIOUtils;
import services.student.StudentService;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.text.SimpleDateFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
     * Exports a student's grade report in multiple formats (PDF, Text, Excel) simultaneously.
     */
    public void exportGradeReportMultiFormat(Student student, int reportType, String baseFilename) throws Exception {
        String[] formats = {"pdf", "text", "excel"};
        String[] extensions = {".pdf", ".txt", ".xlsx"};
        String[] subdirs = {"pdf", "text", "excel"};
    
        for (int i = 0; i < formats.length; i++) {
            String dirPath = baseFilename.substring(0, baseFilename.lastIndexOf('/')) + "/" + subdirs[i];
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();
    
            String filename = baseFilename.substring(baseFilename.lastIndexOf('/') + 1);
            String fullPath = dirPath + "/" + filename;
    
            switch (formats[i]) {
                case "pdf":
                    exportGradeReportPDF(student, fullPath);
                    break;
                case "text":
                    // Use option 3 for transcript format (detailed report)
                    exportGradeReport(student, 3, fullPath);
                    break;
                case "excel":
                    exportGradeReportExcel(student, fullPath);
                    break;
            }
        }
    }
    
    /**
     * Exports a student's grade report to a text file.
     */
    public String exportGradeReport(Student student, int option, String filename) throws IOException {
        // If filename doesn't contain path, use default directory
        String filePath;
        if (filename.contains("/") || filename.contains("\\")) {
            // Full path provided
            filePath = filename + ".txt";
            java.io.File parentDir = new java.io.File(filePath).getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } else {
            // Just filename, use default directory
            File reportsDir = new File("./reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdir();
            }
            filePath = "./reports/" + filename + ".txt";
        }
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
                ObjectMapper mapper = new ObjectMapper();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    List<Map<String, Object>> jsonArray = mapper.readValue(br, new TypeReference<List<Map<String, Object>>>() {});
                    totalRows = jsonArray.size();
                    for (Map<String, Object> obj : jsonArray) {
                        Map<String, String> record = new HashMap<>();
                        record.put("studentId", String.valueOf(obj.getOrDefault("studentId", "")).trim());
                        record.put("subjectName", String.valueOf(obj.getOrDefault("subjectName", "")).trim());
                        record.put("subjectType", String.valueOf(obj.getOrDefault("subjectType", "")).trim());
                        record.put("gradeStr", String.valueOf(obj.getOrDefault("gradeStr", "")).trim());
                        gradeRecords.add(record);
                    }
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
        ObjectMapper mapper = new ObjectMapper();
        java.io.File jsonDir = new java.io.File("./reports/json");
        if (!jsonDir.exists()) {
            jsonDir.mkdirs();
        }
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(java.nio.file.Paths.get("./reports/json/" + filename + ".json"))) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, formattedGrades);
        }
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
     */
    public void exportGradeReportPDF(Student student, String filename) throws Exception {
        // If filename doesn't contain path, use default directory
        String filePath;
        if (filename.contains("/") || filename.contains("\\")) {
            // Full path provided
            filePath = filename + ".pdf";
            java.io.File parentDir = new java.io.File(filePath).getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } else {
            // Just filename, use default directory
            java.io.File pdfDir = new java.io.File("./reports/pdf");
            if (!pdfDir.exists()) {
                pdfDir.mkdirs();
            }
            filePath = "./reports/pdf/" + filename + ".pdf";
        }
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();
        document.add(new Paragraph("Grade Report for " + student.getName()));
        document.add(new Paragraph("Student ID: " + student.getStudentID()));
        document.add(new Paragraph("Type: " + ((student instanceof HonorsStudent) ? "Honors Student" : "Regular Student")));
        document.add(new Paragraph(" "));
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
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Average: " + String.format("%.1f", student.calculateAverage(gradeService)) + "%"));
        document.add(new Paragraph("Status: " + (student.isPassing(gradeService) ? "PASSING" : "FAILING")));
        document.close();
    }
    
    /**
     * Exports grade report to Excel format.
     */
    public void exportGradeReportExcel(Student student, String filename) throws Exception {
        // If filename doesn't contain path, use default directory
        String filePath;
        if (filename.contains("/") || filename.contains("\\")) {
            // Full path provided
            filePath = filename + ".xlsx";
            java.io.File parentDir = new java.io.File(filePath).getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } else {
            // Just filename, use default directory
            java.io.File excelDir = new java.io.File("./reports/excel");
            if (!excelDir.exists()) {
                excelDir.mkdirs();
            }
            filePath = "./reports/excel/" + filename + ".xlsx";
        }
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Grades");
        
        // Header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Grade ID");
        header.createCell(1).setCellValue("Subject");
        header.createCell(2).setCellValue("Type");
        header.createCell(3).setCellValue("Value");
        header.createCell(4).setCellValue("Date");
        
        // Style header row
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        for (int i = 0; i < 5; i++) {
            header.getCell(i).setCellStyle(headerStyle);
        }
        
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
        
        // Add summary row
        int summaryRowIdx = rowIdx + 1;
        Row summaryRow = sheet.createRow(summaryRowIdx);
        summaryRow.createCell(0).setCellValue("Average:");
        summaryRow.createCell(1).setCellValue(String.format("%.1f", student.calculateAverage(gradeService)) + "%");
        summaryRow = sheet.createRow(summaryRowIdx + 1);
        summaryRow.createCell(0).setCellValue("Status:");
        summaryRow.createCell(1).setCellValue(student.isPassing(gradeService) ? "PASSING" : "FAILING");
        
        // Auto-size columns
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
        
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();
    }
}

