package app;

import app.config.Config;
import app.enums.CodePurpose;
import app.enums.Role;
import app.exceptions.DataAccessException;
import app.exceptions.MailException;
import app.exceptions.ValidationException;
import app.interfaces.BookRepository;
import app.interfaces.CodeRepository;
import app.interfaces.Mailer;
import app.interfaces.PasswordEncoder;
import app.interfaces.UserRepository;
import app.mail.ConsoleMailer;
import app.mail.SmtpMailer;
import app.models.Book;
import app.models.Page;
import app.models.Result;
import app.models.Statistics;
import app.models.User;
import app.operations.BorrowBook;
import app.operations.ReturnBook;
import app.repositories.Database;
import app.repositories.MySqlBookRepository;
import app.repositories.MySqlCodeRepository;
import app.repositories.MySqlUserRepository;
import app.security.Sha256PasswordEncoder;
import app.services.AuthService;
import app.services.LibraryService;
import app.util.Lazy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * DEIHS LIBRARY SYSTEM - THE FACADE
 *
 * <p>The single door the user interface knocks on. Everything behind it now lives in
 * its own file under {@code app/}: models, interfaces, repositories, services,
 * operations, mail and security. This class only routes a request to the right
 * service and turns whatever comes back into a {@link Result} the screen can show.
 *
 * <p><b>Lazy loading.</b> Constructing a {@code Process} costs nothing. The database
 * is not contacted, no repository is built and the catalogue is not seeded until the
 * first call that genuinely needs them, which is why the sign-in window appears
 * instantly even when MySQL is slow to wake up. The work is held in a {@link Lazy}
 * holder, so it happens exactly once however many callers race for it, and a failure
 * surfaces as a readable message on screen rather than a crash at start-up.
 *
 * <p>The principles the project is built on are unchanged by the split, and are in
 * fact easier to see now that each one has its own folder:
 * <ul>
 *   <li><b>OOP</b> - {@code app.models} and {@code app.operations}.</li>
 *   <li><b>DRY</b> - one {@code Database} for all JDBC, one {@code Validator} for all
 *       input rules, one {@code Lazy} for all deferred work.</li>
 *   <li><b>SRP</b> - one folder per responsibility, one class per job.</li>
 *   <li><b>OCP</b> - a new lending action is a new class in {@code app.operations}.</li>
 *   <li><b>LSP</b> - {@code app.repositories} ships a MySQL and an in-memory version of
 *       every contract, and the tests swap one for the other.</li>
 *   <li><b>ISP</b> and <b>DIP</b> - {@code app.interfaces} holds the small contracts
 *       that every service depends on instead of a concrete class.</li>
 * </ul>
 */
public final class Process {

    private final Mailer mailer;
    private final Lazy<Core> core;
    private User currentUser;

    /** Production wiring: MySQL plus whichever mailer the configuration asks for. */
    public Process() {
        this(Config.load());
    }

    /**
     * Builds every collaborator from the configuration file, but only describes the
     * work; none of it runs until {@link #core} is first asked for a value.
     */
    public Process(Config config) {
        this.mailer = config.isMailConfigured() ? SmtpMailer.from(config) : new ConsoleMailer();
        PasswordEncoder encoder = new Sha256PasswordEncoder();
        Mailer configured = this.mailer;
        this.core = Lazy.of(() -> {
            Database database = Database.from(config);
            database.verify();
            return Core.build(
                    new MySqlUserRepository(database),
                    new MySqlBookRepository(database),
                    new MySqlCodeRepository(database),
                    encoder, configured);
        });
    }

    /**
     * Dependency injection (DIP): any repository and any mailer will do, which is how
     * the same logic runs against in-memory storage in the tests.
     */
    public Process(UserRepository users, BookRepository books, CodeRepository codes,
            PasswordEncoder encoder, Mailer mailer) {
        this.mailer = mailer;
        this.core = Lazy.of(() -> Core.build(users, books, codes, encoder, mailer));
    }

    /**
     * The assembled system. Built on first use, which is also when the starter
     * catalogue and the librarian account are created if the tables are empty.
     */
    private record Core(AuthService auth, LibraryService library, UserRepository users,
                        BookRepository books) {

        private static Core build(UserRepository users, BookRepository books,
                CodeRepository codes, PasswordEncoder encoder, Mailer mailer) {
            Core core = new Core(new AuthService(users, codes, encoder, mailer),
                    new LibraryService(books), users, books);
            core.seedIfEmpty(encoder);
            return core;
        }

        /** First run only: a librarian account and a starter shelf. */
        private void seedIfEmpty(PasswordEncoder encoder) {
            if (users.count() == 0) {
                User librarian = new User("admin", "Library Administrator",
                        "admin@deihslibrary.ph", Role.LIBRARIAN);
                librarian.setPasswordHash(encoder.encode("admin123"));
                librarian.setVerified(true);
                users.save(librarian);
            }
            if (books.count(null) == 0) {
                books.insert("Java Basics", "Juan Dela Cruz", "Programming");
                books.insert("OOP Guide", "Maria Santos", "Programming");
                books.insert("Java Programming", "Pedro Reyes", "Programming");
                books.insert("Clean Code", "Robert C. Martin", "Software Craft");
                books.insert("Design Patterns", "Erich Gamma", "Software Craft");
                books.insert("Noli Me Tangere", "Jose Rizal", "Filipino Literature");
                books.insert("El Filibusterismo", "Jose Rizal", "Filipino Literature");
                books.insert("Data Structures in Java", "Mark Allen Weiss", "Computer Science");
                books.insert("Introduction to Algorithms", "Thomas H. Cormen",
                        "Computer Science");
                books.insert("The Pragmatic Programmer", "Andrew Hunt", "Software Craft");
                library.resetCaches();
            }
        }
    }

    /** Where verification codes are going; needs no database, so it is always known. */
    public String getMailDescription() {
        return mailer.describe();
    }

    /** True once the database has actually been contacted. */
    public boolean isConnected() {
        return core.isLoaded();
    }

    /**
     * Forces the deferred set-up to happen now. The interface calls this on a
     * background thread once a member is signed in, so the first click on the
     * catalogue is not the one that pays for connecting.
     */
    public Result<String> warmUp() {
        return guarded(() -> {
            core.get();
            return Result.ok("Connected.", "connected");
        });
    }

    /* --- authentication -------------------------------------------------- */

    public Result<User> signUp(String username, String fullName, String email, String password,
            String confirmation) {
        return guarded(() -> core.get().auth()
                .register(username, fullName, email, password, confirmation));
    }

    public Result<User> verifyAccount(String username, String code) {
        return guarded(() -> core.get().auth().verify(username, code));
    }

    public Result<String> resendCode(String username, CodePurpose purpose) {
        return guarded(() -> core.get().auth().resend(username, purpose));
    }

    public Result<User> signIn(String username, String password) {
        Result<User> result =
                guarded(() -> core.get().auth().authenticate(username, password));
        result.getValue().ifPresent(user -> currentUser = user);
        return result;
    }

    public Result<String> requestPasswordReset(String username) {
        return guarded(() -> core.get().auth().requestPasswordReset(username));
    }

    public Result<User> resetPassword(String username, String code, String newPassword,
            String confirmation) {
        return guarded(() -> core.get().auth()
                .resetPassword(username, code, newPassword, confirmation));
    }

    public void signOut() {
        currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isSignedIn() {
        return currentUser != null;
    }

    public int getUserCount() {
        return core.get().auth().countUsers();
    }

    /* --- catalogue and lending ------------------------------------------- */

    /**
     * One page of the catalogue. This is what the table uses: it asks for the next
     * page only when the reader scrolls far enough to need it.
     */
    public Page<Book> getCatalogPage(String query, int offset) {
        return core.get().library().page(query, offset);
    }

    public List<Book> getCatalogue() {
        return core.get().library().catalogue();
    }

    /** Every match at once. Handy in tests; the screen prefers {@link #getCatalogPage}. */
    public List<Book> search(String query) {
        return core.get().library().search(query);
    }

    public List<Book> getMyBooks() {
        return currentUser == null ? Collections.emptyList()
                : core.get().library().borrowedBy(currentUser);
    }

    public Optional<Book> findBook(int id) {
        return core.get().library().findById(id);
    }

    public Result<Book> borrow(int bookId) {
        return withBook(bookId, book ->
                core.get().library().apply(new BorrowBook(book, currentUser)));
    }

    public Result<Book> returnBook(int bookId) {
        return withBook(bookId, book ->
                core.get().library().apply(new ReturnBook(book, currentUser)));
    }

    public Result<Book> addBook(String title, String author, String category) {
        if (!isSignedIn()) {
            return Result.fail("Please sign in first.");
        }
        return guarded(() -> core.get().library().add(title, author, category));
    }

    public Result<Book> removeBook(int bookId) {
        return guarded(() -> core.get().library().remove(bookId, currentUser));
    }

    public Statistics getStatistics() {
        return core.get().library().statistics(currentUser);
    }

    public List<String> getCategories() {
        return core.get().library().categories();
    }

    /* --- internals ------------------------------------------------------- */

    /**
     * Turns any expected failure into a friendly Result - written once (DRY). It also
     * covers the deferred set-up, so a database that is down shows up as a message on
     * the screen the first time something is clicked.
     */
    private <T> Result<T> guarded(ThrowingAction<T> action) {
        try {
            return action.run();
        } catch (ValidationException invalid) {
            return Result.fail(invalid.getMessage());
        } catch (DataAccessException databaseDown) {
            return Result.fail(databaseDown.getMessage());
        } catch (MailException undelivered) {
            return Result.fail(undelivered.getMessage());
        }
    }

    private Result<Book> withBook(int bookId, BookAction action) {
        if (!isSignedIn()) {
            return Result.fail("Please sign in first.");
        }
        return guarded(() -> {
            Optional<Book> found = core.get().library().findById(bookId);
            if (found.isEmpty()) {
                return Result.fail("Book #" + bookId + " was not found.");
            }
            return action.run(found.get());
        });
    }

    /** Small functional helpers so {@link #guarded} can wrap any call. */
    @FunctionalInterface
    private interface ThrowingAction<T> {
        Result<T> run();
    }

    @FunctionalInterface
    private interface BookAction {
        Result<Book> run(Book book);
    }
}
