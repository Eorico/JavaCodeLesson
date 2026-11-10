package app.interfaces;

/** Anything that can be stored by a {@link Repository} must expose its key. */
public interface Identifiable<ID> {
    ID id();
}
