package app.models;

import app.constants.Constants;
import app.enums.CodePurpose;

import java.time.LocalDateTime;

/** A one-time number that was emailed to a member. Only its hash is kept. */
public final class VerificationCode {

    private final long id;
    private final String username;
    private final CodePurpose purpose;
    private final String codeHash;
    private final LocalDateTime expiresAt;
    private int attempts;
    private boolean consumed;

    public VerificationCode(long id, String username, CodePurpose purpose, String codeHash,
            LocalDateTime expiresAt, int attempts, boolean consumed) {
        this.id = id;
        this.username = username;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.consumed = consumed;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public CodePurpose getPurpose() {
        return purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return attempts >= Constants.MAX_CODE_ATTEMPTS;
    }

    public void countAttempt() {
        attempts++;
    }

    public void markConsumed() {
        consumed = true;
    }
}
