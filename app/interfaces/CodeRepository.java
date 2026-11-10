package app.interfaces;

import app.enums.CodePurpose;
import app.models.VerificationCode;

import java.time.LocalDateTime;
import java.util.Optional;

/** Storage for the one-time codes that are emailed out. */
public interface CodeRepository {

    VerificationCode issue(String username, CodePurpose purpose, String codeHash,
            LocalDateTime expiresAt);

    Optional<VerificationCode> findActive(String username, CodePurpose purpose);

    void consume(VerificationCode code);

    void recordAttempt(VerificationCode code);
}
