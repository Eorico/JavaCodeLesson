package app.interfaces;

import app.models.User;

/** Extra lookups that only accounts need. */
public interface UserRepository extends Repository<User, String> {

    boolean existsByEmail(String email);

    /** Counted in SQL rather than by loading every row. */
    int count();
}
