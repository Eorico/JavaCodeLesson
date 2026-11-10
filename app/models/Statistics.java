package app.models;

/** Read-only snapshot of the catalogue, used by the dashboard tiles. */
public final class Statistics {

    private final int total;
    private final int available;
    private final int borrowed;
    private final int overdue;
    private final int mine;

    public Statistics(int total, int available, int borrowed, int overdue, int mine) {
        this.total = total;
        this.available = available;
        this.borrowed = borrowed;
        this.overdue = overdue;
        this.mine = mine;
    }

    public int getTotal() {
        return total;
    }

    public int getAvailable() {
        return available;
    }

    public int getBorrowed() {
        return borrowed;
    }

    public int getOverdue() {
        return overdue;
    }

    public int getMine() {
        return mine;
    }
}
