package SQLite.model;

public enum Category {
    SESSION("Сессия", 0),
    DIAGNOSTIC("Диагностика", 1),
    GSR_PRODUCT("Продукт GSR", 2),
    EXPERT_SUPPORT("Сопровождение", 3),
    SELF_SESSION("Самосессия", 4),
    SOURCE("Подключение к истоку", 5),
    ONE_PLUS("Подписка 1+", 6),
    OTHER_EXPENSES("Сопутствующие расходы", 7);

    private final String name;
    private final int id;

    Category(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public static Category findByName(String name) {
        for (Category category : Category.values()) {
            if (category.getName().equals(name)) {
                return category;
            }
        }
        throw new IllegalArgumentException("No enum constant with name: " + name);
    }
}