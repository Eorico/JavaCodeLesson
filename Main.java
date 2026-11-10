import java.util.Scanner;

//Create a java class
class Book {
    private int id;
    private String title;
    private String author;
    private boolean isBorrowed;

    String category = "General Collection";

    // DEFAULT CONSTRUCTOR
    public Book() {
        this.id = 0;
        this.title = "Untitled";
        this.author = "Unknown";
        this.isBorrowed = false;
    }

    // PARAMETERIZED CONSTRUCTOR
    public Book(int id, String title, String author) {
        // 'this' keyword links parameters to instance variables
        this.id = id;
        this.title = title;
        this.author = author;
        this.isBorrowed = false;
    }

    // GETTERS AND SETTERS
    public int getId() { 
        return id; 
    }

    public String getTitle() { 
        return title; 
    }

    public String getAuthor() {
        return author;
    }

    public boolean isBorrowed() { 
        return isBorrowed; 
    }

    // PROTECTED ACCESS MODIFIER
    protected void setBorrowed(boolean status) {
        this.isBorrowed = status;
    }
}


abstract class LibraryOperation {
    protected Book book;

    public LibraryOperation(Book book) {
        this.book = book;
    }

    abstract void operate();
}

class BorrowBook extends LibraryOperation {

    public BorrowBook(Book book) {
        super(book);
    }

    void operate() {
        if (!book.isBorrowed()) {
            book.setBorrowed(true);
            System.out.println("Success: You borrowed '" + book.getTitle() + "'.");
        } else {
            System.out.println("Success: You borrowed '" + book.getTitle() + "'.");
        }
    }
}

class ReturnBook extends LibraryOperation {
    public ReturnBook(Book book) {
        super(book);
    }

    void operate() {
        if (book.isBorrowed()) {
            book.setBorrowed(false);
            System.out.println("Success: You returned '" + book.getTitle() + "'.");
        } else {
            book.setBorrowed(true);
            System.out.println("Success: You borrowed '" + book.getTitle() + "'.");
        }
    }
}

class GetDetailBook extends LibraryOperation {
    public GetDetailBook(Book book) {
        super(book);
    }

    void operate() {
        String status;
        if (book.isBorrowed()) status = "[Borrowed]"; else status = "[Available]";
        System.out.println(
            "ID: " + book.getId() 
            + " | Title: " + book.getTitle() 
            + " | Author: " + book.getAuthor() 
            + " | Status: " + status
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // instance of book
        Book book1 = new Book(1, "Java Basics", "Juan");
        Book book2 = new Book(2, "OOP Guide", "Maria");
        Book book3 = new Book(3, "Java Programming", "Pedro");

        int choice;

        boolean system = true;

        while (system) {

            System.out.println("\n=== DEIHS LIBRARY SYSTEM ===");
            System.out.println("1. Borrow");
            System.out.println("2. Return");
            System.out.println("3. Search/Catalog");
            System.out.println("4. Upload Book");
            System.out.println("5. Quit");
            System.out.print("Enter choice: ");

            choice = scanner.nextInt();

            if (choice == 1) {

                System.out.print("Enter Book ID: ");
                int id = scanner.nextInt();

                 if (id == 1) {

                    BorrowBook operation =
                        new BorrowBook(book1);

                    operation.operate();

                } else if (id == 2) {

                    BorrowBook operation =
                        new BorrowBook(book2);

                    operation.operate();

                } else if (id == 3) {

                    BorrowBook operation =
                        new BorrowBook(book3);

                    operation.operate();

                } else {

                    System.out.println("Book not found.");
                }


            } else if (choice == 2) {

                System.out.print("Enter Book ID: ");
                int id = scanner.nextInt();

                if (id == 1) {
                    ReturnBook operation = new ReturnBook(book1);
                    operation.operate();
                } else if (id == 2) {
                    ReturnBook operation = new ReturnBook(book2);
                    operation.operate();
                } else if (id == 3) {
                    ReturnBook operation = new ReturnBook(book3);
                    operation.operate();
                } else {
                    System.out.println("Book not found.");
                }

            } else if (choice == 3) {

                System.out.print("Enter Book ID: ");
                int id = scanner.nextInt();

                if (id == 1) {
                    ReturnBook operation = new ReturnBook(book1);
                    operation.operate();
                } else if (id == 2) {
                    ReturnBook operation = new ReturnBook(book2);
                    operation.operate();
                } else if (id == 3) {
                    ReturnBook operation = new ReturnBook(book3);
                    operation.operate();
                } else {
                    System.out.println("Book not found.");
                }

            } else if (choice == 4) {

                System.out.println("Thank you for using DEIHS Library System!");
                break;

            } else {

                System.out.println("Invalid choice.");
            }
        }

        scanner.close();
        
    }
}