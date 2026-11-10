package app.interfaces;

/** Turns a secret into something safe to store, and verifies it later. */
public interface PasswordEncoder {

    String encode(String raw);

    boolean matches(String raw, String encoded);
}
