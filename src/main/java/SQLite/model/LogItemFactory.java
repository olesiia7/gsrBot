package SQLite.model;

public final class LogItemFactory {

    public static LogItem toLogItem(Log log) {
        return new LogItem(log.id(), log.date(), log.description(), log.price(), log.category().getId());
    }
}
