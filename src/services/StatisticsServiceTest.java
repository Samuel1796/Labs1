package services;

import models.Grade;
import models.RegularStudent;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatisticsService.
 * Verifies statistical calculations on grades and students.
 */

class StatisticsServiceTest {

//    Instances of all objects needed
    GradeService gradeService;
    StatisticsService statisticsService;
    Student student;

    /**
     * Sets up a new RegularStudent and adds grades for testing.
     */
    @BeforeEach
    void setUp() {
    gradeService = new GradeService(10);
    student = new RegularStudent("Lui Kang", 21, "kang@ex.com", "0557272539");
    Grade grade1 = new Grade("GRD001", student.getStudentID(), "Mathematics", "Core Subject", 100, new Date());
    Grade grade2 = new Grade("GRD002", student.getStudentID(), "English", "Core Subject", 70, new Date());
    Grade grade3 = new Grade("GRD003", student.getStudentID(), "Science", "Core Subject", 40, new Date());

    gradeService.recordGrade(grade1);
    gradeService.recordGrade(grade2);
    gradeService.recordGrade(grade3);

    Student[] students = new Student[] { student };
    statisticsService = new StatisticsService(gradeService.getGrades(), gradeService.getGradeCount(), students, 1, gradeService);
}

    @Test
    void testCalculateMean() {
    assertEquals(70,statisticsService.calculateMean());
    }

    @Test
    void testCalculateMedian() {
    assertEquals(70, statisticsService.calculateMedian());
    }

    @Test
    void testCalculateMode() {
    assertEquals(0, statisticsService.calculateMode());
    }

    @Test
    void testCalculateStdDev() {
    assertEquals(24.49489742783178, statisticsService.calculateStdDev());
    }

    @Test
    void testCalculateRange() {
    assertEquals(60, statisticsService.calculateRange());
    }

    @Test
    void testGetHighestGrade() {
    assertEquals(100, statisticsService.getHighestGrade());
    }

    @Test
    void testGetLowestGrade() {
    assertEquals(40, statisticsService.getLowestGrade());
    }

    @Test
    void testGetSubjectAverages() {
        assertEquals(70, statisticsService.getSubjectAverages().get("English"));
        assertEquals(40, statisticsService.getSubjectAverages().get("Science"));
        assertEquals(100, statisticsService.getSubjectAverages().get("Mathematics"));

    }

    @Test
    void testGetGradeDistribution() {

        assertEquals(1, statisticsService.getGradeDistribution().get("0-59"));
        assertEquals(0, statisticsService.getGradeDistribution().get("60-69"));
        assertEquals(1, statisticsService.getGradeDistribution().get("70-79"));
        assertEquals(0, statisticsService.getGradeDistribution().get("80-89"));
        assertEquals(1, statisticsService.getGradeDistribution().get("90-100"));
    }

    @Test
    void testGetStudentTypeAverages() {
        assertEquals(70, statisticsService.getStudentTypeAverages().get("Regular Students"));
        assertEquals(0, statisticsService.getStudentTypeAverages().get("Honors Students"));

    }


//    EDGE CASES


    @Test
    void testGetSubjectAveragesWithNonExistentSubject() {
        assertNull(statisticsService.getSubjectAverages().get("NonExistentSubject"));
    }

}