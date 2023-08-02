package SQLite;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.Log;

@Component
public class LogsService extends Service<LogsDAO> {

    public LogsService(LogsDAO dao) {
        super(dao);
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        return dao.getLogs(filter);
    }

    public List<Log> getLastLogs(LogsFilter filter, int amount) throws SQLException {
        return dao.getLastLogs(filter, amount);
    }

    public int addLog(Log log) throws SQLException {
        return dao.addLog(log);
    }

    /**
     * Получаем названия последних (по дате) публикаций (их может быть несколько в одну дату)
     */
    public List<String> getLastSessionOrDiagnostic() throws SQLException {
        return dao.getLastSessionOrDiagnostic();
    }

    /**
     * Получаем {@code amount} последних записей
     */
    public List<Log> getLastRecords(int amount) throws SQLException {
        return dao.getLastRecords(amount);
    }
}
