package app.constants;

import java.nio.file.Path;

/** Every fixed number and path in the system, named once. */
public final class Constants {

    private Constants() {
    }

    /** Where the database and mail settings are read from. */
    public static final Path CONFIG_FILE = Path.of("config", "app.properties");

    /** Days a member may keep a borrowed book. */
    public static final int LOAN_PERIOD_DAYS = 7;

    /** How long an emailed code stays usable. */
    public static final int CODE_VALID_MINUTES = 15;

    /** Wrong guesses allowed before a code is burned. */
    public static final int MAX_CODE_ATTEMPTS = 5;

    /** Length of the number emailed to the member. */
    public static final int CODE_LENGTH = 6;

    /** Rows fetched per trip to the database when the catalogue is paged in. */
    public static final int PAGE_SIZE = 25;

    /** Window width below which the interface switches to its compact layout. */
    public static final int COMPACT_WIDTH = 1000;

    /** Window width below which the sign-in artwork is dropped entirely. */
    public static final int NARROW_WIDTH = 820;
}
