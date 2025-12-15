package test;

import exceptions.AppExceptions;
import exceptions.InvalidGradeException;
import models.Grade;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.GradeService;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for GradeService.
 * Verifies grade recording, updating, and duplicate detection.
 */

public class GradeServiceTest {

    //    Initializes gradeservice, student object and other objects for testing
    GradeService gradeService;
    Student student;
    Grade grade;


    /**
     * Sets up GradeService and a sample student before each test.
     */

    @BeforeEach
    public void setUp() {
        gradeService = new GradeService(10);
        student = new Student("Samuel B", 21, "das@gmail.com", "0557272539", 50, false);
    }


    @Test
    public void testRecordGrade() {
        grade = new Grade("GRD001", "STU001", "English", "Core Subject", 89, new Date());
        assertTrue(gradeService.recordGrade(grade));
        assertEquals(1, gradeService.getGradeCount());
    }

    @Test
    void testGradeDatabaseFull(){
        GradeService smallService = new GradeService(1);
        Grade grade1 = new Grade("GRD001", "STU001", "English", "Core Subject", 89, new Date());
        Grade grade2 = new Grade("GRD001", "STU001", "English", "Core Subject", 89, new Date());
        smallService.recordGrade(grade1);

        assertThrows(AppExceptions.class, () ->{
            smallService.recordGrade(grade2);
        });

    }

    @Test
    void testCountGrades(){
         grade = new Grade("GRD001", "STU001", "English", "Core Subject", 89, new Date());
        gradeService.recordGrade(grade);
        assertEquals(1, gradeService.getGradeCount());
    }

    @Test
    void testIsDuplicateGrades(){
        grade = new Grade("GRD001", "STU001", "English", "Core Subject", 89, new Date());
        gradeService.recordGrade(grade);

        assertTrue(gradeService.isDuplicateGrade("STU001", "English", "Core Subject"));
    }

    @Test
    void testUpdateGrades() {
        grade = new Grade("GRD001", "STU001", "English", "Core Subject", 89, new Date());
        gradeService.recordGrade(grade);
//      Expects 4 arguments
        gradeService.updateGrade("STU001", "English", "Core Subject", 1);
        assertEquals(1, gradeService.getGrades()[0].getValue());
    }

//EDGE CASES

    @Test
    void testRecordInvalidGradeNegative() {
        Grade invalidGrade = new Grade("GRD002", "STU001", "Math", "Core Subject", -5, new Date());
        assertThrows(InvalidGradeException.class, () -> gradeService.recordGrade(invalidGrade));
    }

    @Test
    void testRecordInvalidGradeAbove100() {
        Grade invalidGrade = new Grade("GRD003", "STU001", "Math", "Core Subject", 105, new Date());
        assertThrows(InvalidGradeException.class, () -> gradeService.recordGrade(invalidGrade));
    }






}