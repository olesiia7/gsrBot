package SQLite.model;

import java.sql.Date;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Log из базы данных {@link LogsTable}
 */
public record LogItem(Integer id, Date date, String description, int price, int category_id
) {

    public LogItem(@NotNull Date date, @NotNull String description, int price, int category_id) {
        this(null, date, description, price, category_id);
    }

    public LogItem(@Nullable Integer id, @NotNull Date date, @NotNull String description, int price, int category_id) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.price = price;
        this.category_id = category_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogItem item = (LogItem) o;
        return price == item.price && category_id == item.category_id && Objects.equals(id, item.id) && Objects.equals(date, item.date) && Objects.equals(description, item.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, description, price, category_id);
    }

    @Override
    public String toString() {
        return "LogItem{" +
                "id=" + id +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category_id=" + category_id +
                '}';
    }
}
