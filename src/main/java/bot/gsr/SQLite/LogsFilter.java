package bot.gsr.SQLite;

import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.SessionType;

import javax.validation.constraints.NotNull;
import java.sql.Date;

public class LogsFilter {
    public static final LogsFilter EMPTY = new LogsFilter(Builder.EMPTY);

    private final Integer id;
    private final Date date;
    private final String description;
    private final Category category;
    private final SessionType sessionType;

    private LogsFilter(Builder builder) {
        this.id = builder.id;
        this.date = builder.date;
        this.description = builder.description;
        this.category = builder.category;
        this.sessionType = builder.sessionType;
    }

    public Integer getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public boolean isEmpty() {
        return id == null && date == null && description == null && category == null && sessionType == null;
    }

    public static class Builder {
        static final Builder EMPTY = new Builder();

        private Integer id;
        private Date date;
        private String description;
        private Category category;
        private SessionType sessionType;

        public Builder() {
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setDate(@NotNull Date date) {
            this.date = date;
            return this;
        }

        public Builder setDescription(@NotNull String description) {
            this.description = description;
            return this;
        }

        public Builder setCategory(@NotNull Category category) {
            this.category = category;
            return this;
        }

        public Builder setSessionType(@NotNull SessionType sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        public LogsFilter build() {
            return new LogsFilter(this);
        }
    }
}
