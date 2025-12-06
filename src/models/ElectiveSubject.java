// src/ElectiveSubject.java
// Represents an elective subject with specific attributes and methods
//  Inherits from Subject class
package models;

public class ElectiveSubject extends Subject {
    public ElectiveSubject(String name, String code) {
        super(name, code);
    }

    public String getSubjectType() {
        return "Elective Subject";
    }

    public void displaySubjectDetails() {
        System.out.println(getSubjectType());
    }
}
