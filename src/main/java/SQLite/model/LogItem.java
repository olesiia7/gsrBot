package SQLite.model;

import java.sql.Date;

import javax.validation.constraints.NotNull;

import org.checkerframework.checker.nullness.qual.Nullable;

public record LogItem(Date date, String description, int price, Category category,
                      SessionType sessionType) {
    public LogItem(@NotNull Date date, @NotNull String description, int price, @NotNull Category category, @Nullable SessionType sessionType) {
        this.date = date;
        this.description = description;
        this.price = price;
        this.category = category;
        this.sessionType = sessionType;
    }

    @NotNull
    public Date getDate() {
        return date;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    @NotNull
    public Category getCategory() {
        return category;
    }

    @Nullable
    public SessionType getSessionType() {
        return sessionType;
    }
}
