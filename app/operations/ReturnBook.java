package app.operations;

import app.models.Book;
import app.models.Result;
import app.models.User;

import java.util.Optional;

/** Puts a copy back on the shelf. */
public final class ReturnBook extends LibraryOperation {

    public ReturnBook(Book book, User actor) {
        super(book, actor);
    }

    @Override
    public Result<Book> execute() {
        Optional<Result<Book>> rejection = rejectIfSignedOut();
        if (rejection.isPresent()) {
            return rejection.get();
        }
        if (!book.isBorrowed()) {
            return Result.fail(quotedTitle() + " is already on the shelf.");
        }
        if (!book.isBorrowedBy(actor) && !actor.isLibrarian()) {
            return Result.fail("Only " + book.getBorrowedBy()
                    + " or a librarian can return " + quotedTitle() + ".");
        }
        boolean wasOverdue = book.isOverdue();
        book.markReturned();
        String note = wasOverdue ? " It was overdue - please return on time next visit." : "";
        return Result.ok("You returned " + quotedTitle() + "." + note, book);
    }
}
