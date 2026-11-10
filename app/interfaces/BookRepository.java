package app.interfaces;

import app.models.Book;

import java.util.List;

/** Extra lookups that only the catalogue needs. */
public interface BookRepository extends Repository<Book, Integer> {

    /** Creates a row and returns the book with the identifier the database chose. */
    Book insert(String title, String author, String category);

    /**
     * One page of the catalogue, filtered by {@code query} when it is not blank.
     * Paging is what keeps the table lazy: rows arrive only as they are scrolled to.
     */
    List<Book> findPage(String query, int offset, int limit);

    /** How many rows {@code query} would match, for deciding whether more remain. */
    int count(String query);

    List<Book> findBorrowedBy(String username);

    boolean existsTitleByAuthor(String title, String author);

    /** The distinct categories, read straight from the database. */
    List<String> findCategories();
}
