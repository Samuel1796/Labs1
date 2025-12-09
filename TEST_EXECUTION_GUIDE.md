# Test Execution Guide

## Running Tests in IntelliJ IDEA

1. Open the project in IntelliJ IDEA.
2. Right-click the `src/services` directory or any test class.
3. Select **Run 'Tests'** or **Run 'Tests with Coverage'**.
4. View results in the IDE or open `htmlReport/index.html` for code coverage.

## Running Tests from the Command Line

1. Ensure JUnit and Mockito JARs are downloaded and available.
2. Compile test classes:
    ```bash
    javac -cp .;path\to\junit-5.jar;path\to\mockito-core.jar services\*.java
    ```
3. Run a test class:
    ```bash
    java -cp .;path\to\junit-5.jar;path\to\mockito-core.jar org.junit.runner.JUnitCore services.StudentServiceTest
    ```

## Viewing Reports

- **HTML Coverage Report:** Open `htmlReport/index.html` in your browser.
- **Text Reports:** See files in the `reports/` directory.

## Troubleshooting

- Ensure all dependencies (JUnit, Mockito) are added to your project.
- If tests do not run, check your classpath and JDK version.