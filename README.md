# Student Grade Management System

## 1. Overview

This is a command-line based Student Grade Management System built in Java. The application provides a simple and interactive way to manage student records, including adding new students, recording their grades for various subjects, and viewing detailed academic reports. The system distinguishes between "Regular" and "Honors" students, each with different passing criteria.

## 2. Features

*   **Add Students**: Add new students to the system. You can specify whether a student is a `RegularStudent` or an `HonorsStudent`.
*   **View All Students**: Display a formatted table of all students, including their ID, name, type, average grade, status, and more.
*   **Record Grades**: Record grades for a specific student in either "Core" or "Elective" subjects.
*   **View Grade Report**: Generate a detailed grade report for an individual student, showing their grade history, overall average, and performance summary.
*   **Differentiated Student Types**:
    *   **Regular Student**: Standard passing grade of 50%.
    *   **Honors Student**: Higher passing grade of 60% and eligibility for honors recognition.
*   **Sample Data**: The system initializes with a set of sample students and grades to demonstrate its functionality immediately.

## 3. Project Structure

The project is composed of several classes that model the domain of a school's grading system:

*   `Main.java`: The main entry point and controller of the application. It handles user input, displays menus, and orchestrates the calls to other parts of the system. It also holds the in-memory data stores for students and grades.
*   `Student.java`: An abstract base class for all student types. It defines common properties like name, age, email, and methods for grade calculation.
*   `RegularStudent.java`: A concrete class that extends `Student`. It sets the passing grade to 50%.
*   `HonorsStudent.java`: A concrete class that extends `Student`. It sets a higher passing grade of 60% and includes logic for honors eligibility.
*   `Grade.java`: A data class that represents a single grade entry, containing information like the student ID, subject, grade value, and date.
*   `Subject.java`: An abstract base class for subjects, defining common properties like subject name and type.
*   `CoreSubject.java`: A subclass representing core academic subjects (e.g., Mathematics, Science).
*   `ElectiveSubject.java`: A subclass representing elective subjects (e.g., Art, Physical Education).

## 4. How to Run the Project

To compile and run this project, you will need the Java Development Kit (JDK) installed on your machine.

1.  **Navigate to the Source Directory**:
    Open a terminal or command prompt and change the directory to the `src` folder where your `.java` files are located.

    ```bash
    cd path/to/your/project/src
    ```

2.  **Compile the Java Files**:
    Compile all the `.java` source files using the `javac` command.

    ```bash
    javac *.java
    ```

3.  **Run the Application**:
    Run the compiled application by executing the `Main` class.

    ```bash
    java Main
    ```

    You will then see the main menu and can start interacting with the application.

## 5. Usage Guide

Upon running the application, you will be presented with the main menu:

