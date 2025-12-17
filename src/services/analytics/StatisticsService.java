package services.analytics;

import models.Grade;
import models.Student;
import models.HonorsStudent;
import models.RegularStudent;
import services.file.GradeService;
import java.util.*;

/**
 * Provides statistical analysis and reporting for grades and students.
 */
public class StatisticsService {

    private final Grade[] grades;
    private final int gradeCount;
    private final Collection<Student> students;
    private final int studentCount;
    private final services.file.GradeService gradeService;

    /**
     * Constructs a StatisticsService with the provided grades, students, and supporting services.
     * @param grades Array of Grade objects.
     * @param gradeCount Number of grades recorded.
     * @param students Array of Student objects.
     * @param studentCount Number of students.
     * @param gradeService GradeService instance for grade operations.
     */
    public StatisticsService(Grade[] grades, int gradeCount, Collection<Student> students, int studentCount, GradeService gradeService) {
        this.grades = grades;
        this.gradeCount = gradeCount;
        this.students = students;
        this.studentCount = studentCount;
        this.gradeService = gradeService;
    }

    /**
     * Helper method to get all grade values as a list.
     * @return List of grade values.
     */
    private List<Double> getAllGradeValues() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < gradeCount; i++) {
            if (grades[i] != null) {
                values.add(grades[i].getValue());
            }
        }
        return values;
    }

    /**
     * Calculates the mean (average) of all grades.
     * @return Mean value.
     */
    public double calculateMean() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    /**
     * Calculates the median of all grades.
     * The median is the middle value when grades are sorted in ascending order.
     * For an odd number of values, it's the middle element.
     * For an even number of values, it's the average of the two middle elements.
     * 
     * Time Complexity: O(n log n) due to sorting
     * Space Complexity: O(n) for storing sorted values
     * 
     * @return Median value, or 0.0 if no grades exist.
     */
    public double calculateMedian() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        
        // Sort grades to find the middle value(s)
        Collections.sort(values);
        int n = values.size();
        
        // Odd number of grades: return the middle element
        if (n % 2 == 1) {
            return values.get(n / 2);
        } else {
            // Even number of grades: return average of two middle elements
            // This ensures accurate median calculation for even-sized datasets
            return (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0;
        }
    }

    /**
     * Calculates the mode (most frequently occurring value) of all grades.
     * The mode represents the grade value that appears most often in the dataset.
     * If all values appear with equal frequency (frequency of 1), no mode exists.
     * 
     * Algorithm:
     * 1. Build a frequency map counting occurrences of each grade value
     * 2. Find the grade value with the highest frequency
     * 3. Return 0.0 if all frequencies are equal (no distinct mode)
     * 
     * Time Complexity: O(n) - single pass through values
     * Space Complexity: O(n) - for frequency map in worst case (all unique values)
     * 
     * @return Mode value (most frequent grade), or 0.0 if no mode exists.
     */
    public double calculateMode() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;

        // Build frequency map: count occurrences of each grade value
        // Using HashMap for O(1) average-case lookup and update
        Map<Double, Integer> countMap = new HashMap<>();
        for (double v : values) {
            // Increment count if value exists, otherwise initialize to 1
            if (countMap.containsKey(v)) {
                countMap.put(v, countMap.get(v) + 1);
            } else {
                countMap.put(v, 1);
            }
        }

        // Find the grade value with maximum frequency
        double mode = 0.0;
        int maxCount = 1; // Minimum frequency to be considered a mode
        for (Map.Entry<Double, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mode = entry.getKey();
            }
        }
        
        // If all frequencies are 1, no distinct mode exists
        // This indicates all grade values are unique
        if (maxCount == 1) {
            return 0.0;
        }
        return mode;
    }

    /**
     * Calculates the standard deviation of all grades using the population standard deviation formula.
     * Standard deviation measures the spread or dispersion of grade values around the mean.
     * 
     * Formula: σ = √(Σ(xi - μ)² / N)
     * Where:
     *   - σ = standard deviation
     *   - xi = individual grade value
     *   - μ = mean (average) of all grades
     *   - N = total number of grades
     * 
     * Algorithm:
     * 1. Calculate the mean of all grades
     * 2. For each grade, compute squared difference from mean: (value - mean)²
     * 3. Sum all squared differences
     * 4. Divide by number of grades (population variance)
     * 5. Take square root to get standard deviation
     * 
     * Time Complexity: O(n) - two passes: one for mean, one for variance
     * Space Complexity: O(1) - only using constant extra space
     * 
     * @return Standard deviation value, or 0.0 if no grades exist.
     */
    public double calculateStdDev() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        
        // Step 1: Calculate mean (average) of all grades
        double mean = calculateMean();
        
        // Step 2: Calculate sum of squared differences from mean
        // This measures how much each grade deviates from the average
        double sumSq = 0.0;
        for (double v : values) {
            // Square the difference to eliminate negative values and emphasize larger deviations
            sumSq += Math.pow(v - mean, 2);
        }
        
        // Step 3: Calculate variance (average of squared differences) and take square root
        // Using population standard deviation (dividing by N, not N-1)
        return Math.sqrt(sumSq / values.size());
    }

    /**
     * Calculates the range of all grades.
     * @return Range value.
     */
    public double calculateRange() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        double min = Collections.min(values);
        double max = Collections.max(values);
        return max - min;
    }

    /**
     * Gets the highest grade value.
     * @return Highest grade.
     */
    public double getHighestGrade() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        return Collections.max(values);
    }

    /**
     * Gets the lowest grade value.
     * @return Lowest grade.
     */
    public double getLowestGrade() {
        List<Double> values = getAllGradeValues();
        if (values.isEmpty()) return 0.0;
        return Collections.min(values);
    }

    /**
     * Calculates the average grade for each subject across all students.
     * Groups grades by subject name and computes the mean for each subject.
     * 
     * Algorithm:
     * 1. Group all grades by subject name using a HashMap
     * 2. For each subject, collect all grade values into a list
     * 3. Calculate the average of each subject's grade list using stream operations
     * 4. Return results in insertion order (LinkedHashMap preserves subject order)
     * 
     * Time Complexity: O(n) where n is the number of grades
     * Space Complexity: O(m) where m is the number of unique subjects
     * 
     * @return Map of subject names to their average grades, ordered by first appearance.
     */
    public Map<String, Double> getSubjectAverages() {
        // Step 1: Group grades by subject name
        // Using computeIfAbsent for efficient grouping: creates list if subject doesn't exist
        Map<String, List<Double>> subjectGrades = new HashMap<>();
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null) {
                // Add grade value to the list for this subject
                // computeIfAbsent ensures thread-safe list creation if subject is new
                subjectGrades.computeIfAbsent(g.getSubjectName(), k -> new ArrayList<>()).add(g.getValue());
            }
        }
        
        // Step 2: Calculate average for each subject
        // Using LinkedHashMap to preserve insertion order for consistent output
        Map<String, Double> averages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : subjectGrades.entrySet()) {
            List<Double> vals = entry.getValue();
            // Use stream API for concise average calculation
            // orElse(0.0) handles edge case where list might be empty (shouldn't happen)
            double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            averages.put(entry.getKey(), avg);
        }
        return averages;
    }

    /**
     * Calculates the distribution of grades across predefined performance ranges.
     * This histogram-like analysis helps identify grade clustering and performance patterns.
     * 
     * Grade Ranges (Bins):
     * - Bin 0: 0-59 (Failing range)
     * - Bin 1: 60-69 (D range)
     * - Bin 2: 70-79 (C range)
     * - Bin 3: 80-89 (B range)
     * - Bin 4: 90-100 (A range)
     * 
     * Algorithm:
     * 1. Initialize array of 5 bins (one for each grade range)
     * 2. Iterate through all grades and increment appropriate bin based on value
     * 3. Convert bin array to labeled map for readable output
     * 
     * Time Complexity: O(n) where n is the number of grades
     * Space Complexity: O(1) - fixed-size array regardless of input size
     * 
     * @return Map of grade ranges to counts, ordered from lowest to highest range.
     */
    public Map<String, Integer> getGradeDistribution() {
        // Initialize bins for 5 grade ranges: 0-59, 60-69, 70-79, 80-89, 90-100
        // Using array for O(1) access and minimal memory overhead
        int[] bins = new int[5];
        
        // Categorize each grade into appropriate bin
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null) {
                double v = g.getValue();
                // Use cascading if-else for efficient bin assignment
                // Each grade is checked against ranges in descending order
                if (v < 60) bins[0]++;           // Failing range
                else if (v < 70) bins[1]++;      // D range
                else if (v < 80) bins[2]++;      // C range
                else if (v < 90) bins[3]++;      // B range
                else bins[4]++;                  // A range (90-100)
            }
        }
        
        // Convert bin array to labeled map for human-readable output
        // LinkedHashMap preserves order for consistent reporting
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("0-59", bins[0]);
        dist.put("60-69", bins[1]);
        dist.put("70-79", bins[2]);
        dist.put("80-89", bins[3]);
        dist.put("90-100", bins[4]);
        return dist;
    }

    /**
     * Calculates average grades for each student type (Regular and Honors).
     * @return Map of student type to average grade.
     */
    public Map<String, Double> getStudentTypeAverages() {
        double regularSum = 0.0, honorsSum = 0.0;
        int regularCount = 0, honorsCount = 0;
        for (Student s : students) {
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



    /**
     * Prints a full statistics report to the console.
     */
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
     * @param student Student whose GPA report is to be printed.
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

        // Collect and process all grades for this specific student
        // Iterate through the entire grade array to find matching student ID
        // This approach allows for efficient filtering without additional data structures
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            // Case-insensitive comparison ensures ID matching regardless of case
            if (g != null && g.getStudentID().equalsIgnoreCase(student.getStudentID())) {
                // Collect subject information for display
                subjects.add(g.getSubjectName());
                gradeStrings.add(g.getValue() + "%");
                
                // Convert percentage grade to 4.0 GPA scale
                // GPA conversion uses tiered system: A=4.0, B=3.0, C=2.0, D=1.0, F=0.0
                double gpa = convertToGPA(g.getValue());
                gpaStrings.add(String.format("%.1f", gpa));
                
                // Get letter grade equivalent (A, B+, C-, etc.)
                letterGrades.add(getLetterGrade(g.getValue()));
                
                // Accumulate values for cumulative calculations
                gpaSum += gpa;           // Sum for cumulative GPA
                gpaCount++;              // Count for averaging
                total += g.getValue();   // Sum for percentage average
                count++;                 // Count for percentage average
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

    /**
     * Converts a percentage grade to GPA on a standard 4.0 scale.
     * This implements a common US grading scale where:
     * - A (90-100) = 3.7-4.0
     * - B (80-89) = 2.7-3.0
     * - C (70-79) = 2.0-2.3
     * - D (60-69) = 1.0-1.7
     * - F (<60) = 0.0
     * 
     * The conversion uses a tiered system with plus/minus distinctions:
     * Higher percentages within a letter grade range receive higher GPA values.
     * 
     * Algorithm:
     * Uses cascading if-statements for efficient range checking.
     * Checks from highest to lowest to match first applicable range.
     * 
     * Time Complexity: O(1) - constant time regardless of input
     * Space Complexity: O(1) - no additional data structures
     * 
     * @param grade Percentage grade value (0-100)
     * @return Corresponding GPA value on 4.0 scale (0.0-4.0)
     */
    private double convertToGPA(double grade) {
        // A range: 4.0 scale
        if (grade >= 93) return 4.0;   // A (93-100)
        if (grade >= 90) return 3.7;   // A- (90-92)
        
        // B range: 2.7-3.3 scale
        if (grade >= 87) return 3.3;   // B+ (87-89)
        if (grade >= 83) return 3.0;   // B (83-86)
        if (grade >= 80) return 2.7;   // B- (80-82)
        
        // C range: 2.0-2.3 scale
        if (grade >= 77) return 2.3;   // C+ (77-79)
        if (grade >= 73) return 2.0;   // C (73-76)
        if (grade >= 70) return 1.7;   // C- (70-72)
        
        // D range: 1.0-1.3 scale
        if (grade >= 67) return 1.3;   // D+ (67-69)
        if (grade >= 60) return 1.0;    // D (60-66)
        
        // F range: failing grades
        return 0.0;                     // F (<60)
    }

    /**
     * Converts a grade value to a letter grade.
     * @param grade Grade value.
     * @return Letter grade as a String.
     */
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