package app.models;

import java.util.Optional;

/** The outcome of any operation: a flag, a human message and an optional payload. */
public final class Result<T> {

    private final boolean success;
    private final String message;
    private final T value;

    private Result(boolean success, String message, T value) {
        this.success = success;
        this.message = message;
        this.value = value;
    }

    public static <T> Result<T> ok(String message, T value) {
        return new Result<>(true, message, value);
    }

    public static <T> Result<T> ok(String message) {
        return new Result<>(true, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }
}
