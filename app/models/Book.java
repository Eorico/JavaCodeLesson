package app.models;

import app.constants.Constants;
import app.interfaces.Identifiable;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/** A single title in the catalogue. */
public final class Book implements Identifiable<Integer> {

    private final int id;
    private String title;
    private String author;
    private String category;
    private String borrowedBy;
    private LocalDate borrowedOn;

    public Book(int id, String title, String author, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
    }

    @Override
    public Integer id() {
        return id;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    /** A book is on loan exactly when somebody holds it. */
    public boolean isBorrowed() {
        return borrowedBy != null;
    }

    public String getBorrowedBy() {
        return borrowedBy == null ? "" : borrowedBy;
    }

    public Optional<LocalDate> getBorrowedOn() {
        return Optional.ofNullable(borrowedOn);
    }

    /** The day this copy is expected back, if it is out on loan. */
    public Optional<LocalDate> getDueDate() {
        return getBorrowedOn().map(date -> date.plusDays(Constants.LOAN_PERIOD_DAYS));
    }

    public boolean isOverdue() {
        return getDueDate().map(due -> due.isBefore(LocalDate.now())).orElse(false);
    }

    public String getStatus() {
        if (!isBorrowed()) {
            return "Available";
        }
        return isOverdue() ? "Overdue" : "Borrowed";
    }

    public boolean isBorrowedBy(User user) {
        return isBorrowed() && user != null && user.getUsername().equals(borrowedBy);
    }

    /** True when the search text appears anywhere in this record. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return String.valueOf(id).equals(needle)
                || title.toLowerCase(Locale.ROOT).contains(needle)
                || author.toLowerCase(Locale.ROOT).contains(needle)
                || category.toLowerCase(Locale.ROOT).contains(needle);
    }

    /*
     * State changes are deliberately narrow: a caller can lend or shelve a copy,
     * but cannot set the borrower and the date independently and leave them
     * disagreeing with each other.
     */

    public void markBorrowed(String username, LocalDate when) {
        this.borrowedBy = username;
        this.borrowedOn = when;
    }

    public void markReturned() {
        this.borrowedBy = null;
        this.borrowedOn = null;
    }

    public void edit(String title, String author, String category) {
        this.title = title;
        this.author = author;
        this.category = category;
    }

    @Override
    public String toString() {
        return "#" + id + " " + title + " - " + author;
    }
}
