package app.models;

import java.util.List;

/**
 * One slice of a longer list, plus whether anything follows it.
 *
 * <p>This is what makes the catalogue lazy: the table holds a page at a time and
 * asks for the next one only when the reader scrolls far enough to need it.
 */
public final class Page<T> {

    private final List<T> items;
    private final int offset;
    private final int total;

    public Page(List<T> items, int offset, int total) {
        this.items = List.copyOf(items);
        this.offset = offset;
        this.total = total;
    }

    public static <T> Page<T> empty() {
        return new Page<>(List.of(), 0, 0);
    }

    public List<T> getItems() {
        return items;
    }

    /** How many rows came before this page. */
    public int getOffset() {
        return offset;
    }

    /** How many rows match in total, loaded or not. */
    public int getTotal() {
        return total;
    }

    public int getLoadedCount() {
        return offset + items.size();
    }

    public boolean hasMore() {
        return getLoadedCount() < total;
    }
}
