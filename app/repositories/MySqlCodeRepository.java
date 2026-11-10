package app.repositories;

import app.enums.CodePurpose;
import app.interfaces.CodeRepository;
import app.models.VerificationCode;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/** One-time codes, stored in the verification_codes table. */
public final class MySqlCodeRepository implements CodeRepository {

    private final Database database;

    public MySqlCodeRepository(Database database) {
        this.database = database;
    }

    @Override
    public VerificationCode issue(String username, CodePurpose purpose, String codeHash,
            LocalDateTime expiresAt) {
        // Only the newest code may be used, so retire anything still outstanding.
        database.update("UPDATE verification_codes SET consumed = TRUE"
                + " WHERE username = ? AND purpose = ? AND consumed = FALSE", statement -> {
                    statement.setString(1, username);
                    statement.setString(2, purpose.name());
                });
        long id = database.insert("INSERT INTO verification_codes"
                + " (username, purpose, code_hash, expires_at) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, purpose.name());
                    statement.setString(3, codeHash);
                    statement.setTimestamp(4, Timestamp.valueOf(expiresAt));
                });
        return new VerificationCode(id, username, purpose, codeHash, expiresAt, 0, false);
    }

    @Override
    public Optional<VerificationCode> findActive(String username, CodePurpose purpose) {
        return database.selectOne("SELECT id, username, purpose, code_hash, expires_at,"
                + " attempts, consumed FROM verification_codes"
                + " WHERE username = ? AND purpose = ? AND consumed = FALSE"
                + " ORDER BY id DESC LIMIT 1",
                statement -> {
                    statement.setString(1, username);
                    statement.setString(2, purpose.name());
                },
                rows -> new VerificationCode(
                        rows.getLong("id"),
                        rows.getString("username"),
                        CodePurpose.valueOf(rows.getString("purpose")),
                        rows.getString("code_hash"),
                        rows.getTimestamp("expires_at").toLocalDateTime(),
                        rows.getInt("attempts"),
                        rows.getBoolean("consumed")));
    }

    @Override
    public void consume(VerificationCode code) {
        database.update("UPDATE verification_codes SET consumed = TRUE WHERE id = ?",
                statement -> statement.setLong(1, code.getId()));
        code.markConsumed();
    }

    @Override
    public void recordAttempt(VerificationCode code) {
        database.update("UPDATE verification_codes SET attempts = attempts + 1 WHERE id = ?",
                statement -> statement.setLong(1, code.getId()));
        code.countAttempt();
    }
}
