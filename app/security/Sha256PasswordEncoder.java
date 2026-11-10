package app.security;

import app.interfaces.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted SHA-256 hashing. The only class that ever sees a plain password. */
public final class Sha256PasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String encode(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + hash(raw, salt);
    }

    @Override
    public boolean matches(String raw, String encoded) {
        if (raw == null || encoded == null || !encoded.contains(":")) {
            return false;
        }
        String[] parts = encoded.split(":", 2);
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            return MessageDigest.isEqual(
                    hash(raw, salt).getBytes(StandardCharsets.UTF_8),
                    parts[1].getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private String hash(String raw, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(ALGORITHM + " is not available", impossible);
        }
    }
}
