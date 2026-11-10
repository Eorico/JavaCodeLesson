package app.enums;

import java.util.Locale;

/** Who a user is allowed to be. */
public enum Role {
    MEMBER("Member"),
    LIBRARIAN("Librarian");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads the value stored in the database, defaulting to the safer role. */
    public static Role from(String raw) {
        try {
            return Role.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return MEMBER;
        }
    }
}
