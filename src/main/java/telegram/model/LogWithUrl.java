package telegram.model;

import org.checkerframework.checker.nullness.qual.Nullable;

import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;

public record LogWithUrl(Log log, @Nullable String url) {

    public static LogWithUrl getLogWithNewPrice(LogWithUrl oldLog, int newPrice) {
        return new LogWithUrl(new Log(oldLog.log().date(), oldLog.log().description(), newPrice, oldLog.log().category(),
                oldLog.log().sessionType()), oldLog.url());
    }

    public static LogWithUrl getLogWithNewCategoryAndSessionType(LogWithUrl oldLog, Category newCategory, SessionType sessionType) {
        return new LogWithUrl(new Log(oldLog.log().date(), oldLog.log().description(), oldLog.log().price(), newCategory,
                sessionType), oldLog.url());
    }
}
