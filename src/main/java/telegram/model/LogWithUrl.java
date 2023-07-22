package telegram.model;

import java.sql.Date;

import SQLite.model.Category;
import SQLite.model.SessionType;

public record LogWithUrl(Date date, String description, int price, Category category,
                         SessionType sessionType, String url) {

    public static LogWithUrl getLogWithNewPrice(LogWithUrl oldLog, int newPrice) {
        return new LogWithUrl(oldLog.date(), oldLog.description(), newPrice, oldLog.category(),
                oldLog.sessionType(), oldLog.url());
    }

    public static LogWithUrl getLogWithNewCategoryAndSessionType(LogWithUrl oldLog, Category newCategory, SessionType sessionType) {
        return new LogWithUrl(oldLog.date(), oldLog.description(), oldLog.price(), newCategory,
                sessionType, oldLog.url());
    }
}
