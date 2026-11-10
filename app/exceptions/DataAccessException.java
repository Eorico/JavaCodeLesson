package app.exceptions;

/** Thrown when the database cannot be reached or a statement fails. */
public final class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
