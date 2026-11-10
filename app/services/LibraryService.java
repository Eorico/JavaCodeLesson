package app.services;

import app.constants.Constants;
import app.interfaces.BookRepository;
import app.models.Book;
import app.models.Page;
import app.models.Result;
import app.models.Statistics;
import app.models.User;
import app.operations.LibraryOperation;
import app.util.Lazy;
import app.util.Validator;

import java.util.List;
import java.util.Optional;

/**
 * The catalogue and the lending desk. Knows nothing about passwords or mail.
 *
 * <p>Two kinds of laziness live here. Rows arrive a {@link Page} at a time, so a long
 * catalogue is never pulled across the wire in one go. And the handful of values the
 * dashboard asks for repeatedly - the category list and the totals - are held in
 * {@link Lazy} holders that are dropped the moment a write makes them stale.
 */
public final class LibraryService {

    private final BookRepository books;

    private Lazy<List<String>> categories;
    private Lazy<Integer> totalCount;
    private Lazy<List<Book>> snapshot;

    public LibraryService(BookRepository books) {
        this.books = books;
        resetCaches();
    }

    /** One page of the catalogue, filtered when a search term is given. */
    public Page<Book> page(String query, int offset) {
        List<Book> rows = books.findPage(query, offset, Constants.PAGE_SIZE);
        int total = isBlank(query) ? totalCount.get() : books.count(query);
        return new Page<>(rows, offset, total);
    }

    /** Every row matching the query, filtered in SQL rather than in memory. */
    public List<Book> search(String query) {
        int total = books.count(query);
        return total == 0 ? List.of() : books.findPage(query, 0, total);
    }

    /** The whole catalogue. Used by reports; the table prefers {@link #page}. */
    public List<Book> catalogue() {
        return books.findAll();
    }

    public List<Book> borrowedBy(User user) {
        return user == null ? List.of() : books.findBorrowedBy(user.getUsername());
    }

    public Optional<Book> findById(int id) {
        return books.findById(id);
    }

    /** Loaded once and remembered, because the Add Book form asks on every refresh. */
    public List<String> categories() {
        return categories.get();
    }

    public Result<Book> add(String title, String author, String category) {
        String cleanTitle = Validator.text(title, "Title", 2, 200);
        String cleanAuthor = Validator.text(author, "Author", 2, 120);
        String cleanCategory = Validator.text(category, "Category", 2, 80);
        if (books.existsTitleByAuthor(cleanTitle, cleanAuthor)) {
            return Result.fail("That title by that author is already in the catalogue.");
        }
        Book book = books.insert(cleanTitle, cleanAuthor, cleanCategory);
        resetCaches();
        return Result.ok("\"" + cleanTitle + "\" was added to the catalogue.", book);
    }

    public Result<Book> remove(int id, User actor) {
        Optional<Book> found = books.findById(id);
        if (found.isEmpty()) {
            return Result.fail("Book #" + id + " was not found.");
        }
        if (actor == null || !actor.isLibrarian()) {
            return Result.fail("Only a librarian can remove a book.");
        }
        if (found.get().isBorrowed()) {
            return Result.fail("That book is out on loan and cannot be removed.");
        }
        books.deleteById(id);
        resetCaches();
        return Result.ok("\"" + found.get().getTitle() + "\" was removed.", found.get());
    }

    /**
     * Runs any lending action polymorphically - this method never needs to change
     * when a new {@link LibraryOperation} is introduced.
     */
    public Result<Book> apply(LibraryOperation operation) {
        Result<Book> result = operation.execute();
        if (result.isSuccess()) {
            result.getValue().ifPresent(books::save);
            resetCaches();
        }
        return result;
    }

    /**
     * The dashboard totals, computed from a snapshot that is loaded once and then
     * reused until a borrow, return or edit makes it stale.
     */
    public Statistics statistics(User user) {
        List<Book> all = snapshot.get();
        long borrowed = all.stream().filter(Book::isBorrowed).count();
        long overdue = all.stream().filter(Book::isOverdue).count();
        long mine = all.stream().filter(book -> book.isBorrowedBy(user)).count();
        return new Statistics(totalCount.get(), (int) (all.size() - borrowed),
                (int) borrowed, (int) overdue, (int) mine);
    }

    /** Called after every write, so the next read reloads instead of lying. */
    public void resetCaches() {
        categories = Lazy.of(books::findCategories);
        totalCount = Lazy.of(() -> books.count(null));
        snapshot = Lazy.of(books::findAll);
    }

    private static boolean isBlank(String query) {
        return query == null || query.isBlank();
    }
}
