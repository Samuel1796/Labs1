package services.search;

import models.Student;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Advanced Pattern-Based Search Service (US-7).
 * 
 * Provides regex-based search functionality for finding students
 * matching complex criteria.
 * 
 * Features:
 * - Email domain pattern search
 * - Phone area code pattern search
 * - Student ID pattern with wildcards
 * - Name pattern search
 * - Custom regex pattern input
 * - Match highlighting
 * - Pattern match statistics
 * - Case-insensitive matching option
 * - Distribution statistics
 */
public class PatternSearchService {
    
    private final Collection<Student> students;
    
    /**
     * Search result with match information.
     */
    public static class SearchResult {
        private final Student student;
        private final String matchedField;
        private final String matchedText;
        private final String highlightedMatch;
        
        public SearchResult(Student student, String matchedField, String matchedText, String highlightedMatch) {
            this.student = student;
            this.matchedField = matchedField;
            this.matchedText = matchedText;
            this.highlightedMatch = highlightedMatch;
        }
        
        public Student getStudent() { return student; }
        public String getMatchedField() { return matchedField; }
        public String getMatchedText() { return matchedText; }
        public String getHighlightedMatch() { return highlightedMatch; }
    }
    
    /**
     * Search statistics.
     */
    public static class SearchStatistics {
        private final int totalScanned;
        private final int matchesFound;
        private final long searchTime;
        private final String patternComplexity;
        
        public SearchStatistics(int totalScanned, int matchesFound, long searchTime, String patternComplexity) {
            this.totalScanned = totalScanned;
            this.matchesFound = matchesFound;
            this.searchTime = searchTime;
            this.patternComplexity = patternComplexity;
        }
        
        public int getTotalScanned() { return totalScanned; }
        public int getMatchesFound() { return matchesFound; }
        public long getSearchTime() { return searchTime; }
        public String getPatternComplexity() { return patternComplexity; }
    }
    
    public PatternSearchService(Collection<Student> students) {
        this.students = students;
    }
    
    /**
     * Searches students by email domain pattern.
     * 
     * @param domainPattern Pattern for email domain (e.g., "@university.edu")
     * @param caseSensitive Whether matching should be case-sensitive
     * @return Search results with statistics
     */
    public Map<String, Object> searchByEmailDomain(String domainPattern, boolean caseSensitive) {
        long startTime = System.currentTimeMillis();
        String regex = ".*" + Pattern.quote(domainPattern) + ".*";
        Pattern pattern = caseSensitive ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        List<SearchResult> results = new ArrayList<>();
        int scanned = 0;
        
        for (Student student : students) {
            scanned++;
            String email = student.getEmail();
            Matcher matcher = pattern.matcher(email);
            
            if (matcher.find()) {
                String highlighted = highlightMatch(email, matcher, pattern);
                results.add(new SearchResult(student, "email", email, highlighted));
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        String complexity = assessPatternComplexity(regex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("statistics", new SearchStatistics(scanned, results.size(), searchTime, complexity));
        response.put("distribution", getEmailDomainDistribution(results));
        
        return response;
    }
    
    /**
     * Searches students by phone area code pattern.
     */
    public Map<String, Object> searchByPhoneAreaCode(String areaCodePattern, boolean caseSensitive) {
        long startTime = System.currentTimeMillis();
        // Convert wildcards to regex
        String regex = areaCodePattern.replace("*", "\\d").replace("?", "\\d");
        if (!regex.startsWith("^")) regex = ".*" + regex;
        Pattern pattern = caseSensitive ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        List<SearchResult> results = new ArrayList<>();
        int scanned = 0;
        
        for (Student student : students) {
            scanned++;
            String phone = student.getPhone();
            Matcher matcher = pattern.matcher(phone);
            
            if (matcher.find()) {
                String highlighted = highlightMatch(phone, matcher, pattern);
                results.add(new SearchResult(student, "phone", phone, highlighted));
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        String complexity = assessPatternComplexity(regex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("statistics", new SearchStatistics(scanned, results.size(), searchTime, complexity));
        response.put("distribution", getPhoneAreaCodeDistribution(results));
        
        return response;
    }
    
    /**
     * Searches students by Student ID pattern with wildcards.
     * Supports * (any characters) and ? (single character) wildcards.
     */
    public Map<String, Object> searchByStudentIdPattern(String idPattern, boolean caseSensitive) {
        long startTime = System.currentTimeMillis();
        // Convert wildcards to regex
        String regex = idPattern.replace("*", ".*").replace("?", ".");
        if (!regex.startsWith("^")) regex = "^" + regex;
        if (!regex.endsWith("$")) regex = regex + "$";
        
        Pattern pattern = caseSensitive ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        List<SearchResult> results = new ArrayList<>();
        int scanned = 0;
        
        for (Student student : students) {
            scanned++;
            String studentId = student.getStudentID();
            Matcher matcher = pattern.matcher(studentId);
            
            if (matcher.matches()) {
                String highlighted = highlightMatch(studentId, matcher, pattern);
                results.add(new SearchResult(student, "studentId", studentId, highlighted));
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        String complexity = assessPatternComplexity(regex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("statistics", new SearchStatistics(scanned, results.size(), searchTime, complexity));
        
        return response;
    }
    
    /**
     * Searches students by name pattern.
     */
    public Map<String, Object> searchByNamePattern(String namePattern, boolean caseSensitive) {
        long startTime = System.currentTimeMillis();
        String regex = ".*" + Pattern.quote(namePattern) + ".*";
        Pattern pattern = caseSensitive ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        List<SearchResult> results = new ArrayList<>();
        int scanned = 0;
        
        for (Student student : students) {
            scanned++;
            String name = student.getName();
            Matcher matcher = pattern.matcher(name);
            
            if (matcher.find()) {
                String highlighted = highlightMatch(name, matcher, pattern);
                results.add(new SearchResult(student, "name", name, highlighted));
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        String complexity = assessPatternComplexity(regex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("statistics", new SearchStatistics(scanned, results.size(), searchTime, complexity));
        
        return response;
    }
    
    /**
     * Custom regex pattern search across all student fields.
     */
    public Map<String, Object> searchByCustomPattern(String customPattern, boolean caseSensitive) {
        long startTime = System.currentTimeMillis();
        
        Pattern pattern;
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(customPattern, flags);
        } catch (java.util.regex.PatternSyntaxException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid regex pattern: " + e.getMessage());
            error.put("pattern", customPattern);
            return error;
        }
        
        List<SearchResult> results = new ArrayList<>();
        int scanned = 0;
        String[] fields = {"studentId", "name", "email", "phone"};
        
        for (Student student : students) {
            scanned++;
            String[] values = {
                student.getStudentID(),
                student.getName(),
                student.getEmail(),
                student.getPhone()
            };
            
            for (int i = 0; i < fields.length; i++) {
                Matcher matcher = pattern.matcher(values[i]);
                if (matcher.find()) {
                    String highlighted = highlightMatch(values[i], matcher, pattern);
                    results.add(new SearchResult(student, fields[i], values[i], highlighted));
                    break; // Only add once per student
                }
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        String complexity = assessPatternComplexity(customPattern);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("statistics", new SearchStatistics(scanned, results.size(), searchTime, complexity));
        
        return response;
    }
    
    /**
     * Highlights the matched portion of text.
     */
    private String highlightMatch(String text, Matcher matcher, Pattern pattern) {
        if (matcher.groupCount() == 0) {
            // Simple match highlighting
            String match = matcher.group();
            return text.replace(match, ">>>" + match + "<<<");
        } else {
            // Multiple groups
            StringBuilder highlighted = new StringBuilder(text);
            int offset = 0;
            matcher.reset();
            while (matcher.find()) {
                String match = matcher.group();
                int start = matcher.start() + offset;
                int end = matcher.end() + offset;
                highlighted.replace(start, end, ">>>" + match + "<<<");
                offset += 7; // Length of ">>>" + "<<<"
            }
            return highlighted.toString();
        }
    }
    
    /**
     * Assesses pattern complexity and provides hint.
     */
    private String assessPatternComplexity(String pattern) {
        // Simple heuristic: count quantifiers and alternations
        int quantifiers = (pattern.split("\\*|\\+|\\?|\\{")).length - 1;
        int alternations = (pattern.split("\\|")).length - 1;
        
        if (quantifiers > 10 || alternations > 5) {
            return "WARNING: This pattern may be slow for large datasets";
        } else if (quantifiers > 5 || alternations > 2) {
            return "MODERATE: Pattern complexity is moderate";
        } else {
            return "SIMPLE: Pattern should perform well";
        }
    }
    
    /**
     * Gets email domain distribution from search results.
     */
    private Map<String, Integer> getEmailDomainDistribution(List<SearchResult> results) {
        return results.stream()
            .map(r -> {
                String email = r.getStudent().getEmail();
                int atIndex = email.indexOf('@');
                return atIndex > 0 ? email.substring(atIndex + 1) : "unknown";
            })
            .collect(Collectors.groupingBy(
                domain -> domain,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }
    
    /**
     * Gets phone area code distribution from search results.
     */
    private Map<String, Integer> getPhoneAreaCodeDistribution(List<SearchResult> results) {
        return results.stream()
            .map(r -> {
                String phone = r.getStudent().getPhone();
                // Extract area code (first 3 digits)
                String digits = phone.replaceAll("\\D", "");
                return digits.length() >= 3 ? digits.substring(0, 3) : "unknown";
            })
            .collect(Collectors.groupingBy(
                areaCode -> areaCode,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }
}

