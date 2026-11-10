package app.security;

import app.constants.Constants;

import java.security.SecureRandom;

/** Produces the unpredictable numbers that get emailed out. */
public final class CodeFactory {

    private final SecureRandom random = new SecureRandom();

    /** A zero-padded number, for example "048213". */
    public String next() {
        int bound = (int) Math.pow(10, Constants.CODE_LENGTH);
        return String.format("%0" + Constants.CODE_LENGTH + "d", random.nextInt(bound));
    }
}
