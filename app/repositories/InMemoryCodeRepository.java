package app.repositories;

import app.enums.CodePurpose;
import app.interfaces.CodeRepository;
import app.models.VerificationCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Volatile code storage, used by the tests. */
public final class InMemoryCodeRepository implements CodeRepository {

    private final List<VerificationCode> issued = new ArrayList<>();
    private long sequence;

    @Override
    public VerificationCode issue(String username, CodePurpose purpose, String codeHash,
            LocalDateTime expiresAt) {
        findActive(username, purpose).ifPresent(VerificationCode::markConsumed);
        VerificationCode code = new VerificationCode(++sequence, username, purpose,
                codeHash, expiresAt, 0, false);
        issued.add(code);
        return code;
    }

    @Override
    public Optional<VerificationCode> findActive(String username, CodePurpose purpose) {
        for (int index = issued.size() - 1; index >= 0; index--) {
            VerificationCode code = issued.get(index);
            if (code.getUsername().equals(username) && code.getPurpose() == purpose
                    && !code.isConsumed()) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }

    @Override
    public void consume(VerificationCode code) {
        code.markConsumed();
    }

    @Override
    public void recordAttempt(VerificationCode code) {
        code.countAttempt();
    }
}
