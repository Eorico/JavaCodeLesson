package app.operations;

import app.models.Book;
import app.models.Result;
import app.models.User;

import java.util.Optional;

/**
 * One lending action on one book. Adding a new action (reserve, renew, archive)
 * means adding a subclass here - nothing else in the system changes.
 */
public abstract class LibraryOperation {

    protected final Book book;
    protected final User actor;

    protected LibraryOperation(Book book, User actor) {
        this.book = book;
        this.actor = actor;
    }

    /** Performs the action and reports what happened. */
    public abstract Result<Book> execute();

    /** Shared guard so no subclass repeats the sign-in check. */
    protected Optional<Result<Book>> rejectIfSignedOut() {
        if (actor == null) {
            return Optional.of(Result.fail("Please sign in first."));
        }
        return Optional.empty();
    }

    protected String quotedTitle() {
        return "\"" + book.getTitle() + "\"";
    }
}
