// src/Subject.java

// Base class representing a subject with common attributes and methods
//  Serves as a foundation for core and elective subjects
package models;

public abstract class Subject {
    protected String subjectName;
    protected String subjectCode;

    public Subject(String name, String code) {
        this.subjectName = name;
        this.subjectCode = code;
    }

    public abstract void displaySubjectDetails();

//      return The type of subject (implemented by subclasses)
    public abstract String getSubjectType();

    // Getters
    public String getSubjectName() {
        return subjectName;
    }
    public String getSubjectCode() {
        return subjectCode;
    }
}
