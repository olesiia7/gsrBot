package SQLite.model;

public enum SessionType {
    SR("Судьба Рода", 1),
    RANG("Ранг", 2),
    SCH1("Судьба Человечества прямая", 3),
    SCH2("Судьба Человечества обратная", 4),
    STRUCTURE("Структура", 5),
    STRUCTURE_SCH1("Структура в СЧ1", 6),
    STRUCTURE_SCH2("Структура в СЧ2", 7),
    RANG_SCH1("Ранг в СЧ1", 8),
    RANG_SCH2("Ранг в СЧ2", 9);

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
