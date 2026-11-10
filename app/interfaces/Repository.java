package app.interfaces;

import java.util.List;
import java.util.Optional;

/** The storage operations every entity needs. */
public interface Repository<T extends Identifiable<ID>, ID> {

    T save(T item);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean deleteById(ID id);
}
