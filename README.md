# Student Grade Management System (v2.0)

## 1. Overview

The Student Grade Management System is a console-based Java application for managing students, grades, analytics, and batch reports in a classroom or training environment.  
Version **2.0** introduces a modular architecture, real-time analytics, scheduled background tasks, advanced pattern-based search, an LRU cache, and a concurrent audit trail.

## 2. Feature Highlights

- **Student & Grade Management**
  - Add, list, and search students (Regular and Honors) with different passing criteria.
  - Record grades for core and elective subjects, automatically managing subject enrollment.
  - View detailed per-student grade reports, including averages and core vs. elective performance.

- **Analytics & Dashboards**
  - View class-wide statistics (mean, median, standard deviation, grade distribution, GPA-style summaries).
  - Launch a **Real-Time Statistics Dashboard** with a background daemon thread that auto-refreshes every 5 seconds while still supporting manual refresh and pause/resume.

- **Import, Export & Batch Reporting**
  - Bulk import students/grades from CSV/JSON with duplicate detection by (name, email).
  - Export grades and per-student reports in CSV, JSON, Binary, and extended formats (PDF/Excel via `GradeImportExportService`).
  - Generate per-student reports concurrently using `BatchReportTaskManager` with a live progress bar and performance summary.

- **Search, Validation & Caching**
  - Advanced pattern-based search over student ID, name, email, and phone (wildcards and full regex supported).
  - Comprehensive regex validation for all key fields, including Ghana-specific phone number rules.
  - LRU cache for repeated lookups, implemented with `ConcurrentHashMap` and a doubly-linked list.

- **Background Tasks & Audit Trail**
  - `TaskScheduler` for recurring jobs (GPA recomputation, stats refresh, backups, simulated batch report runs).
  - Non-blocking audit trail service that logs actions and events using a background `ExecutorService`.
  - Main menu displays live background-task status (e.g., active batch reports and dashboard state).

See [CHANGELOG.md](CHANGELOG.md) for a detailed history of features and changes.

## 3. Project Structure

- `src/Main.java` – Application entry point and wiring of services.
- `src/models/` – Core domain classes: `Student`, `RegularStudent`, `HonorsStudent`, `Subject` hierarchy, `Grade`.
- `src/services/student/` – `StudentService` for managing students and student-level queries.
- `src/services/file/` – `GradeService`, `GradeImportExportService`, and `BatchReportTaskManager` for grade storage and I/O.
- `src/services/analytics/` – `StatisticsService` and `StatisticsDashboard` for analytics and real-time statistics.
- `src/services/search/` – `PatternSearchService` for advanced pattern-based search (US-7).
- `src/services/system/` – `TaskScheduler`, `AuditTrailService`, and `LRUCache` (US-6, US-9, caching).
- `src/services/menu/` – `MainMenuHandler` and `MenuService` for console menu rendering and navigation.
- `src/utilities/` – `ValidationUtils`, `FileIOUtils`, `StudentIdGenerator` and other shared utilities.
- `src/test/` – Unit tests for core services (where present).

## 4. Setup & Running

### 4.1 Prerequisites

- Java Development Kit (JDK 11 or later) installed and on your `PATH`.
- Any Java-capable IDE (IntelliJ IDEA recommended) or a terminal with `javac`/`java` available.

### 4.2 Build & Run from the Command Line

From the project root:

```bash
# Compile all sources into the 'out' directory
javac -classpath . -d out $(find src -name "*.java")

# On Windows PowerShell, the equivalent is roughly:
# javac -classpath . -d out (Get-ChildItem -Recurse -Filter *.java).FullName

# Run the application
cd out
java Main
```

If `java` and `javac` are on your `PATH`, you will see the main menu in the console.

### 4.3 Running from IntelliJ IDEA

1. Open the project folder in IntelliJ IDEA.
2. Ensure a Java SDK is configured for the project.
3. Build the project via **Build ▸ Build Project**.
4. Right-click `src/Main.java` and select **Run 'Main'**.

## 5. Using the Application

When you start the application you will see a numbered main menu similar to:

- Manage students (add, list, search).
- Record and view grades.
- Import/export data (grades and students).
- View class statistics.
- Launch the Real-Time Statistics Dashboard.
- Run advanced pattern-based searches.
- Trigger or inspect background tasks (scheduler, batch reporting).

Each menu option will guide you through the necessary input (validated by `ValidationUtils`) and print clear summaries or error messages. Some operations (batch reports, scheduler, dashboard, audit logging) run work in the background and expose their status either in the dashboard or directly on the main menu.

## 6. Testing

- Where present, unit tests live under `src/test/` and can be run from your IDE’s test runner.
- For JUnit-based tests, configure your preferred test framework and run the test classes (`StudentServiceTest`, `GradeServiceTest`, `StatisticsServiceTest`, `IntegrationTest`, etc.).
- See `TEST_EXECUTION_GUIDE.md` for additional instructions if you maintain a dedicated test setup.

## 7. Additional Documentation

- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) – High-level architecture and implementation overview for v2.0.
- [CHANGELOG.md](CHANGELOG.md) – Feature and update history.
- `TIME_COMPLEXITY.md` – Time/space complexity notes with references to concrete code paths (v2.0+).
- [TEST_EXECUTION_GUIDE.md](TEST_EXECUTION_GUIDE.md) – How to run and interpret tests.
- [GIT_WORKFLOW.md](GIT_WORKFLOW.md) – Git workflow and best practices.