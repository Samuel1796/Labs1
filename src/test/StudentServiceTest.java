package test;

import exceptions.DuplicateStudentException;
import exceptions.StudentNotFoundException;
import models.HonorsStudent;
import models.RegularStudent;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.file.GradeService;
import services.student.StudentService;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StudentService (v2.0).
 * Verifies student management, search operations, duplicate detection, and basic validation.
 */
class StudentServiceTest {

    private StudentService studentService;
    private GradeService gradeService;
    private Student regularStudent;
    private Student honorsStudent;

    @BeforeEach
    void setUp() throws DuplicateStudentException {
        studentService = new StudentService();
        gradeService = new GradeService(20);

        regularStudent = new RegularStudent("John Doe", 20, "john.doe@example.com", "0241234567");
        honorsStudent  = new HonorsStudent("Jane Smith", 21, "jane.smith@example.com", "0249876543");

        studentService.addStudent(regularStudent);
        studentService.addStudent(honorsStudent);
    }

    @Test
    void testAddAndFindStudentById() throws StudentNotFoundException {
        Student found = studentService.findStudentById(regularStudent.getStudentID());
        assertSame(regularStudent, found);

        Student foundHonors = studentService.findStudentById(honorsStudent.getStudentID());
        assertSame(honorsStudent, foundHonors);
    }

    @Test
    void testFindStudentByIdThrowsWhenNotFound() {
        assertThrows(StudentNotFoundException.class,
                () -> studentService.findStudentById("STU999"));
    }

    @Test
    void testAddDuplicateStudentThrowsDuplicateStudentException() {
        assertThrows(DuplicateStudentException.class,
                () -> studentService.addStudent(regularStudent));
    }

    @Test
    void testIsDuplicateStudentByNameAndEmail() {
        assertTrue(studentService.isDuplicateStudent("John Doe", "john.doe@example.com"));
        assertFalse(studentService.isDuplicateStudent("Other Name", "other@example.com"));
    }

    @Test
    void testSearchStudentsByName_PartialAndCaseInsensitive() throws DuplicateStudentException {
        Student extra = new RegularStudent("Johnny Johnson", 19, "johnny@example.com", "0240000000");
        studentService.addStudent(extra);

        Student[] results = studentService.searchStudentsByName("john");
        assertEquals(2, results.length);
    }

    @Test
    void testSearchStudentsByTypeFiltersRegularAndHonors() {
        Student[] regulars = studentService.searchStudentsByType(false);
        Student[] honors   = studentService.searchStudentsByType(true);

        assertEquals(1, regulars.length);
        assertSame(regularStudent, regulars[0]);

        assertEquals(1, honors.length);
        assertSame(honorsStudent, honors[0]);
    }

    @Test
    void testSearchStudentsByGradeRange_UsesGradeService() throws DuplicateStudentException {
        // Record grades for both students
        gradeService.recordGrade(
                new models.Grade("GRD001", regularStudent.getStudentID(), "Math", "Core Subject", 95, new Date()),
                studentService
        );
        gradeService.recordGrade(
                new models.Grade("GRD002", honorsStudent.getStudentID(), "Math", "Core Subject", 60, new Date()),
                studentService
        );

        Student[] topPerformers = studentService.searchStudentsByGradeRange(90, 100, gradeService);
        assertEquals(1, topPerformers.length);
        assertSame(regularStudent, topPerformers[0]);
    }



    @Test
    void testFindSubjectByNameAndTypeReturnsNullWhenMissing() {
        assertNull(studentService.findSubjectByNameAndType("NonExistentSubject", "Core"));
    }
}


