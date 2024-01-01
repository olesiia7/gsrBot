package bot.gsr.model;


import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.sql.Date;
import java.util.Objects;

public record Log(@NotNull Date date,
                  @NotNull String description,
                  int price,
                  @NotNull Category category,
                  @Nullable SessionType sessionType) {

    @Override
    // date не сравнивается, поэтому перевожу в LocalDate
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Log log = (Log) o;
        return price == log.price && Objects.equals(date.toLocalDate(), log.date.toLocalDate()) && Objects.equals(description, log.description) && category == log.category && sessionType == log.sessionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, description, price, category, sessionType);
    }
}