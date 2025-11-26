import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static Student[] students  = new Student[50];
    private static int studentCount = 0;


    public static void main(String[] args) {
        System.out.println("|===================================================|");
        System.out.println("|        STUDENT GRADE MANAGEMENT - MAIN MENU       |");
        System.out.println("|===================================================|");

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            displayMenu();
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    addStudent(sc);
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    System.out.println("Thank you for using the system ");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice, Try again ");
            }
            System.out.println();


        }
        sc.close();

    }

    public static void displayMenu() {
        System.out.println("1. Add Student");
        System.out.println("2. View Students");
        System.out.println("3. Record Grade");
        System.out.println("4. View Grade Report");
        System.out.println("5. Exit");
    }

    public static void addStudent(Scanner sc){
        System.out.println();
        System.out.println("ADD STUDENT");
        System.out.println("_________________________");

        if (studentCount >= students.length) {
            System.out.println("ERROR: Student database is full!");
            return;
        }

        System.out.print("Enter student name: ");
        String name = sc.nextLine();

        System.out.print("Enter student age: ");
        int age = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter student mail: ");
        String email = sc.nextLine();

        System.out.print("Enter student phone: ");
        String phone = sc.nextLine();
        System.out.println();

        System.out.println("Student type: ");
        System.out.println("1. Regular Student (Passing grade: 50%)");
        System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
        System.out.print("Select type (1-2): ");
        int type = sc.nextInt();
        sc.nextLine();
        System.out.println();


        if (type == 1){

        }else if(type == 2){

        }else{
            System.out.println("Invalid type, Creating Regular Student by default");
        }


        Student newStudent;
        students[studentCount] = newStudent;
        studentCount++;


        System.out.println("Student added successfully");
        System.out.printf("Student ID: %s",newStudent.getStudentID());
        System.out.printf("Name: %s", newStudent.getName());
        System.out.println("Type: ");
        System.out.printf("Email: %s", newStudent.getEmail());
        System.out.println("Passing Grade: ");
        System.out.println("Honors Eligible: ");
        System.out.printf("Status: %s", newStudent.getStatus());







    }

}

