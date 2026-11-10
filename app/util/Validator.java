package app.util;

import app.constants.Constants;
import app.exceptions.ValidationException;

import java.util.Locale;

/** Every input rule in the system lives here, so no rule is duplicated. */
public final class Validator {

    private Validator() {
    }

    /** Trims, collapses runs of spaces, strips control characters, rejects blanks. */
    public static String text(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        String cleaned = value.replaceAll("\\p{Cntrl}", " ").trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return cleaned;
    }

    public static String text(String value, String fieldName, int minimumLength) {
        String cleaned = text(value, fieldName);
        if (cleaned.length() < minimumLength) {
            throw new ValidationException(
                    fieldName + " must be at least " + minimumLength + " characters.");
        }
        return cleaned;
    }

    public static String text(String value, String fieldName, int minimum, int maximum) {
        String cleaned = text(value, fieldName, minimum);
        if (cleaned.length() > maximum) {
            throw new ValidationException(
                    fieldName + " must be " + maximum + " characters or fewer.");
        }
        return cleaned;
    }

    public static String username(String value) {
        String cleaned = text(value, "Username", 4, 32).toLowerCase(Locale.ROOT);
        if (!cleaned.matches("[a-z0-9._]+")) {
            throw new ValidationException(
                    "Username may only contain letters, numbers, dots and underscores.");
        }
        return cleaned;
    }

    public static String email(String value) {
        String cleaned = text(value, "Email address", 5, 190);
        if (!cleaned.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}")) {
            throw new ValidationException("Please enter a valid email address.");
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }

    public static String password(String value) {
        if (value == null || value.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        if (value.chars().noneMatch(Character::isLetter)
                || value.chars().noneMatch(Character::isDigit)) {
            throw new ValidationException("Password must contain both a letter and a number.");
        }
        return value;
    }

    public static void confirmed(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ValidationException("The two passwords do not match.");
        }
    }

    /** Keeps only the digits, so "482 913" and "482-913" both work. */
    public static String code(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() != Constants.CODE_LENGTH) {
            throw new ValidationException(
                    "Enter the " + Constants.CODE_LENGTH + " digit code from your email.");
        }
        return digits;
    }

    /** Hides most of an address, for example juan@mail.com becomes ju***@mail.com. */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "your email";
        }
        String local = email.substring(0, email.indexOf('@'));
        String domain = email.substring(email.indexOf('@'));
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + domain;
    }
}
