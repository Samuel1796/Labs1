package services;

import models.Grade;
import models.Student;
import models.HonorsStudent;
import models.RegularStudent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides statistical analysis and reporting for grades and students.
 */
public class StatisticsService {

    private final Grade[] grades;
    private final int gradeCount;
    private final Student[] students;
    private final int studentCount;
    private final GradeService gradeService;


    public StatisticsService(Grade[] grades, int gradeCount, Student[] students, int studentCount, GradeService gradeService) {
        this.grades = grades;
        this.gradeCount = gradeCount;
        this.students = students;
        this.studentCount = studentCount;
        this.gradeService = gradeService;
    }

    // Helper: Get all grade values
    private List<Double> getAllGradeValues() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < gradeCount; i++) {
            if (grades[i] != null) {
                values.add(grades[i].getValue());
            }
        }
        return values;
    }

    // Mean (Average)
    public double calculateMean() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    // Median
    public double calculateMedian() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        Collections.sort(values);
        int n = values.size();
        if (n % 2 == 1) {
            return values.get(n / 2);
        } else {
            return (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0;
        }
    }

    // Mode
    public double calculateMode() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        Map<Double, Integer> freq = new HashMap<>();
        for (double v : values) freq.put(v, freq.getOrDefault(v, 0) + 1);
        int maxFreq = 0;
        double mode = values.get(0);
        for (Map.Entry<Double, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                mode = entry.getKey();
            }
        }
        return mode;
    }

    // Standard Deviation
    public double calculateStdDev() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        double mean = calculateMean();
        double sumSq = 0.0;
        for (double v : values) sumSq += Math.pow(v - mean, 2);
        return Math.sqrt(sumSq / values.size());
    }

    // Range
    public double calculateRange() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        double min = Collections.min(values);
        double max = Collections.max(values);
        return max - min;
    }

    // Highest Grade
    public double getHighestGrade() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        return Collections.max(values);
    }

    // Lowest Grade
    public double getLowestGrade() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        return Collections.min(values);
    }

    // Subject Performance
    public Map<String, Double> getSubjectAverages() {
        Map<String, List<Double>> subjectGrades = new HashMap<>();
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null) {
                subjectGrades.computeIfAbsent(g.getSubjectName(), k -> new ArrayList<>()).add(g.getValue());
            }
        }
        Map<String, Double> averages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : subjectGrades.entrySet()) {
            List<Double> vals = entry.getValue();
            double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            averages.put(entry.getKey(), avg);
        }
        return averages;
    }

    // Grade Distribution (by ranges)
    public Map<String, Integer> getGradeDistribution() {
        int[] bins = new int[5]; // 0-59, 60-69, 70-79, 80-89, 90-100
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null) {
                double v = g.getValue();
                if (v < 60) bins[0]++;
                else if (v < 70) bins[1]++;
                else if (v < 80) bins[2]++;
                else if (v < 90) bins[3]++;
                else bins[4]++;
            }
        }
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("0-59%", bins[0]);
        dist.put("60-69%", bins[1]);
        dist.put("70-79%", bins[2]);
        dist.put("80-89%", bins[3]);
        dist.put("90-100%", bins[4]);
        return dist;
    }

    // Student Type Comparison
    public Map<String, Double> getStudentTypeAverages() {
        double regularSum = 0.0, honorsSum = 0.0;
        int regularCount = 0, honorsCount = 0;
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            if (s instanceof RegularStudent) {
                double avg = s.calculateAverage(gradeService);
                regularSum += avg;
                regularCount++;
            } else if (s instanceof HonorsStudent) {
                double avg = s.calculateAverage(gradeService);
                honorsSum += avg;
                honorsCount++;
            }
        }
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("Regular Students", regularCount > 0 ? regularSum / regularCount : 0.0);
        result.put("Honors Students", honorsCount > 0 ? honorsSum / honorsCount : 0.0);
        return result;
    }

    // Utility for Student average calculation from grades array
    // Add this method to Student.java if not present
    // public double calculateAverageFromGrades(Grade[] grades, int gradeCount) { ... }

    // Print full statistics report
    public void printStatisticsReport() {
        System.out.println("STATISTICAL ANALYSIS");
        System.out.println("====================");
        System.out.printf("Mean (Average): %.2f%%\n", calculateMean());
        System.out.printf("Median: %.2f%%\n", calculateMedian());
        System.out.printf("Mode: %.2f%%\n", calculateMode());
        System.out.printf("Standard Deviation: %.2f\n", calculateStdDev());
        System.out.printf("Range: %.2f\n", calculateRange());
        System.out.printf("Highest Grade: %.2f%%\n", getHighestGrade());
        System.out.printf("Lowest Grade: %.2f%%\n", getLowestGrade());
        System.out.println();

        System.out.println("SUBJECT PERFORMANCE");
        Map<String, Double> subjectAvgs = getSubjectAverages();
        for (Map.Entry<String, Double> entry : subjectAvgs.entrySet()) {
            System.out.printf("%-20s: %.2f%%\n", entry.getKey(), entry.getValue());
        }
        System.out.println();

        System.out.println("CLASS STATISTICS");
        System.out.printf("Total Students: %d\n", studentCount);
        System.out.printf("Total Grades Recorded: %d\n", gradeCount);
        System.out.println();

        System.out.println("GRADE DISTRIBUTION");
        Map<String, Integer> dist = getGradeDistribution();
        int totalGrades = gradeCount;
        for (Map.Entry<String, Integer> entry : dist.entrySet()) {
            double percent = totalGrades > 0 ? (entry.getValue() * 100.0 / totalGrades) : 0.0;
            System.out.printf("%-10s: %2d grades (%.1f%%)\n", entry.getKey(), entry.getValue(), percent);
        }
        System.out.println();

        System.out.println("STUDENT TYPE COMPARISON");
        Map<String, Double> typeAvgs = getStudentTypeAverages();
        for (Map.Entry<String, Double> entry : typeAvgs.entrySet()) {
            System.out.printf("%-20s: %.2f%% average\n", entry.getKey(), entry.getValue());
        }
    }



    /**
     * Prints a detailed GPA report for a student.
     */
    public void printStudentGPAReport(Student student) {
        Grade[] grades = gradeService.getGrades();
        int gradeCount = gradeService.getGradeCount();
        double total = 0.0;
        int count = 0;
        double gpaSum = 0.0;
        int gpaCount = 0;
        List<String> subjects = new ArrayList<>();
        List<String> gradeStrings = new ArrayList<>();
        List<String> gpaStrings = new ArrayList<>();
        List<String> letterGrades = new ArrayList<>();

        // GPA scale: 90-100 = 4.0 (A), 80-89 = 3.7 (A-), 75-79 = 3.3 (B+), 70-74 = 3.0 (B), 65-69 = 2.7 (B-), 60-64 = 2.3 (C+), 50-59 = 2.0 (C), <50 = 0.0 (F)
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                subjects.add(g.getSubjectName());
                gradeStrings.add(g.getValue() + "%");
                double gpa = convertToGPA(g.getValue());
                gpaStrings.add(String.format("%.1f", gpa));
                letterGrades.add(getLetterGrade(g.getValue()));
                gpaSum += gpa;
                gpaCount++;
                total += g.getValue();
                count++;
            }
        }

        double average = count > 0 ? total / count : 0.0;
        double cumulativeGPA = gpaCount > 0 ? gpaSum / gpaCount : 0.0;

        System.out.printf("Student: %s - %s%n", student.getStudentID(), student.getName());
        System.out.printf("Type: %s%n", (student instanceof models.HonorsStudent) ? "Honors Student" : "Regular Student");
        System.out.printf("Overall Average: %.1f%%%n", average);
        System.out.println("GPA CALCULATION (4.0 Scale)");
        System.out.println(String.format("%-15s %-8s %-8s %-10s", "Subject", "Grade", "GPA", "Points"));
        for (int i = 0; i < subjects.size(); i++) {
            System.out.println(String.format("%-15s %-8s %-8s %-10s", subjects.get(i), gradeStrings.get(i), gpaStrings.get(i), "(" + letterGrades.get(i) + ")"));
        }
        System.out.printf("Cumulative GPA: %.2f / 4.0%n", cumulativeGPA);
        System.out.printf("Letter Grade: %s%n", getLetterGrade(average));
        // Class rank and performance analysis can be added if you have class-wide GPA data
        System.out.println("Performance Analysis:");
        if (cumulativeGPA >= 3.5) {
            System.out.println("Excellent performance (3.5+ GPA)");
            if (student instanceof models.HonorsStudent) {
                System.out.println("Honors eligibility maintained");
            }
        } else if (cumulativeGPA >= 3.0) {
            System.out.println("Above average performance (3.0+ GPA)");
        } else if (cumulativeGPA >= 2.0) {
            System.out.println("Satisfactory performance (2.0+ GPA)");
        } else {
            System.out.println("Needs improvement (<2.0 GPA)");
        }
    }

    private double convertToGPA(double grade) {
        if (grade >= 93) return 4.0;
        if (grade >= 90) return 3.7;
        if (grade >= 87) return 3.3;
        if (grade >= 83) return 3.0;
        if (grade >= 80) return 2.7;
        if (grade >= 77) return 2.3;
        if (grade >= 73) return 2.0;
        if (grade >= 70) return 1.7;
        if (grade >= 67) return 1.3;
        if (grade >= 60) return 1.0;
        return 0.0;
    }

    private String getLetterGrade(double grade) {
        if (grade >= 93) return "A";
        if (grade >= 90) return "A-";
        if (grade >= 87) return "B+";
        if (grade >= 83) return "B";
        if (grade >= 80) return "B-";
        if (grade >= 77) return "C+";
        if (grade >= 73) return "C";
        if (grade >= 70) return "C-";
        if (grade >= 67) return "D+";
        if (grade >= 60) return "D";
        return "F";
    }
}