package SQLite;

import java.sql.Date;
import java.util.Set;

import javax.validation.constraints.NotNull;

import SQLite.model.Category;
import SQLite.model.SessionType;

public class LogsFilter {
    public static final LogsFilter EMPTY = new LogsFilter(Builder.EMPTY);

    private final Integer id;
    private final Date date;
    private final String description;
    private final Category category;
    private final Set<SessionType> sessionTypes;

    private LogsFilter(Builder builder) {
        this.id = builder.id;
        this.date = builder.date;
        this.description = builder.description;
        this.category = builder.category;
        this.sessionTypes = builder.sessionTypes;
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

    public Set<SessionType> getSessionTypes() {
        return sessionTypes;
    }

    public boolean isEmpty() {
        return id == null && date == null && description == null && category == null && sessionTypes == null;
    }

    public static class Builder {
        static final Builder EMPTY = new Builder();

        private Integer id;
        private Date date;
        private String description;
        private Category category;
        private Set<SessionType> sessionTypes;

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

        public Builder setSessionTypes(@NotNull Set<SessionType> sessionTypes) {
            if (sessionTypes.isEmpty()) {
                return this;
            }
            this.sessionTypes = sessionTypes;
            return this;
        }

        public LogsFilter build() {
            return new LogsFilter(this);
        }
    }
}
