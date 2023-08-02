package SQLite.model;

import java.sql.Date;

import javax.validation.constraints.NotNull;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Полная информация о логе
 */
public record Log(Integer id, Date date, String description, int price, Category category,
                  SessionType sessionType) {

    public Log(int id, @NotNull Log log) {
        this(id, log.date(), log.description(), log.price(), log.category(), log.sessionType());
    }

    public Log(@NotNull Date date, @NotNull String description, int price, @NotNull Category category, @Nullable SessionType sessionType) {
        this(null, date, description, price, category, sessionType);
    }


    public Log(Integer id, @NotNull Date date, @NotNull String description, int price, @NotNull Category category, @Nullable SessionType sessionType) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.price = price;
        this.category = category;
        this.sessionType = sessionType;
    }
}
