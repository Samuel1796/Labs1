package services;
/**
 * Handles menu display and navigation.
 */
public class MenuService {
    public void displayMainMenu() {
        System.out.println("|===================================================|");
        System.out.println("|        STUDENT GRADE MANAGEMENT - MAIN MENU       |");
        System.out.println("|===================================================|");        System.out.println("1. Add Student");
        System.out.println("2. View Students");
        System.out.println("3. Record Grade");
        System.out.println("4. View Grade Report");
        System.out.println("5. Export Grade Report");
        System.out.println("6. Calculate Student GPA");
        System.out.println("7. Bulk Import Grades");
        System.out.println("8. View Class Statistics");
        System.out.println("9. Search Students");
        System.out.println("10. Exit");
        System.out.println();
        System.out.print("Enter choice: ");
    }
}