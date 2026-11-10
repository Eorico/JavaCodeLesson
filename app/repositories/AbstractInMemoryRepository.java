package app.repositories;

import app.interfaces.Identifiable;
import app.interfaces.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Shared map handling, so neither in-memory repository repeats it. */
public abstract class AbstractInMemoryRepository<T extends Identifiable<ID>, ID>
        implements Repository<T, ID> {

    protected final Map<ID, T> items = new LinkedHashMap<>();

    @Override
    public T save(T item) {
        items.put(item.id(), item);
        return item;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public boolean deleteById(ID id) {
        return items.remove(id) != null;
    }
}
