package models;

import java.io.Serializable;

public class ElectiveSubject implements Subject, Serializable {
    private static final long serialVersionUID = 1L;
    private String subjectName;
    private String subjectCode;

    public ElectiveSubject(String name, String code) {
        this.subjectName = name;
        this.subjectCode = code;
    }

    @Override
    public String getSubjectType() {
        return "Elective Subject";
    }

    @Override
    public void displaySubjectDetails() {
        System.out.println(getSubjectType());
    }

    @Override
    public String getSubjectName() {
        return subjectName;
    }

    @Override
    public String getSubjectCode() {
        return subjectCode;
    }
}