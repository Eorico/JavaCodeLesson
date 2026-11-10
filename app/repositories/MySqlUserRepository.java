package app.repositories;

import app.enums.Role;
import app.interfaces.UserRepository;
import app.models.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Accounts, stored in the users table. */
public final class MySqlUserRepository implements UserRepository {

    private static final String COLUMNS =
            "username, full_name, email, password_hash, role, verified";

    private final Database database;

    public MySqlUserRepository(Database database) {
        this.database = database;
    }

    @Override
    public User save(User user) {
        // The "AS incoming" row alias is the current upsert form; the older
        // VALUES(col) function has been deprecated since MySQL 8.0.20.
        database.update("""
                INSERT INTO users (username, full_name, email, password_hash, role, verified)
                VALUES (?, ?, ?, ?, ?, ?) AS incoming
                ON DUPLICATE KEY UPDATE
                    full_name = incoming.full_name,
                    email = incoming.email,
                    password_hash = incoming.password_hash,
                    role = incoming.role,
                    verified = incoming.verified
                """, statement -> {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getFullName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.setString(5, user.getRole().name());
            statement.setBoolean(6, user.isVerified());
        });
        return user;
    }

    @Override
    public Optional<User> findById(String username) {
        return database.selectOne("SELECT " + COLUMNS + " FROM users WHERE username = ?",
                statement -> statement.setString(1, username), MySqlUserRepository::read);
    }

    @Override
    public List<User> findAll() {
        return database.selectList("SELECT " + COLUMNS + " FROM users ORDER BY username",
                Database.NOTHING, MySqlUserRepository::read);
    }

    @Override
    public boolean deleteById(String username) {
        return database.update("DELETE FROM users WHERE username = ?",
                statement -> statement.setString(1, username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return database.selectOne("SELECT 1 FROM users WHERE email = ?",
                statement -> statement.setString(1, email), rows -> true).isPresent();
    }

    /** Counted in SQL, so the dashboard never pulls every account across the wire. */
    @Override
    public int count() {
        return database.selectCount("SELECT COUNT(*) FROM users", Database.NOTHING);
    }

    private static User read(ResultSet rows) throws SQLException {
        User user = new User(rows.getString("username"), rows.getString("full_name"),
                rows.getString("email"), Role.from(rows.getString("role")));
        user.setPasswordHash(rows.getString("password_hash"));
        user.setVerified(rows.getBoolean("verified"));
        return user;
    }
}
