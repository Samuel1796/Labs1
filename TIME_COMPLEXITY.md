# Time & Space Complexity Guide (v2.0)

This document summarizes the time and (where relevant) space complexity of key operations in the **Student Grade Management System v2.0**, with pointers to the actual implementation in the codebase. Unless otherwise stated, complexities are worst‑case and in terms of:

- \(n\) – number of students
- \(g\) – total number of grades (or grades per student when clear from context)
- \(t\) – number of scheduled tasks

Line ranges are approximate and given in the style you requested, for example: `@StudentService.java (17–32)`.

---

## 1. Student Management (`services.student.StudentService`)

### 1.1 Add / Lookup by ID

- **Code**: `studentMap` and `addStudent` / `findStudentById`  
  - `@StudentService.java (17–32, 34–40)`
- **Data structure**: `HashMap<String, Student>` keyed by normalized student ID.
- **Complexity**:
  - `addStudent(Student)` – **O(1)** average for `containsKey` + `put`.
  - `findStudentById(String)` – **O(1)** average for `get`.
- **Notes**: HashMap buckets degrade to O(n) in the extreme collision case, but with good hashing of IDs this is effectively constant time.

### 1.2 Duplicate Detection by Name + Email

- **Code**: `isDuplicateStudent(String name, String email)`  
  - `@StudentService.java (124–135)`
- **Complexity**:
  - Iterates over all `studentMap.values()` once → **O(n)** time.
  - Constant additional space → **O(1)** space.
- **Usage**: Applied mainly during imports to skip duplicates based on logical identity (name + email).

### 1.3 Name Search (Substring, Case-Insensitive)

- **Code**: `searchStudentsByName(String namePart)`  
  - `@StudentService.java (145–185)`
- **Complexity**:
  - Single pass over all students with a constant‑time `contains` check per name.
  - **O(n)** time, **O(m)** space where \(m\) is the number of matches.
- **Notes**: Implemented via Java Streams; laziness doesn’t change asymptotic behavior but keeps code concise and readable.

### 1.4 Grade-Range Search

- **Code**: `searchStudentsByGradeRange(double min, double max, GradeService gradeService)`  
  - `@StudentService.java (188–234)`
- **Complexity**:
  - For each student, computes an average using `Student.calculateAverage(gradeService)` which scans that student’s grades.
  - Overall **O(n * g)** time where \(g\) is average number of grades per student.
  - Additional space for result array only → **O(m)** where \(m\) matches.

### 1.5 Subject Lookup by Name & Type

- **Code**: `findSubjectByNameAndType(String name, String type)`  
  - `@StudentService.java (44–105)`
- **Complexity**:
  - Nested loops over students and each student’s enrolled subjects → **O(n * s)** where \(s\) is average subjects per student.
  - **O(1)** extra space.
- **Design tradeoff**: Simple to reason about and acceptable for typical class sizes; comments in the code explicitly call out that a dedicated subject index (e.g., a `HashMap`) could reduce this to O(1) for very large datasets.

---

## 2. Grade Management (`services.file.GradeService`)

### 2.1 Recording Grades

- **Code**: `recordGrade(Grade grade, StudentService studentService)`  
  - `@GradeService.java (18–88)`
- **Data structures**:
  - `Grade[] grades` array with `gradeCount` index.
  - `LinkedList<Grade> gradeHistory` for insertion‑ordered history.
- **Complexity**:
  - Capacity check and range validation – **O(1)**.
  - Insertion into `grades[gradeCount++]` – **O(1)**.
  - `gradeHistory.addLast(grade)` – **O(1)**.
  - Subject enrollment checks over a single student’s subjects – **O(s)** where \(s\) is subjects for that student.
- **Summary**: Overall **O(s)** per recorded grade, dominated by subject-enrollment checks; grade storage itself is constant time.

### 2.2 Counting Grades per Student

- **Code**: `countGradesForStudent(Student student)`  
  - `@GradeService.java (210–224)`
- **Complexity**:
  - Iterates through the first `gradeCount` entries in the central array and compares `studentID` each time.
  - **O(g)** time in total grades; O(1) extra space.

### 2.3 Duplicate-Grade Detection

- **Code**: `isDuplicateGrade(String studentId, String subjectName, String subjectType)`  
  - `@GradeService.java (229–247)`
- **Complexity**:
  - Linear scan over grades with a few string comparisons per entry.
  - **O(g)** time, **O(1)** space.

### 2.4 Grade History Access

- **Code**: `getGradeHistory()`  
  - `@GradeService.java (113–119)`
- **Complexity**:
  - Returns an unmodifiable view of the `LinkedList` → **O(1)** time.
  - Iteration over history when used is **O(g)**.

---

## 3. Analytics & Real-Time Dashboard (`services.analytics`)

### 3.1 Statistics Calculation

- **Code**: `StatisticsDashboard.calculateAndCacheStatistics()`  
  - `@StatisticsDashboard.java (104–152)`
- **High-level behavior**:
  - Builds a fresh `StatisticsService` from the current grade array and student collection.
  - Computes mean, median, standard deviation, and grade distribution.
  - Rebuilds GPA rankings via `updateGpaRankings()` and extracts top performers.
  - Writes all metrics into `statsCache` (`ConcurrentHashMap`) and updates `lastUpdateTime`.
- **Complexity (per run)**:
  - Basic iteration over all grades for aggregates – **O(g)**.
  - Median: typically requires a sort or partial sort – **O(g log g)** worst-case.
  - Grade distribution: another pass over grades – **O(g)**.
  - GPA rankings: loops over all students and inserts into a `TreeMap<Double, List<Student>>` – **O(n log k)** where \(k\) is number of distinct GPA buckets (often small).
- **Summary**: Overall dominated by sorting for median → **O(g log g + n log k)** per calculation, invoked:
  - On each manual refresh (`R`).
  - Every 5 seconds by the background scheduler when not paused.

### 3.2 GPA Rankings

- **Code**: `updateGpaRankings()` and `getTopPerformers(int count)`  
  - `@StatisticsDashboard.java (148–159, 161–193)`
- **Complexity**:
  - `updateGpaRankings`:
    - Clears existing `TreeMap` – **O(k)**.
    - For each student, computes average and inserts into `TreeMap` – **O(n log k)**.
  - `getTopPerformers(count)`:
    - Iterates over the `TreeMap` keys from highest GPA downward until `count` performers are collected – at most **O(k + count)**.

### 3.3 Cache Access & Hit-Rate Tracking

- **Code**: `getFromCache(String key)` and read sites in `displayDashboard()`  
  - `@StatisticsDashboard.java (266–334)` and `@StatisticsDashboard.java (300–334)`
- **Complexity**:
  - Single `ConcurrentHashMap.get` plus atomic increments → **O(1)** average time per metric read.
  - Used heavily when rendering the dashboard; complexity is dominated by console formatting, not by cache access.

### 3.4 Dashboard Scheduling

- **Code**: `start()` / `stop()` and background scheduling setup  
  - `@StatisticsDashboard.java (73–96, 80–88, 86–96, 429–457)`
- **Complexity**:
  - Scheduling is performed with `ScheduledExecutorService.scheduleAtFixedRate` – **O(1)** per scheduling call.
  - Each scheduled execution runs `calculateAndCacheStatistics()` with complexity as above.

---

## 4. Pattern-Based Search (`services.search.PatternSearchService`)

For all pattern-based searches, the high-level structure is:

1. Compile or derive a `Pattern` from user input.
2. Iterate over all students.
3. For each candidate field (ID, name, email, phone), run a regex `Matcher` and, if matched, create a `SearchResult`.

### 4.1 Email Domain Search

- **Code**: `searchByEmailDomain(String domainPattern, boolean caseSensitive)`  
  - `@PatternSearchService.java (78–113)`
- **Complexity**:
  - Pattern compilation – **O(L)** in pattern length \(L\) (one-time per call).
  - For each student, run a regex match against the email string – roughly **O(len(email))** per student.
  - Overall **O(n * len(email))** time, **O(m)** space for `m` matches.

### 4.2 Phone & Student ID Pattern Search

- **Code**:
  - `searchByPhoneAreaCode(String areaCodePattern, boolean caseSensitive)` – `@PatternSearchService.java (115–148)`
  - `searchByStudentIdPattern(String idPattern, boolean caseSensitive)` – `@PatternSearchService.java (150–185)`
- **Complexity**:
  - Convert wildcard pattern to regex – **O(L)** in pattern length.
  - For each student, run `Matcher.find()` or `Matcher.matches()` against phone or ID – **O(len(field))** each.
  - Overall **O(n * len(field))** time, **O(m)** space for matches.

### 4.3 Custom Regex Search

- **Code**: `searchByCustomPattern(String customPattern, boolean caseSensitive)`  
  - `@PatternSearchService.java (219–257)`
- **Complexity**:
  - Pattern compilation – **O(L)** for the user-supplied regex.
  - For each student, test multiple fields (ID, name, email, phone) until first match – worst‑case all 4 fields checked → **O(4 * len(field)) ≈ O(len(field))**.
  - Overall **O(n * len(field))** time, **O(m)** space for matches.
- **Caution**: Complexity also depends on regex features (e.g., backtracking); pathological patterns can degrade performance, which is reported back via `SearchStatistics` (`patternComplexity` field).

---

## 5. Batch Report Generation (`services.file.BatchReportTaskManager`)

### 5.1 Core Export Loop

- **Code**: `startBatchExport()` main loop and per-student task body  
  - `@BatchReportTaskManager.java (62–171)`
- **Complexity (work)**:
  - For each of \(n\) students, a task is submitted that:
    - Computes paths and exports to CSV/JSON/Binary (and possibly all three formats).
    - Writes all grades for that student via `getStudentGrades(student)` and formatting.
  - Work per student is proportional to grades for that student, \(g_s\), so total work is **O(Σ g_s) = O(g)**.
- **Complexity (time, wall-clock)**:
  - With a fixed pool of `p` threads, idealized wall-clock time approaches **O(g / p)**, assuming balanced work and ignoring I/O contention.
  - Progress-loop overhead is small: periodic `showProgress` calls and short sleeps → approximately **O(iterations)** where iterations ≈ `totalTasks * (check frequency)`.

### 5.2 Per-Format Exports

- **Code**:
  - CSV export – `exportStudentReportCSV`  
    - `@BatchReportTaskManager.java (174–201)`
  - JSON export – `exportStudentReportJSON`  
    - `@BatchReportTaskManager.java (203–239)`
  - Binary export – `exportStudentReportBinary`  
    - `@BatchReportTaskManager.java (241–260)`
- **Complexity**:
  - Each export runs through that student’s grades once → **O(g_s)** per student and format.
  - JSON adds a serialization step via `ObjectMapper`, still linear in grade count.
  - Directory checks and file existence verification are constant-factor overhead on top of the O(g_s) writing phase.

---

## 6. Task Scheduling & System Services (`services.system`)

### 6.1 Scheduling Tasks

- **Code**: `scheduleTask(...)`  
  - `@TaskScheduler.java (141–171)`
- **Data structures**:
  - `Map<String, ScheduledTask> scheduledTasks` – `ConcurrentHashMap` for O(1) average add/get.
  - `PriorityQueue<ScheduledTask> taskQueue` – ordered by next execution time.
- **Complexity**:
  - Insertion into `scheduledTasks` – **O(1)** average.
  - `taskQueue.offer(task)` – **O(log t)** where \(t\) is number of scheduled tasks.
  - Scheduling into the executor (`scheduleAtFixedRate`) – **O(1)**.

### 6.2 Executing Tasks

- **Code**: `createTaskRunnable(ScheduledTask task)` and its body  
  - `@TaskScheduler.java (173–253)`
- **Complexity**:
  - GPA recalculation: for each student, recompute average using `gradeService` → **O(n * g)**.
  - Statistics cache refresh: reuses `StatisticsService` calculations → roughly **O(g log g + n log k)** as in the dashboard.
  - Database backup simulation: constant relative to data size in this version (writing small metadata files) → **O(1)** or **O(size of backup payload)** if extended.
  - All of this is performed asynchronously in scheduler threads, with `TaskExecution` history appended in **O(1)** per run.

---

## 7. Validation & Utilities (`utilities.ValidationUtils`)

### 7.1 Core Validation Methods

- **Code**: Boolean validators and error-producing helpers  
  - e.g. `isValidStudentId`, `isValidEmail`, `isValidPhone`, `getValidationErrorMessage`, `validateStudentId`, `validateEmail`, `validatePhone`  
  - `@ValidationUtils.java (212–261)` and surrounding methods
- **Complexity**:
  - All validators are based on precompiled regex patterns → matching is **O(L)** in the length of the input string.
  - Error message construction concatenates a small number of strings → **O(L)** where L is length of the message+input.
- **Notes**:
  - Ghana phone validation uses the pattern `^(0\\d{9}|\\+233\\d{9})$`, so matching time is linear in the length of the phone number (at most ~13 characters).

---

## 8. LRU Cache (`services.system.LRUCache`)

*(High-level description based on implementation intent; exact line numbers may vary.)*

- **Data structures**:
  - `ConcurrentHashMap<K, Node<K,V>>` for storing nodes by key.
  - Doubly-linked list (head = most recently used, tail = least recently used) for recency ordering.
- **Complexity**:
  - `get(K key)`:
    - Hash map lookup – **O(1)** average.
    - Move node to front of list – **O(1)** with direct node references.
  - `put(K key, V value)`:
    - Hash map insert/update – **O(1)** average.
    - Insert/move node in list – **O(1)**.
    - Evict least-recently-used node when over capacity – remove from tail and hash map in **O(1)**.
- **Thread safety**: Uses concurrent collections and minimal critical sections to maintain O(1) operations even under moderate contention.

---

## 9. Logging & Cache Management

### 9.1 Logger (`utilities.Logger`)

- **Code**: `Logger` class with daily file rotation and performance monitoring
  - `@Logger.java`
- **Features**:
  - Daily log file rotation (one file per day: `app-YYYY-MM-DD.log`)
  - Thread-safe asynchronous file writing using `BlockingQueue` and `ExecutorService`
  - Audit trail logging with timestamps and operation details
  - Performance monitoring with collection sizes and thread pool metrics
- **Complexity**:
  - Log entry creation: **O(1)** - adds to queue
  - File writing: **O(1)** per entry (asynchronous, batched)
  - Daily rotation: **O(1)** - checks date on each write
  - Performance logging: **O(1)** - single map operation
- **Space**: Queue size bounded by system capacity; log files grow linearly with operations

### 9.2 Cache Management (`utilities.CacheUtils`)

- **Code**: `CacheUtils` centralized cache management
  - `@CacheUtils.java`
- **Data structures**:
  - `ConcurrentHashMap<String, LRUCache<?, ?>>` for cache registry
  - Multiple `LRUCache` instances for statistics, student data, and performance metrics
- **Complexity**:
  - Cache retrieval: **O(1)** average (delegates to LRUCache)
  - Cache creation: **O(1)** - hash map insertion
  - Statistics retrieval: **O(1)** - hash map lookup
  - Cache invalidation: **O(1)** - hash map + LRU eviction
- **Notes**: Provides unified interface for managing multiple cache instances across the application

---

## 10. Practical Impact & Guidelines

- **Small–medium class sizes (tens to low hundreds of students)**:
  - All operations (search, analytics, scheduling) are comfortably fast; even O(n * g) scans are acceptable.
  - Simpler, linear algorithms (e.g., duplicate checks, subject search) favor clarity over micro-optimizations.

- **Large classes (thousands of students, tens of thousands of grades)**:
  - Analytics paths with **O(g log g)** behavior (median, full statistics refresh) become dominant.
  - Batch reporting and scheduled GPA recomputation benefit significantly from thread pools (`ExecutorService`) and from the LRU cache.
  - Consider increasing thread pool sizes and scheduler intervals, or adding additional indexes/caches if the workload grows.

- **When extending the system**:
  - Prefer operations that are **O(1)** or **O(log n)** on shared collections (`HashMap`, `ConcurrentHashMap`, `TreeMap`, `PriorityQueue`) for hot paths.
  - Use `Logger` for all logging operations to ensure consistent audit trails and performance monitoring.
  - Use `CacheUtils` for centralized cache management instead of creating ad-hoc caching solutions.
  - Reuse existing utilities (validation, file I/O, scheduler, cache, logger) instead of re-implementing ad-hoc logic in new services.


