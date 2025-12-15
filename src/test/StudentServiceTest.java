package test;

import exceptions.DuplicateStudentException;
import exceptions.StudentNotFoundException;
import models.HonorsStudent;
import models.RegularStudent;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.GradeService;
import services.StudentService;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for StudentService.
 * Verifies student management operations such as adding, searching, and validation.
 */

class StudentServiceTest {
    StudentService studentService;
    GradeService gradeService;

    // Sample students for testing
    Student s1  = new RegularStudent("Bankah", 22, "bankah@gmail.com", "0557272539");
    Student s2  = new RegularStudent("Anthony", 22, "anthony@gmail.com", "0557272539");

    /**
     * Initializes StudentService before each test.
     */
    @BeforeEach
    void setUp() {
        studentService = new StudentService(5);
    }

    @Test
    void testisValidEmail() {
        assertTrue(studentService.isValidEmail("mine@gmail.com"));
        assertFalse(studentService.isValidEmail("kofi is a boy"));
    }

//    @Test
//    void TestIsDuplicateStudent() {
//        studentService.addStudent(s1);
//        assertTrue(studentService.isDuplicateStudent("Bankah", "bankah@gmail.com"));
//        assertFalse(studentService.isDuplicateStudent("Yosh", "ada@gmail.com"));
//        assertThrows(DuplicateStudentException.class, ()-> {
//            studentService.isDuplicateStudent("Bankah", "bankah@gmail.com");
//        });
//
//    }

    @Test
    void TestAddStudent() {
//        Student s  = new RegularStudent("Bankah", 22, "banah@gmail.com", "0557272539");
        studentService.addStudent(s1);
        assertEquals(s1, studentService.findStudentById(s1.getStudentID()));
    }

    @Test
    void TestFindStudentById() {

        studentService.addStudent(s1);
        studentService.addStudent(s2);

        assertEquals(s1, studentService.findStudentById(s1.getStudentID()));
        assertEquals(s2, studentService.findStudentById(s2.getStudentID()));
        assertNotEquals(s1, studentService.findStudentById(s2.getStudentID()));
//        assertNull(studentService.findStudentById("STU003"));
    }

    @Test
    void testStudentNotFound() {
        assertThrows(StudentNotFoundException.class, ()->{
                studentService.findStudentById("STU003");
        });
    }

    @Test
    void searchStudentsByName() {
        studentService.addStudent(s1);
        Student[] results = studentService.searchStudentsByName("Bankah");
        assertEquals(1, results.length); // Only s1 should match
        assertEquals(s1, results[0]);    // The first (and only) result should be s1
    }


    @Test
    void searchStudentsByType() {
        Student s3  = new HonorsStudent("Ant", 22, "ant@gmail.com", "0557272539");
        studentService.addStudent(s1);
        studentService.addStudent(s2);
        studentService.addStudent(s3);

//        FOR REGULAR STUDENTS
        Student[] regulars = studentService.searchStudentsByType(false);
        assertEquals(2, regulars.length);
        assertEquals(s2, regulars[1]);

//        FOR HONORS STUDENTS
        Student[] honors = studentService.searchStudentsByType(true);
        assertEquals(1, honors.length);
        assertEquals(s3, honors[0]);
    }




//    EDGE CASES
@Test
void testAddDuplicateStudentThrowsException() {
    studentService.addStudent(s1);
    assertThrows(DuplicateStudentException.class, () -> {
        studentService.addStudent(s1);
    });
}

    @Test
    void testFindNonExistentSubjectReturnsNull() {
        Student s3 = new RegularStudent("Test", 20, "test@email.com", "1234567890");
        studentService.addStudent(s3);
        assertNull(studentService.findSubjectByNameAndType("NonExistentSubject", "Core"));
    }

}