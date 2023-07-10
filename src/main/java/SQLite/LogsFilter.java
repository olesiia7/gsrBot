package SQLite;

import java.sql.Date;

import javax.validation.constraints.NotNull;

import SQLite.model.Category;
import SQLite.model.SessionType;

public class LogsFilter {
    private final Date date;
    private final String description;
    private final Category category;
    private final SessionType sessionType;

    private LogsFilter(Builder builder) {
        this.date = builder.date;
        this.description = builder.description;
        this.category = builder.category;
        this.sessionType = builder.sessionType;
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

    public static class Builder {
        private Date date;
        private String description;
        private Category category;
        private SessionType sessionType;

        public Builder() {
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
