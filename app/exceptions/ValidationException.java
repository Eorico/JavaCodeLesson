package app.exceptions;

/** Thrown when user input is unusable; always translated into a failed Result. */
public final class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
