package app.repositories;

import app.interfaces.BookRepository;
import app.models.Book;

import java.util.List;

/** Volatile catalogue storage, used by the tests. */
public final class InMemoryBookRepository
        extends AbstractInMemoryRepository<Book, Integer> implements BookRepository {

    @Override
    public Book insert(String title, String author, String category) {
        int id = items.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        return save(new Book(id, title, author, category));
    }

    /** Pages the same way the SQL repository does, so both behave alike. */
    @Override
    public List<Book> findPage(String query, int offset, int limit) {
        return matching(query).stream().skip(Math.max(0, offset)).limit(limit).toList();
    }

    @Override
    public int count(String query) {
        return matching(query).size();
    }

    @Override
    public List<Book> findBorrowedBy(String username) {
        return items.values().stream()
                .filter(book -> username.equals(book.getBorrowedBy())).toList();
    }

    @Override
    public boolean existsTitleByAuthor(String title, String author) {
        return items.values().stream()
                .anyMatch(book -> book.getTitle().equalsIgnoreCase(title)
                        && book.getAuthor().equalsIgnoreCase(author));
    }

    @Override
    public List<String> findCategories() {
        return items.values().stream().map(Book::getCategory).distinct().sorted().toList();
    }

    private List<Book> matching(String query) {
        return items.values().stream().filter(book -> book.matches(query)).toList();
    }
}
