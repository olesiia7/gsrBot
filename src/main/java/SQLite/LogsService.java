package SQLite;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.Log;
import SQLite.model.LogItem;

@Component
public class LogsService extends Service<LogsDAO> {

    public LogsService(LogsDAO dao) {
        super(dao);
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        return dao.getLogs(filter);
    }

    public int addLog(LogItem logItem) throws SQLException {
        return dao.addLog(logItem);
    }
}
