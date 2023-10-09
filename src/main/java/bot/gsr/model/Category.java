package bot.gsr.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Category {

    SESSION("Сессия"),
    DIAGNOSTIC("Диагностика"),
    GSR_PRODUCT("Продукт GSR"),
    EXPERT_SUPPORT("Сопровождение"),
    SELF_SESSION("Самосессия"),
    SOURCE("Подключение к истоку"),
    ONE_PLUS("Подписка 1+"),
    OTHER_EXPENSES("Сопутствующие расходы"),
    PG1("ПГ1"),
    PG2("ПГ2");

    private static final Logger logger = LoggerFactory.getLogger(Category.class);
    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Category getCategory(String name) {
        for (Category category : Category.values()) {
            if (category.getName().equals(name)) {
                return category;
            }
        }
        logger.error("Нет такой категории: " + name);
        throw new IllegalArgumentException("No enum constant with name: " + name);
    }
}
