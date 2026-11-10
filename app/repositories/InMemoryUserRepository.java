package app.repositories;

import app.interfaces.UserRepository;
import app.models.User;

/** Volatile account storage, used by the tests. */
public final class InMemoryUserRepository
        extends AbstractInMemoryRepository<User, String> implements UserRepository {

    @Override
    public boolean existsByEmail(String email) {
        return items.values().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }

    @Override
    public int count() {
        return items.size();
    }
}
