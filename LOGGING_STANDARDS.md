# Logging Standards

This document defines the consistent logging patterns used throughout the Student Grade Management System.

## Log Message Format

All log messages follow the pattern: `OPERATION_NAME: Message`

### Operation Names
- `APPLICATION` - Application lifecycle events (start, shutdown, exit)
- `ADD_STUDENT` - Student addition operations
- `FIND_STUDENT` - Student lookup operations
- `RECORD_GRADE` - Grade recording operations
- `CALCULATE_STUDENT_GPA` - GPA calculation operations
- `VIEW_CLASS_STATISTICS` - Class statistics viewing
- `REAL_TIME_STATISTICS_DASHBOARD` - Real-time dashboard operations
- `GENERATE_BATCH_REPORTS` - Batch report generation
- `SEARCH_STUDENTS` - Student search operations
- `PATTERN_BASED_SEARCH` - Pattern-based search operations
- `BATCH_EXPORT` - Individual batch export operations
- `BATCH_EXPORT_ALL` - Complete batch export operations
- `SCHEDULE_TASK` - Task scheduling operations
- `SCHEDULED_TASK` - Scheduled task execution
- `SCHEDULED_TASK_EXECUTION` - Scheduled task performance metrics
- `TASK_SCHEDULER` - Task scheduler initialization
- `BATCH_REPORT_MANAGER` - Batch report manager initialization
- `MENU_HANDLER` - Menu handler errors
- `VALIDATION_UTILS` - Input validation errors

## Log Levels

### INFO
- Operation start: `OPERATION_NAME: Starting`
- Operation completion: `OPERATION_NAME: Completed`
- Initialization: `COMPONENT_NAME: Initialized with details`
- General information: `OPERATION_NAME: Information message`

### DEBUG
- Detailed operation information: `OPERATION_NAME: Detailed message`
- Internal state changes: `OPERATION_NAME: State change description`

### WARN
- Non-critical errors: `OPERATION_NAME: Warning description - details`
- Validation failures: `OPERATION_NAME: Validation failed - reason`
- Retryable errors: `OPERATION_NAME: Retryable error - details`

### ERROR
- Critical errors: `OPERATION_NAME: Error description - details`
- Always include exception: `Logger.error("OPERATION_NAME: Error - " + e.getMessage(), e)`

## Audit Trail Logging

Format: `Logger.logAudit(operation, action, duration, success, details)`

- **operation**: Operation name (e.g., "ADD_STUDENT")
- **action**: Brief action description (e.g., "Add student STU001")
- **duration**: Execution time in milliseconds
- **success**: true/false
- **details**: Additional details or error message

Example:
```java
Logger.logAudit("ADD_STUDENT", "Add student STU001", duration, true, "Student added successfully");
Logger.logAudit("ADD_STUDENT", "Add student STU001", duration, false, "Duplicate student ID: STU001");
```

## Performance Logging

Format: `Logger.logPerformance(operation, duration, metrics)`

- **operation**: Operation name
- **duration**: Execution time in milliseconds
- **metrics**: Map containing relevant metrics (collection sizes, thread pool stats, etc.)

Example:
```java
Map<String, Object> metrics = new HashMap<>();
metrics.put("studentCount", studentMap.size());
Logger.logPerformance("ADD_STUDENT", duration, metrics);
```

## Best Practices

1. **Always use operation name prefix** - Makes logs searchable and filterable
2. **Include context in messages** - Add relevant IDs, counts, or identifiers
3. **Log at appropriate levels** - Use INFO for normal flow, WARN for recoverable issues, ERROR for failures
4. **Include exceptions** - Always pass exception object to error() method
5. **Log operation start and end** - Helps track operation lifecycle
6. **Use consistent action descriptions** - Keep audit trail action descriptions concise and consistent
7. **Include metrics for performance** - Log collection sizes, thread pool stats, etc. for performance monitoring

## Examples

### Operation Start
```java
Logger.info("CALCULATE_STUDENT_GPA: Starting");
```

### Success Audit
```java
Logger.logAudit("CALCULATE_STUDENT_GPA", "Calculate GPA for STU001", duration, true, "GPA calculated successfully");
```

### Failure Audit
```java
Logger.logAudit("CALCULATE_STUDENT_GPA", "Calculate GPA", duration, false, "Student not found: STU001");
```

### Error with Exception
```java
Logger.error("ADD_STUDENT: Error adding student - " + e.getMessage(), e);
```

### Warning
```java
Logger.warn("ADD_STUDENT: Duplicate student detected - " + e.getMessage());
```

### Performance
```java
Map<String, Object> metrics = new HashMap<>();
metrics.put("resultCount", searchResults.length);
metrics.put("searchOption", searchOption);
Logger.logPerformance("SEARCH_STUDENTS", duration, metrics);
```

