package SQLite.model;

import java.sql.Date;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Полная информация из всех таблиц о логе
 */
public record Log(Integer id, Date date, String description, int price, Category category,
                  Set<SessionType> sessionTypes) {

    public Log(Integer id, LogItem logItem, Set<SessionType> sessionTypes) {
        this(id, logItem.date(), logItem.description(), logItem.price(), Category.findById(logItem.category_id()), sessionTypes);
    }

    public Log(int id, Log log) {
        this(id, log.date(), log.description(), log.price(), log.category(), log.sessionTypes());
    }

    public Log(Integer id, @NotNull Date date, @NotNull String description, int price, @NotNull Category category, @Nullable Set<SessionType> sessionTypes) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.price = price;
        this.category = category;
        this.sessionTypes = sessionTypes;
    }
}
