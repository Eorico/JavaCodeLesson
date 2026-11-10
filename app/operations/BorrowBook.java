package app.operations;

import app.models.Book;
import app.models.Result;
import app.models.User;

import java.time.LocalDate;
import java.util.Optional;

/** Lends a copy to the member performing the action. */
public final class BorrowBook extends LibraryOperation {

    public BorrowBook(Book book, User actor) {
        super(book, actor);
    }

    @Override
    public Result<Book> execute() {
        Optional<Result<Book>> rejection = rejectIfSignedOut();
        if (rejection.isPresent()) {
            return rejection.get();
        }
        if (book.isBorrowed()) {
            String holder = book.isBorrowedBy(actor) ? "you" : "another member";
            return Result.fail(quotedTitle() + " is already borrowed by " + holder + ".");
        }
        book.markBorrowed(actor.getUsername(), LocalDate.now());
        return Result.ok("You borrowed " + quotedTitle() + ". Due "
                + book.getDueDate().orElseThrow() + ".", book);
    }
}
