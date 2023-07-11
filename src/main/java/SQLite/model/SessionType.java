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

    public static SessionType findByName(String name) {
        for (SessionType sessionType : SessionType.values()) {
            if (sessionType.getName().equals(name)) {
                return sessionType;
            }
        }
        throw new IllegalArgumentException("No enum constant with name: " + name);
    }

    public static SessionType findById(int id) {
        for (SessionType sessionType : SessionType.values()) {
            if (sessionType.getId() == id) {
                return sessionType;
            }
        }
        throw new IllegalArgumentException("No enum constant with id: " + id);
    }
}
