package services;

/**
 * Handles menu display and navigation.
 */
public class MenuService {
    public void displayMainMenu() {
        System.out.println("+===================================================+");
        System.out.println("| STUDENT GRADE MANAGEMENT - MAIN MENU             |");
        System.out.println("|          [Advanced Edition v3.0]                 |");
        System.out.println("+===================================================+");
        System.out.println();

        System.out.println("STUDENT MANAGEMENT");
        System.out.println("1. Add Student ");
        System.out.println("2. View Students");
        System.out.println("3. Record Grade");
        System.out.println("4. View Grade Report");

        System.out.println("FILE OPERATIONS");
        System.out.println("5. Export Grade Report (CSV/JSON/Binary)");
        System.out.println("6. Import Data (Multi-format support) ");
        System.out.println("7. Bulk Import Grades");

        System.out.println("ANALYTICS & REPORTING");
        System.out.println("8. Calculate Student GPA");
        System.out.println("9. View Class Statistics");
        System.out.println("10. Real-Time Statistics Dashboard ");
        System.out.println("11. Generate Batch Reports");

        System.out.println("SEARCH & QUERY");
        System.out.println("12. Search Students (Advanced)");
        System.out.println("13. Pattern-Based Search");

        System.out.println("ADVANCED FEATURES");
        System.out.println("15. Schedule Automated Tasks");
        System.out.println("16. View System Performance");
        System.out.println("17. Cache Management");
        System.out.println("18. Audit Trail Viewer");

        System.out.println("19. Exit");

        System.out.print("Enter choice: ");
    }
}