# Student Grade Management System

## 1. Overview

This is a command-line based Student Grade Management System built in Java. The application provides a simple and interactive way to manage student records, including adding new students, recording their grades for various subjects, and viewing detailed academic reports. The system distinguishes between "Regular" and "Honors" students, each with different passing criteria.

## 2. Features

See [CHANGELOG.md](CHANGELOG.md) for a full list of features and updates.

## 3. Project Structure

- `src/`: Source code and service classes.
- `out/production/Lab1/`: Compiled `.class` files for running the application.
- `htmlReport/`: HTML code coverage reports.
- `reports/`: Text-based test reports.
- See [GIT_WORKFLOW.md](GIT_WORKFLOW.md) for recommended Git practices.

## 4. Setup Instructions

### Prerequisites

- Java Development Kit (JDK) installed.
- IntelliJ IDEA (recommended) or any Java IDE.

### Running the Application (Compiled Classes)

1. Open a terminal and navigate to the compiled classes directory:
    ```bash
    cd C:\Users\SamuelOduroDuahBoaky\OneDrive - AmaliTech gGmbH\Desktop\QA\Labs1\out\production\Lab1
    ```
2. Run the application:
    ```bash
    java Main
    ```

### Running from Source (IntelliJ IDEA)

1. Open the project folder in IntelliJ IDEA.
2. Build the project: `Build > Build Project`.
3. Run: Right-click `Main.java` > **Run 'Main'**.

## 5. Testing Instructions

- All tests are located in `src/services/`.
- To run tests in IntelliJ IDEA:
    - Right-click the `src/services` folder or any test class and select **Run 'Tests'** or **Run 'Tests with Coverage'**.
    - View coverage results in the IDE or open `htmlReport/index.html` for a summary.
- To run tests from the command line (if JUnit is set up):
    ```bash
    cd C:\Users\SamuelOduroDuahBoaky\OneDrive - AmaliTech gGmbH\Desktop\QA\Labs1\src
    javac -cp .;path\to\junit-5.jar;path\to\mockito-core.jar services\*.java
    java -cp .;path\to\junit-5.jar;path\to\mockito-core.jar org.junit.runner.JUnitCore services.StudentServiceTest
    ```
- See [TEST_EXECUTION_GUIDE.md](TEST_EXECUTION_GUIDE.md) for detailed instructions.

## 6. Usage Guide

Upon running the application, you will be presented with the main menu:


## 7. Additional Documentation

- [CHANGELOG.md](CHANGELOG.md): Feature and update history.
- [TEST_EXECUTION_GUIDE.md](TEST_EXECUTION_GUIDE.md): How to run and interpret tests.
- [GIT_WORKFLOW.md](GIT_WORKFLOW.md): Git workflow and best practices.