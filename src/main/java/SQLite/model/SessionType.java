package SQLite.model;

public enum SessionType {
    SR("Судьба Рода", 0),
    RANG("Ранг", 1),
    ALL("По всем полям", 2),
    TO_ALL("По остальным полям", 3);

    private final String name;
    private final int id;

    SessionType(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
