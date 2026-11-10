package app.repositories;

import app.interfaces.BookRepository;
import app.models.Book;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** The catalogue, stored in the books table. */
public final class MySqlBookRepository implements BookRepository {

    private static final String COLUMNS =
            "id, title, author, category, borrowed_by, borrowed_on";

    private static final String SEARCH_WHERE =
            " WHERE title LIKE ? OR author LIKE ? OR category LIKE ? OR id = ?";

    private final Database database;

    public MySqlBookRepository(Database database) {
        this.database = database;
    }

    @Override
    public Book insert(String title, String author, String category) {
        long id = database.insert(
                "INSERT INTO books (title, author, category) VALUES (?, ?, ?)",
                statement -> {
                    statement.setString(1, title);
                    statement.setString(2, author);
                    statement.setString(3, category);
                });
        return new Book((int) id, title, author, category);
    }

    @Override
    public Book save(Book book) {
        database.update("UPDATE books SET title = ?, author = ?, category = ?,"
                + " borrowed_by = ?, borrowed_on = ? WHERE id = ?", statement -> {
                    statement.setString(1, book.getTitle());
                    statement.setString(2, book.getAuthor());
                    statement.setString(3, book.getCategory());
                    statement.setString(4, book.isBorrowed() ? book.getBorrowedBy() : null);
                    statement.setDate(5, book.getBorrowedOn().map(Date::valueOf).orElse(null));
                    statement.setInt(6, book.getId());
                });
        return book;
    }

    @Override
    public Optional<Book> findById(Integer id) {
        return database.selectOne("SELECT " + COLUMNS + " FROM books WHERE id = ?",
                statement -> statement.setInt(1, id), MySqlBookRepository::read);
    }

    @Override
    public List<Book> findAll() {
        return database.selectList("SELECT " + COLUMNS + " FROM books ORDER BY id",
                Database.NOTHING, MySqlBookRepository::read);
    }

    @Override
    public boolean deleteById(Integer id) {
        return database.update("DELETE FROM books WHERE id = ?",
                statement -> statement.setInt(1, id)) > 0;
    }

    /**
     * One page of rows. LIMIT and OFFSET are what let the table stay lazy: the window
     * only ever holds the rows that have actually been scrolled to.
     */
    @Override
    public List<Book> findPage(String query, int offset, int limit) {
        if (isBlank(query)) {
            return database.selectList(
                    "SELECT " + COLUMNS + " FROM books ORDER BY id LIMIT ? OFFSET ?",
                    statement -> {
                        statement.setInt(1, limit);
                        statement.setInt(2, offset);
                    }, MySqlBookRepository::read);
        }
        return database.selectList(
                "SELECT " + COLUMNS + " FROM books" + SEARCH_WHERE
                        + " ORDER BY id LIMIT ? OFFSET ?",
                statement -> {
                    bindSearch(statement, query);
                    statement.setInt(5, limit);
                    statement.setInt(6, offset);
                }, MySqlBookRepository::read);
    }

    /** Counted in SQL, so "is there more?" never means fetching the rest. */
    @Override
    public int count(String query) {
        if (isBlank(query)) {
            return database.selectCount("SELECT COUNT(*) FROM books", Database.NOTHING);
        }
        return database.selectCount("SELECT COUNT(*) FROM books" + SEARCH_WHERE,
                statement -> bindSearch(statement, query));
    }

    @Override
    public List<Book> findBorrowedBy(String username) {
        return database.selectList("SELECT " + COLUMNS
                        + " FROM books WHERE borrowed_by = ? ORDER BY borrowed_on, id",
                statement -> statement.setString(1, username), MySqlBookRepository::read);
    }

    @Override
    public boolean existsTitleByAuthor(String title, String author) {
        return database.selectOne(
                "SELECT 1 FROM books WHERE LOWER(title) = LOWER(?) AND LOWER(author) = LOWER(?)",
                statement -> {
                    statement.setString(1, title);
                    statement.setString(2, author);
                }, rows -> true).isPresent();
    }

    @Override
    public List<String> findCategories() {
        return database.selectList("SELECT DISTINCT category FROM books ORDER BY category",
                Database.NOTHING, rows -> rows.getString(1));
    }

    /** The four placeholders every search shares, bound the same way each time. */
    private static void bindSearch(PreparedStatement statement, String query)
            throws SQLException {
        String trimmed = query.trim();
        String pattern = "%" + trimmed + "%";
        statement.setString(1, pattern);
        statement.setString(2, pattern);
        statement.setString(3, pattern);
        statement.setInt(4, trimmed.matches("\\d{1,9}") ? Integer.parseInt(trimmed) : -1);
    }

    private static boolean isBlank(String query) {
        return query == null || query.isBlank();
    }

    private static Book read(ResultSet rows) throws SQLException {
        Book book = new Book(rows.getInt("id"), rows.getString("title"),
                rows.getString("author"), rows.getString("category"));
        String borrower = rows.getString("borrowed_by");
        if (borrower != null) {
            Date borrowedOn = rows.getDate("borrowed_on");
            book.markBorrowed(borrower,
                    borrowedOn == null ? LocalDate.now() : borrowedOn.toLocalDate());
        }
        return book;
    }
}
