// java
package models;

public class HonorsStudent extends Student {
    public HonorsStudent(String name, int age, String email, String phone) {
        super(name, age, email, phone, 60, true);
    }

//    @Override
    public boolean isHonorsEligible(services.GradeService gradeService) {
        return calculateAverage(gradeService) >= passingGrade;
    }


}
