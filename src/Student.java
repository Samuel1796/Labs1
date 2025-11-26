public class Student {
    protected String studentID;
    protected String name;
    protected int age;
    protected String email;
    protected String phone;
    protected String status;

    protected double[] grades;
    protected int gradeCount;

    private static int studentCounter = 0;


    public Student(String name, int age, String email, String phone){
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;

        studentCounter++;
        this.studentID = String.format("STU.00%d",studentCounter);
        this.status = "Active";

//        INITIALIZE GRADE ARRAYS
        this.grades = new double[10];
        this.gradeCount = 0;
    }


//    GRADE MANAGEMENT
    public void addGrade(double grade){
     if (grade < 0 || grade>10){
         System.out.println("Invalid grade, Must be between 0 and 10");
         return;
     }
     if(gradeCount<grades.length){
         grades[gradeCount] =  grade;
         gradeCount++;
         System.out.println("Grade added successfully");
     }else {
         System.out.println("Cannot add more grades, limit reached");
     }
    }

    public double calculateAverage(){
        if (gradeCount == 0){
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < gradeCount; i++) {
            sum += grades[i];
        }
        return sum/gradeCount;
    }

    public boolean isPassing() {
        return calculateAverage() >= 5.0;
    }

    public int getGradeCount() {
        return gradeCount;
    }


//    GETTERS
    public String getStudentID(){
        return studentID;
    }
    public String getName(){
        return name;
    }
    public String getEmail(){
        return email;
    }
    public String getPhone(){
        return phone;
    }
    public String getStatus(){
        return status;
    }

//    SETTERS
public void setName(String name) {
    this.name = name;
}

    public void setAge(int age) {
        this.age = age;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    class RegularStudent extends Student{
        public RegularStudent(String name, int age, String email, String phone){
            super(name, age, email, phone);
        }
    }


}
