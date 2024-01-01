package bot.gsr.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SessionType {
    SR("Судьба Рода"),
    RANG("Ранг"),
    SCH1("Судьба Человечества прямая"),
    SCH2("Судьба Человечества обратная"),
    STRUCTURE("Структура"),
    STRUCTURE_SCH1("Структура в СЧ1"),
    STRUCTURE_SCH2("Структура в СЧ2"),
    RANG_SCH1("Ранг в СЧ1"),
    RANG_SCH2("Ранг в СЧ2");

    private static final Logger logger = LoggerFactory.getLogger(SessionType.class);
    private final String name;

    SessionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SessionType getSessionType(String name) {
        for (SessionType sessionType : SessionType.values()) {
            if (sessionType.getName().equals(name)) {
                return sessionType;
            }
        }
        logger.error("Нет такого типа сессии: {}", name);
        throw new IllegalArgumentException("No enum constant with name: " + name);
    }
}
