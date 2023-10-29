package bot.gsr.model;

import javax.validation.constraints.NotNull;
import java.sql.Date;

public class LogFilter {
    public static final LogFilter EMPTY = new LogFilter(Builder.EMPTY);

    private final Date date;
    private final String description;
    private final Category category;
    private final SessionType sessionType;

    private LogFilter(Builder builder) {
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

    public boolean isEmpty() {
        return date == null && description == null && category == null && sessionType == null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LogFilter{");
        if (date != null) {
            sb.append("date=").append(date).append(", ");
        }
        if (description != null) {
            sb.append("description='").append(description).append("', ");
        }
        if (category != null) {
            sb.append("category=").append(category).append(", ");
        }
        if (sessionType != null) {
            sb.append("sessionType=").append(sessionType);
        }
        sb.append("}");
        return sb.toString();
    }

    public static class Builder {
        static final Builder EMPTY = new Builder();

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

        public LogFilter build() {
            return new LogFilter(this);
        }
    }
}
