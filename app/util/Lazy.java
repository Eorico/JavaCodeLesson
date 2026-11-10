package app.util;

import java.util.function.Supplier;

/**
 * A value that is produced the first time it is asked for and then remembered.
 *
 * <p>This is the single piece of machinery behind the lazy loading in the system.
 * Database connections, services, catalogue pages and interface screens are all
 * wrapped in one of these, so nothing is built or fetched until something actually
 * needs it, and nothing is built twice.
 *
 * <p>The double-checked lock makes it safe to share between the Swing thread and the
 * background workers that talk to MySQL.
 */
public final class Lazy<T> implements Supplier<T> {

    private final Supplier<T> source;
    private volatile T value;

    private Lazy(Supplier<T> source) {
        this.source = source;
    }

    public static <T> Lazy<T> of(Supplier<T> source) {
        return new Lazy<>(source);
    }

    @Override
    public T get() {
        T loaded = value;
        if (loaded == null) {
            synchronized (this) {
                loaded = value;
                if (loaded == null) {
                    loaded = source.get();
                    value = loaded;
                }
            }
        }
        return loaded;
    }

    /** True once the value has actually been produced. */
    public boolean isLoaded() {
        return value != null;
    }

    /** Forgets the value so the next call rebuilds it. */
    public synchronized void invalidate() {
        value = null;
    }
}
