# Implementation Summary (v2.0)

## 1. System Overview

The Student Grade Management System is a console-based Java application for managing students, grades, analytics, and reports in a classroom setting.  
Version **2.0** restructures the codebase into clear modules, adds advanced analytics and background processing, and hardens validation, performance, and concurrency guarantees.

At a high level the system consists of:
- **Domain model** (`src/models`): `Student`, `RegularStudent`, `HonorsStudent`, `Subject` hierarchy, and `Grade`.
- **Services layer** (`src/services`): feature-oriented packages for student management, grade management, analytics, search, scheduling, audit, caching, menu, and batch reporting.
- **Utilities** (`src/utilities`): cross-cutting helpers for validation, file I/O, and ID generation.
- **Entry point** (`src/Main.java`): bootstraps services, wires dependencies, and hands off to the main menu loop.

## 2. Domain Model

- **Student / RegularStudent / HonorsStudent** (`src/models`):
  - Encapsulate identity (`studentID`, `name`, `email`, `phone`), enrollment (list of `Subject`), and grading policies.
  - Honors students enforce stricter passing thresholds while sharing core behavior with regular students.
- **Subject / CoreSubject / ElectiveSubject**:
  - Represent courses with a `subjectName` and `subjectType` (e.g., “Core Subject”, “Elective Subject”).
  - Used by `GradeService` and `StudentService` to maintain consistent enrollment when grades are recorded.
- **Grade**:
  - Immutable record of a single assessment: `gradeID`, `studentID`, `subjectName`, `subjectType`, numeric `value`, and `date`.
  - Stored centrally by `GradeService` for efficient analytics and reporting.

## 3. Core Services

### 3.1 Student Management (`services.student.StudentService`)

- Maintains all students in a `HashMap<String, Student> studentMap` keyed by normalized ID for O(1) average add/get (`addStudent`, `findStudentById`).  
- Provides high-level search and filtering APIs:
  - `searchStudentsByName` performs case-insensitive substring matching over names using Java Streams (linear in number of students).
  - `searchStudentsByGradeRange` collaborates with `GradeService` to filter by computed averages.
  - `searchStudentsByType` uses `instanceof` to separate honors and regular students.
- Enforces **data integrity** during imports:
  - `isDuplicateStudent(String name, String email)` scans `studentMap.values()` and prevents duplicate insertion by logical identity (name + email).
- Exposes `getStudents()` and `getStudentCount()` for downstream consumers (analytics, batch exports, schedulers, dashboards).

### 3.2 Grade Management (`services.file.GradeService`)

- Uses a fixed-size `Grade[] grades` array plus an integer `gradeCount` for compact grade storage and O(1) append (`recordGrade`).  
- Tracks **grade history** in a `LinkedList<Grade> gradeHistory` for O(1) add/remove at the ends and chronological iteration (`getGradeHistory`).  
- `recordGrade(Grade, StudentService)` coordinates:
  - Capacity and numeric-range validation (0–100).
  - Central array write and history append.
  - Lazy creation of `CoreSubject` / `ElectiveSubject` instances when missing.
  - Ensuring the student is enrolled in the subject exactly once.
- Reporting and analysis helpers:
  - `viewGradeReport(Student)` builds a detailed grade history view, aggregates statistics per student, and differentiates core vs. elective averages.
  - `countGradesForStudent(Student)` and `isDuplicateGrade(...)` provide integrity checks when recording or updating grades.

### 3.3 Grade Import/Export (`services.file.GradeImportExportService`)

- Centralizes all I/O related to grades and per-student reports, removing this responsibility from `GradeService`:
  - Bulk import from CSV/JSON with duplicate detection and summary reporting.
  - Export of all grades to CSV/JSON/Binary, and per-student multi-format reports (CSV, JSON, Binary, PDF, Excel).
- Coordinates with `FileIOUtils` to:
  - Ensure directories exist before writing.
  - Handle exceptions consistently and surface meaningful error messages to the UI.
- Exposes simple, cohesive APIs (`exportGradeReportMultiFormat`, `bulkImportGrades`, `exportGradesCSV`, etc.) used by menu handlers and the batch-report manager.

## 4. Analytics & Reporting

### 4.1 Statistics Service & Dashboard (`services.analytics`)

- **StatisticsService** performs core numeric analytics over the central grade array:
  - Mean, median, standard deviation, grade distribution buckets, and GPA-friendly aggregates.
  - Designed so both “View Class Statistics” and the real-time dashboard call the same logic.
- **StatisticsDashboard** (`StatisticsDashboard.java`):
  - Wraps `StatisticsService` in a **live console dashboard** that:
    - Recomputes statistics via a daemon `ScheduledExecutorService` every 5 seconds.
    - Caches results in a `ConcurrentHashMap<String,Object> statsCache` for O(1) average reads.
    - Ranks students using `TreeMap<Double, List<Student>> gpaRankings` (sorted by GPA) and displays top performers.
  - Exposes commands:
    - `[R]` – force immediate recomputation and redraw.
    - `[P]` – pause/resume background auto-refresh (`isPaused` flag).
    - `[Q]` – stop dashboard and gracefully shut down the scheduler.
  - Shows system metrics such as active threads, memory usage, cache hit-rate, calculation time, and last-update timestamps.

### 4.2 Batch Report Generation (`services.file.BatchReportTaskManager`)

- Manages **concurrent** generation of per-student reports via a fixed-size `ExecutorService`:
  - Accepts a list of `Student` objects, a `GradeImportExportService`, output directory, format selection, and thread count.
  - Submits a reporting task per student and tracks state (submitted/started/completed/failed) in thread-safe `AtomicInteger` counters.
- Export paths:
  - Single-format (CSV/JSON/Binary) per student.
  - Multi-format exports that fan out into `csv/`, `json/`, and `binary/` subdirectories.
- Reliability & observability:
  - Verifies each generated file with polling to accommodate slow I/O visibility.
  - Tracks individual report execution times (`reportTimes`) and per-student statuses (`threadStatus`).
  - Provides a real-time progress bar and periodic background-task count for a responsive CLI experience.

## 5. System Services & Infrastructure

### 5.1 Task Scheduling (`services.system.TaskScheduler`)

- Encapsulates **scheduled automated tasks** (US-6):
  - Uses a `ScheduledExecutorService` with daemon threads to run tasks on fixed intervals (hourly/daily/weekly).
  - Keeps tasks in a `ConcurrentHashMap<String, ScheduledTask>` plus a `PriorityQueue<ScheduledTask>` ordered by next execution time for fast inspection of upcoming work.
- Supported task types (`TaskType`):
  - GPA recalculation across all students.
  - Statistics cache refresh using `StatisticsService`.
  - Batch report generation (simulated integration with `BatchReportTaskManager`).
  - Data backup to `./data` with timestamped filenames.
- Persists task configuration to disk so recurring jobs survive application restarts and maintains an execution history (`TaskExecution`) for diagnostics and audit.

### 5.2 Audit Trail (`services.system.AuditTrailService`)

- Provides a **concurrent audit trail** (US-9):
  - Uses an `ExecutorService` and thread-safe collections to write audit records asynchronously without blocking user interactions.
  - Each entry records timestamp, operation, user/session context, and the calling thread’s `threadId()`.
  - Designed to support high-frequency writes from menus, import/export flows, and scheduled tasks.

### 5.3 LRU Cache (`services.system.LRUCache`)

- Implements a **Least Recently Used** cache for frequently accessed values:
  - Backed by a `ConcurrentHashMap` for O(1) average key lookup.
  - Maintains recency order via a doubly-linked list so eviction of the least-recently-used entry is O(1).
  - Tuned for concurrent read-heavy workloads such as repeated lookups in search, statistics, or menu flows.

## 6. Search & Validation

### 6.1 Advanced Pattern-Based Search (`services.search.PatternSearchService`)

- Implements US-7 with rich, regex-backed queries over the student dataset:
  - **Email domain search**: finds students by domain suffix and produces domain distribution statistics.
  - **Phone pattern search**: supports `*` and `?` wildcards over Ghana-style phone numbers.
  - **Student ID pattern search**: converts wildcard patterns to anchored regex (`^...$`) for precise matching.
  - **Name pattern search** and **custom regex** across multiple fields (`studentId`, `name`, `email`, `phone`).
- Each operation returns:
  - A list of `SearchResult` objects containing the student, matched field, matched text, and highlighted representation.
  - `SearchStatistics` (total scanned, matches, duration, pattern-complexity label).
  - Optional distribution maps (e.g., counts per email domain or phone prefix).

### 6.2 Validation Utilities (`utilities.ValidationUtils`)

- Centralizes all field-level validation using **compiled regex patterns**:
  - Student ID, email, name, date, course code, grade, and **Ghana-specific phone numbers**:
    - `0XXXXXXXXX` (local) and `+233XXXXXXXXX` (international), where `X` is a digit.
- Exposes two complementary APIs:
  - Boolean validators (`isValidStudentId`, `isValidEmail`, `isValidPhone`, etc.) for fast checks.
  - High-level helpers (`validateEmail`, `validatePhone`, etc.) that return **full error messages** with examples when invalid.
- Used extensively in menu flows (`MainMenuHandler`) to reject malformed input early and guide users with clear feedback.

## 7. Menu, UI, and Application Flow

### 7.1 Menu Services (`services.menu`)

- **MenuService** is responsible for **rendering** the main menu and metadata:
  - Accepts references to `BatchReportTaskManager` and `StatisticsDashboard` to display:
    - Background batch report status (e.g., number of active tasks).
    - Real-time statistics dashboard status (running/paused/stopped).
- **MainMenuHandler** orchestrates control flow:
  - Initializes and wires `StudentService`, `GradeService`, `GradeImportExportService`, `TaskScheduler`, `PatternSearchService`, `AuditTrailService`, and `StatisticsDashboard`.
  - Provides a numbered menu for:
    - Student CRUD and listing.
    - Grade recording and reports.
    - Bulk import/export of grades and students with duplicate detection by (name, email).
    - Viewing class statistics and launching the real-time dashboard (option 10).
    - Running advanced pattern-based searches and triggering scheduled tasks.
  - Wraps all operations in robust input validation, exception handling, and audit logging hooks.

### 7.2 Application Entry Point (`src/Main.java`)

- Creates core service instances with appropriate capacities and dependencies.
- Wires them into `MainMenuHandler` and enters the main menu loop.
- Responsible for clean shutdown of background services (scheduler, dashboard thread pool, batch-report executor, and audit writers) on application exit.

## 8. Concurrency, Performance & Reliability

- **Concurrency primitives**:
  - `ConcurrentHashMap` for shared caches and status maps (`statsCache`, `threadStatus`, `scheduledTasks`).
  - `AtomicInteger` / `AtomicLong` to track counts, timestamps, and cache hits/misses without explicit locks.
  - `ScheduledExecutorService` and `ExecutorService` for background tasks, dashboards, batch reporting, and audit logging.
- **Design patterns** in use:
  - **Producer–Consumer**: batch report generation and audit logging (work submitted via queues/executors, consumed by worker threads).
  - **Strategy / Factory**: dynamic subject creation and export format handling.
  - **Observer-style** integrations: menu reacts to background-task state via injected service references.
  - **Singleton-like compiled regex pool**: validation patterns compiled once and reused.
- **Error handling & robustness**:
  - Custom exceptions (`AppExceptions`, `InvalidGradeException`, `StudentNotFoundException`, `DuplicateStudentException`, `InvalidSubjectException`) clearly separate validation issues from system failures.
  - File operations guard against missing directories, delayed file-system visibility, and inconsistent states, logging detailed paths when problems occur.

## 9. Testing & Extensibility

- Test classes under `src/test` exercise `StudentService`, `GradeService`, and `StatisticsService`, and can be extended to cover new features (scheduler, batch reports, pattern search, cache, and audit trail).
- The modular service structure (student/file/analytics/system/search/menu) is designed to make future enhancements easy to implement and reason about, e.g.:
  - Adding new export formats without touching core grade logic.
  - Introducing additional scheduled tasks without modifying the scheduler core.
  - Swapping or extending search strategies while keeping the main menu stable.
