package app.repositories;

import app.config.Config;
import app.constants.Constants;
import app.exceptions.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Owns the connection details and all JDBC boilerplate. Every statement in the
 * system goes through one of the methods below, so no repository ever repeats a
 * try-with-resources block or forgets to close a result set.
 *
 * <p>A fresh connection is taken per statement. That keeps the repositories safe to
 * call from a background thread while the window is still repainting on the Swing
 * thread, which a single shared connection would not be.
 *
 * <p>Nothing here connects until a statement actually runs, so building a Database
 * costs nothing and the program can start with MySQL switched off.
 */
public final class Database {

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database from(Config config) {
        return new Database(
                config.get("db.url",
                        "jdbc:mysql://localhost:3306/deihs_library?useSSL=false"
                                + "&allowPublicKeyRetrieval=true&serverTimezone=UTC"),
                config.get("db.user", "deihs_app"),
                config.get("db.password", ""));
    }

    /** Opens one connection to prove the settings work, with a readable failure. */
    public void verify() {
        try (Connection connection = open()) {
            if (!connection.isValid(5)) {
                throw new DataAccessException("The database did not answer in time.", null);
            }
        } catch (SQLException unreachable) {
            throw new DataAccessException(explain(unreachable), unreachable);
        }
    }

    private String explain(SQLException failure) {
        String reason = String.valueOf(failure.getMessage());
        if (reason.contains("No suitable driver")) {
            return "The MySQL driver is missing. Put mysql-connector-j-9.4.0.jar in lib/"
                    + " and include it on the classpath.";
        }
        if (reason.contains("Access denied")) {
            return "MySQL refused the username or password in " + Constants.CONFIG_FILE + ".";
        }
        if (reason.contains("Unknown database")) {
            return "The database does not exist yet. Run sql/schema.sql first.";
        }
        if (reason.contains("Communications link failure")
                || reason.contains("Connection refused")) {
            return "MySQL is not reachable. Check that the MySQL service is running.";
        }
        return "Database error: " + reason;
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Runs a query and hands the whole result set to the reader. */
    public <T> T select(String sql, Binder binder, Reader<T> reader) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                return reader.read(rows);
            }
        } catch (SQLException failure) {
            throw new DataAccessException(explain(failure), failure);
        }
    }

    /** Runs a query and maps every row. */
    public <T> List<T> selectList(String sql, Binder binder, Reader<T> rowReader) {
        return select(sql, binder, rows -> {
            List<T> mapped = new ArrayList<>();
            while (rows.next()) {
                mapped.add(rowReader.read(rows));
            }
            return mapped;
        });
    }

    /** Runs a query that returns at most one row. */
    public <T> Optional<T> selectOne(String sql, Binder binder, Reader<T> rowReader) {
        return select(sql, binder,
                rows -> rows.next() ? Optional.of(rowReader.read(rows)) : Optional.empty());
    }

    /** Runs a query that returns a single number, such as a COUNT. */
    public int selectCount(String sql, Binder binder) {
        return select(sql, binder, rows -> rows.next() ? rows.getInt(1) : 0);
    }

    /** Runs an INSERT, UPDATE or DELETE and returns the number of rows touched. */
    public int update(String sql, Binder binder) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException failure) {
            throw new DataAccessException(explain(failure), failure);
        }
    }

    /** Runs an INSERT and returns the identifier the database generated. */
    public long insert(String sql, Binder binder) {
        try (Connection connection = open();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException failure) {
            throw new DataAccessException(explain(failure), failure);
        }
    }

    /** Fills in the ? placeholders of a statement. */
    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Turns rows into objects. */
    @FunctionalInterface
    public interface Reader<T> {
        T read(ResultSet rows) throws SQLException;
    }

    /** A binder that sets nothing, for statements without placeholders. */
    public static final Binder NOTHING = statement -> { };
}
