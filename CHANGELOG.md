# CHANGELOG

## v3.0.0 – Objectives Achievement & Code Quality Release (2025-12-17)

This release demonstrates comprehensive achievement of all five core project objectives through systematic implementation of advanced Java features and best practices.

### **Objective 1: Design and Implement Type-Safe Data Structures**

The project extensively uses Java Collections Framework and generics to create type-safe, efficient data structures:

- **Type-Safe Collections with Generics:**
  - `StudentService` uses `HashMap<String, Student>` for O(1) student lookups by ID, with generic type parameters ensuring compile-time type safety.
  - `StatisticsService` employs `Set<String>` for unique course tracking and `List<Double>` for grade value collections, leveraging generic type safety.
  - `LRUCache<K, V>` is a fully generic cache implementation using `ConcurrentHashMap<K, CacheEntry<V>>` for thread-safe, type-safe caching.
  - `TaskScheduler` uses `PriorityQueue<ScheduledTask>` with custom comparators for ordered task execution.
  - `PatternSearchService` returns `List<SearchResult>` with strongly-typed result objects.

- **Optimal Collection Selection Based on Access Patterns:**
  - **HashMap** (`StudentService.studentMap`): Selected for O(1) average-time student ID lookups and insertions, ideal for frequent `findStudentById()` operations.
  - **HashSet** (`StatisticsService.uniqueCourses`): Chosen for O(1) average-time membership checks when tracking unique course names during statistics computation.
  - **LinkedList** (`GradeService.gradeHistory`): Used for O(1) append operations and chronological iteration of grade history, supporting efficient sequential access patterns.
  - **ArrayList** (`RegularStudent.enrolledSubjects`, `StudentService` search results): Selected for O(1) indexed access and efficient iteration when order matters.
  - **ConcurrentHashMap** (`BatchReportTaskManager.threadStatus`, `TaskScheduler.scheduledTasks`, `LRUCache.cache`): Chosen for thread-safe concurrent access without explicit synchronization, enabling lock-free reads and writes in multi-threaded contexts.
  - **PriorityQueue** (`TaskScheduler.taskQueue`): Used for O(log n) insertion and O(1) peek operations when maintaining tasks ordered by execution time.

- **Performance Characteristics:**
  - All collection choices are documented with time complexity analysis in `TIME_COMPLEXITY.md`.
  - Collections are sized appropriately (e.g., `Grade[]` with fixed capacity, `HashMap` with default load factor) to balance memory usage and performance.

### **Objective 2: Implement Modern File I/O Operations**

The project implements comprehensive file I/O using NIO.2 API and Stream processing for multiple formats:

- **NIO.2 API Usage:**
  - `FileIOUtils.readGradesFromCSV()` and `readStudentsFromCSV()` use `Files.lines(Path)` with try-with-resources for automatic resource management, reading files as streams without loading entire content into memory.
  - `FileIOUtils.writeGradesToCSV()` and `writeStudentsToCSV()` use `Files.newBufferedWriter(Path)` for efficient buffered writing with NIO.2 paths.
  - `FileIOUtils.monitorDirectory()` implements `WatchService` API to monitor the `./imports` directory for file creation/modification events, enabling reactive file processing.
  - All file operations use `java.nio.file.Paths` for path construction and `Path` objects for type-safe file system operations.

- **Stream Processing for Functional Transformations:**
  - CSV reading uses `Stream<String>` with `Iterator` for line-by-line processing, enabling functional transformations (e.g., `lines.skip(1)` for header removal, `filter()` for data validation).
  - `StudentService.searchStudentsByName()` uses Java Streams API (`stream().filter().toArray()`) for declarative, functional-style filtering operations.
  - `StudentService.searchStudentsByGradeRange()` and `searchStudentsByType()` leverage Streams for functional data transformations without imperative loops.

- **Multiple File Format Support:**
  - **CSV**: `FileIOUtils.readGradesFromCSV()` / `writeGradesToCSV()` for comma-separated value import/export with proper header handling.
  - **JSON**: `FileIOUtils.readGradesFromJSON()` / `writeGradesToJSON()` using Jackson `ObjectMapper` with `TypeReference<List<Grade>>` for type-safe JSON serialization/deserialization.
  - **Binary**: `FileIOUtils.readGradesFromBinary()` / `writeGradesToBinary()` using `ObjectInputStream` / `ObjectOutputStream` with `Files.newInputStream()` / `Files.newOutputStream()` for efficient binary serialization.
  - **PDF**: `GradeImportExportService.exportGradeReportPDF()` using iText library for PDF generation.
  - **Excel**: `GradeImportExportService.exportGradeReportExcel()` using Apache POI for XLSX file generation.

- **Proper Resource Management:**
  - All file operations use try-with-resources statements to ensure automatic closure of streams, readers, and writers, preventing resource leaks.
  - `BatchReportTaskManager` includes explicit directory creation (`new File(outputDir).mkdirs()`) and post-write file verification to guard against silent I/O failures.

### **Objective 3: Create Comprehensive Input Validation**

The project implements robust input validation using regular expressions for all structured data formats:

- **Precompiled Regex Patterns:**
  - `ValidationUtils` defines static, precompiled `Pattern` objects (`STUDENT_ID_PATTERN`, `EMAIL_PATTERN`, `PHONE_PATTERN`) for efficient pattern matching without recompilation overhead.
  - Patterns are compiled once at class load time and reused across all validation operations, following best practices for regex performance.

- **Student ID Validation:**
  - Pattern: `^STU\\d{3}$` - Validates student IDs in format "STU" followed by exactly 3 digits (e.g., STU001, STU042, STU999).
  - `ValidationUtils.isValidStudentId()` and `validateStudentId()` provide validation with detailed error messages.

- **Email Address Validation:**
  - Pattern: `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$` - Validates standard email format with domain and TLD requirements.
  - `ValidationUtils.isValidEmail()` ensures email addresses conform to RFC-compliant formats.

- **Phone Number Validation (Ghana-Specific):**
  - Pattern: `^(0\\d{9}|\\+233\\d{9})$` - Supports both local format (0XXXXXXXXX) and international format (+233XXXXXXXXX) for Ghana phone numbers.
  - Validates exactly 10 digits for local format and 9 digits after country code for international format.
  - Examples: 0241234567, 0509876543, +233241234567, +233509876543.

- **Grade Value Validation:**
  - `ValidationUtils.isValidGrade()` validates numeric grades within 0-100 range using both string parsing and direct numeric validation.

- **Comprehensive Error Messages:**
  - `ValidationUtils.getValidationErrorMessage()` provides detailed, user-friendly error messages with expected patterns and examples for each validation failure.
  - Error messages include field name, invalid value, expected pattern description, and concrete examples.

- **Integration Across System:**
  - Validation is enforced during student creation, grade recording, bulk imports, and all user input operations.
  - `StudentService.addStudent()` and `GradeService.recordGrade()` integrate validation checks before data persistence.
  - `GradeImportExportService` validates all imported data before processing, ensuring data integrity across the system.

### **Objective 4: Design and Implement Thread-Safe Concurrent Operations**

The project implements comprehensive thread-safe concurrent operations using appropriate synchronization strategies and the Executor framework:

- **Executor Framework Usage:**
  - **Fixed Thread Pool**: `BatchReportTaskManager` uses `Executors.newFixedThreadPool(threadCount)` for concurrent batch report generation, preventing thread explosion and resource exhaustion.
  - **Scheduled Executor Service**: `StatisticsDashboard` uses `ScheduledExecutorService` with `scheduleAtFixedRate()` to recalculate statistics every 5 seconds in the background.
  - **Single Thread Executor**: `AuditTrailService` uses `Executors.newSingleThreadExecutor()` with daemon threads for asynchronous audit logging without blocking main operations.
  - **Cached Thread Pool**: `CacheUtils` provides `Executors.newCachedThreadPool()` for dynamic thread allocation based on workload.
  - **Scheduled Thread Pool**: `TaskScheduler` uses `ScheduledExecutorService` for recurring background tasks (GPA recalculation, stats refresh, backups).

- **Thread-Safe Data Structures:**
  - **ConcurrentHashMap**: Used extensively for thread-safe maps without explicit locking:
    - `BatchReportTaskManager.threadStatus` and `reportTimes` for concurrent status tracking.
    - `TaskScheduler.scheduledTasks` for concurrent task management.
    - `LRUCache.cache` for thread-safe cache operations.
  - **AtomicInteger / AtomicLong**: Used for lock-free counter updates:
    - `BatchReportTaskManager` uses `AtomicInteger` for `submittedTasks`, `startedTasks`, `completedTasks`, `failedTasks`.
    - `LRUCache` uses `AtomicInteger` for `hits`, `misses`, `evictions` and `AtomicLong` for timing metrics.
    - `AuditTrailService` uses `AtomicInteger` / `AtomicLong` for operation counters and execution time tracking.

- **Synchronization Strategies:**
  - **Non-Blocking Queues**: `AuditTrailService` uses `ConcurrentLinkedQueue<AuditEntry>` for lock-free producer-consumer pattern, allowing audit logging without blocking main operations.
  - **Synchronized Collections**: `TaskScheduler.executionHistory` uses `Collections.synchronizedList()` for thread-safe list operations when multiple threads append execution records.
  - **Volatile Fields**: `LRUCache.CacheEntry.lastAccessTime` uses `volatile` for safe concurrent read/write access without full synchronization.

- **Background Task Management:**
  - **Batch Processing**: `BatchReportTaskManager.startBatchExport()` submits tasks to thread pool, tracks progress with atomic counters, and provides real-time status updates.
  - **Automated Report Generation**: `TaskScheduler` schedules recurring batch report generation tasks with configurable intervals and execution history tracking.
  - **Real-Time Statistics Updates**: `StatisticsDashboard` runs as a daemon thread, continuously updating statistics without blocking the main menu.

- **Thread Safety Guarantees:**
  - All concurrent operations are designed to be thread-safe without data races.
  - Modern thread ID API: Uses `Thread.threadId()` instead of deprecated `Thread.getId()`.
  - Proper shutdown: All executor services implement graceful shutdown with `shutdown()` and `awaitTermination()` in shutdown hooks.

### **Objective 5: Optimize Application Performance**

The project implements comprehensive performance optimizations through collection analysis, efficient data access patterns, and thread safety:

- **Collection Performance Analysis:**
  - All collection choices are documented with time complexity in `TIME_COMPLEXITY.md`:
    - `HashMap`: O(1) average for get/put operations (student lookups).
    - `HashSet`: O(1) average for membership checks (unique course tracking).
    - `LinkedList`: O(1) for append, O(n) for indexed access (grade history).
    - `ArrayList`: O(1) for indexed access, O(n) for insertion (enrolled subjects).
    - `PriorityQueue`: O(log n) for insertion, O(1) for peek (task scheduling).
  - Collections are selected based on actual access patterns (e.g., frequent lookups → HashMap, sequential access → LinkedList).

- **Efficient Data Access Patterns:**
  - **Caching**: `LRUCache` implementation provides O(1) average get/put operations with automatic eviction of least-recently-used entries, reducing recomputation overhead in statistics and search operations.
  - **Lazy Initialization**: `GradeService` lazily creates `CoreSubject` / `ElectiveSubject` instances only when needed during grade recording.
  - **Stream Processing**: Java Streams API enables efficient functional transformations without intermediate collections (e.g., `filter().toArray()` chains).
  - **Precomputed Sets**: `StatisticsService` pre-populates `uniqueCourses` HashSet during construction to avoid repeated scans.

- **Thread Safety in Multi-Threaded Operations:**
  - **Lock-Free Algorithms**: Atomic operations (`AtomicInteger`, `AtomicLong`) provide thread-safe counters without explicit locks, reducing contention.
  - **Concurrent Collections**: `ConcurrentHashMap` enables concurrent reads and writes without blocking, improving throughput in multi-threaded scenarios.
  - **Thread Pool Management**: Fixed-size thread pools prevent thread explosion, and daemon threads ensure proper cleanup on application shutdown.
  - **Non-Blocking Queues**: `ConcurrentLinkedQueue` in `AuditTrailService` allows lock-free producer-consumer pattern for audit logging.

- **Performance Monitoring:**
  - `LRUCache` tracks cache hit/miss rates, eviction counts, and timing metrics for performance analysis.
  - `BatchReportTaskManager` provides throughput metrics (reports per second) and execution time tracking for batch operations.
  - `AuditTrailService` tracks operation execution times and success rates for performance auditing.

- **Memory Efficiency:**
  - Fixed-size arrays (`Grade[]`) for grade storage reduce memory overhead compared to dynamic collections when capacity is known.
  - LRU cache eviction prevents unbounded memory growth in caching scenarios.
  - Proper resource management (try-with-resources) prevents memory leaks from unclosed streams.

### **Code Quality Improvements in v3.0.0**

- Fixed duplicate `Logger.initialize()` call in `Main.java`.
- Resolved merge conflict in `StudentService.java` by removing conflict markers and consolidating imports.
- Fixed duplicate while loop structure in `Main.java` main method.
- Moved shutdown hook registration outside the main loop for proper cleanup.
- Removed unused imports and variables to eliminate compiler warnings.
- Improved code structure and readability in entry point.

### **Documentation Updates**

- Added comprehensive explanation of how each objective is addressed throughout the codebase.
- Updated changelog with detailed breakdown of objective implementations.
- All implementations align with Java best practices and performance optimization principles.

## v2.0.0 – Advanced Analytics & Concurrency Release (2025-12-18)

- **Architecture & Codebase Restructure**
  - Reorganized the `services` package into feature-oriented modules: `student`, `file`, `analytics`, `system`, `search`, and `menu`.
  - Extracted all grade import/export responsibilities into `services.file.GradeImportExportService`, keeping `GradeService` focused on core grade management and grade history.
  - Split UI concerns between `MainMenuHandler` (flow control) and `MenuService` (rendering), and wired in a dynamic background-task status banner on the main menu.

- **Analytics & Dashboards (US-5)**
  - Implemented a **Real-Time Statistics Dashboard** (`services.analytics.StatisticsDashboard`) backed by a daemon `ScheduledExecutorService` that recalculates class statistics every 5 seconds.
  - Reuses `StatisticsService` calculations so “View Class Statistics” and the dashboard share the same logic for mean, median, standard deviation, and grade distribution.
  - Displays live grade distribution, top performers with GPA conversion, cache hit rate, active thread count, memory usage, and last-update timestamp.
  - Supports interactive commands: `[R]` manual refresh, `[P]` pause/resume auto-refresh, `[Q]` clean shutdown with proper thread termination.

- **Scheduled Automated Tasks (US-6)**
  - Added `services.system.TaskScheduler` to manage recurring background work such as GPA recomputation, statistics cache refresh, batch report generation, and simulated database backups.
  - Uses `ScheduledExecutorService` plus a `PriorityQueue<ScheduledTask>` to maintain tasks ordered by next execution time, and persists task definitions to disk.
  - Tracks execution history with timestamps, durations, success flags, and human-readable details for auditing and troubleshooting.

- **Advanced Pattern-Based Search (US-7)**
  - Implemented `services.search.PatternSearchService` with regex- and wildcard-based search over student fields (ID, name, email, phone).
  - Supports domain-based email search, phone “area code”/prefix search with `*`/`?` wildcards, student ID patterns, name patterns, and full custom regex queries.
  - Returns rich search results including highlighted matches, per-field distributions, and timing/complexity metadata.

- **LRU Cache & Performance Utilities**
  - Added an `LRUCache` implementation in `services.system.LRUCache` using `ConcurrentHashMap` and a doubly-linked list to provide O(1) average `get`/`put` while safely supporting concurrent access.
  - Integrated caching in performance-sensitive paths (e.g., repeated lookups during analytics and search) to reduce recomputation and improve responsiveness for large datasets.

- **Concurrent Audit Trail (US-9)**
  - Introduced `services.system.AuditTrailService` to asynchronously record user actions and system events.
  - Uses an `ExecutorService` and non-blocking queues so that audit logging does not slow down the main menu or data operations.
  - Switched from deprecated `Thread.getId()` to modern `Thread.threadId()` and tightened thread-safety guarantees.

- **Batch Reporting & File I/O**
  - Implemented `services.file.BatchReportTaskManager` to generate per-student grade reports concurrently using a configurable fixed-size thread pool.
  - Supports CSV, JSON, binary, and multi-format export, with explicit directory creation and post-write file verification to guard against silent I/O failures.
  - Provides a live console progress bar, throughput metrics, background task count, and a final summary comparing overall performance.

- **Data Integrity & Validation Enhancements**
  - Updated `StudentService` to detect duplicate students using **name + email** as the uniqueness key during import, skipping duplicates and reporting them in the summary.
  - Strengthened `ValidationUtils` with Ghana-specific phone rules (`0XXXXXXXXX` and `+233XXXXXXXXX`), clear error messages, and precompiled regex patterns for all field types.
  - Centralized import/export logic in `GradeImportExportService`, ensuring all CSV/JSON/binary/PDF/Excel operations use consistent validation and error handling.

- **User Experience Improvements**
  - Main menu now displays live background-task status (e.g., active batch exports and stats updates) via `MenuService` integration with `BatchReportTaskManager` and `StatisticsDashboard`.
  - Added clearer empty-state messages (e.g., “No students found”), improved summaries after imports/exports, and more structured console output with box-drawing characters.

- **Documentation & Maintainability**
  - Shortened and standardized Javadoc across services, focusing on concise, professional explanations of intent and behavior.
  - Added implementation-oriented documentation and a time-complexity guide aligned with the new architecture and data structures.

## v1.0.0 - Initial Release

- Student Grade Management System created.
- Add students (Regular and Honors).
- Record grades for core and elective subjects.
- View all students and detailed grade reports.
- Differentiated passing criteria for student types.
- Sample data initialization.
- Statistics and analytics (grade distribution, GPA).
- Bulk import grades from CSV.
- Search and filter students by name, type, or grade range.
- Exception handling for invalid input and duplicates.
- Comprehensive JUnit tests for all major services.
- Achieved 80%+ code coverage.
- HTML and text-based test reports generated.
