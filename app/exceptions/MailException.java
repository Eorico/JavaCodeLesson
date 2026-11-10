package app.exceptions;

/** Thrown when a message could not be handed to the mail server. */
public final class MailException extends RuntimeException {

    public MailException(String message, Throwable cause) {
        super(message, cause);
    }
}
