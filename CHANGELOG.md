# CHANGELOG

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
