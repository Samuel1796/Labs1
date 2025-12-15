package test;

import exceptions.AppExceptions;
import exceptions.DuplicateStudentException;
import models.Grade;
import models.HonorsStudent;
import models.Student;
import models.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.GradeService;
import services.StudentService;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for GradeService and related student/subject interactions.
 * Uses mocks to simulate dependencies and verify integration points.
 */

class IntegrationTest {

    private GradeService gradeService;
    private StudentService mockStudentService;
    private Student mockStudent;
    private Subject mockSubject;

    /**
     * Sets up mocks and GradeService for integration testing.
     */

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(10);
        mockStudentService = mock(StudentService.class);
        mockStudent = mock(Student.class);
        mockSubject = mock(Subject.class);

        when(mockStudent.getStudentID()).thenReturn("STU001");
        when(mockStudent.getName()).thenReturn("Test Student");
        when(mockStudent.getPassingGrade()).thenReturn(50);
        when(mockStudent.calculateAverage(any(GradeService.class))).thenReturn(80.0);
        when(mockStudent.isPassing(any(GradeService.class))).thenReturn(true);

    }


    /**
     * Tests recording a grade and viewing the grade report for a mocked honors student.
     */
    @Test
    void testRecordGradeAndViewGradeReport() {
        Grade grade = new Grade("GRD001", "STU001", "Mathematics", "Core Subject", 89, new Date());
        gradeService.recordGrade(grade);

        // Should not throw any exceptions when viewing grade report
        assertDoesNotThrow(() -> gradeService.viewGradeReport(mockStudent));
        assertEquals(1, gradeService.getGradeCount());
    }

    /**
     * Tests exporting a grade report for a mocked honors student.
     */
    @Test
    void testExportGradeReportWithMockedStudent() throws IOException {
        Grade grade = new Grade("GRD002", "STU001", "Science", "Core Subject", 85, new Date());
        gradeService.recordGrade(grade);

        String filePath = gradeService.exportGradeReport(mockStudent, 3, "test_export");
        assertTrue(filePath.contains("test_export.txt"));
    }

    /**
     * Tests recording multiple grades and verifying the count for a mocked honors student.
     */
    @Test
    void testMultipleGradeRecordsWithMockedStudent() {
        Grade grade1 = new Grade("GRD011", "STU001", "Mathematics", "Core Subject", 75, new Date());
        Grade grade2 = new Grade("GRD012", "STU001", "English", "Core Subject", 85, new Date());
        gradeService.recordGrade(grade1);
        gradeService.recordGrade(grade2);

        // Should count 2 grades for the mocked student
        assertEquals(2, gradeService.countGradesForStudent(mockStudent));
    }

    /**
     * Tests subject retrieval using mocked StudentService.
     */
    @Test
    void testFindSubjectByNameAndTypeWithMock() {
        when(mockStudentService.findSubjectByNameAndType("Mathematics", "Core Subject")).thenReturn(mockSubject);

        // Should return the mocked subject
        Subject foundSubject = mockStudentService.findSubjectByNameAndType("Mathematics", "Core Subject");
        assertNotNull(foundSubject);
        assertEquals(mockSubject, foundSubject);
    }



    /**
     * Tests counting grades for a mocked honors student.
     */
    @Test
    void testCountGradesForMockedStudent() {
        Grade grade1 = new Grade("GRD005", "STU001", "Mathematics", "Core Subject", 90, new Date());
        Grade grade2 = new Grade("GRD006", "STU001", "English", "Core Subject", 85, new Date());
        gradeService.recordGrade(grade1);
        gradeService.recordGrade(grade2);

        // Should count 2 grades for the mocked student
        assertEquals(2, gradeService.countGradesForStudent(mockStudent));
    }








}