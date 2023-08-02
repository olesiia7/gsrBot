package SQLite;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.Log;

@Component
public class DbController {
    private final LogsService logService;

    public DbController(LogsService logService) {
        this.logService = logService;
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        return logService.getLogs(filter);
    }

    public int addLog(Log log) throws SQLException {
        return logService.addLog(log);
    }

    public void createTablesIfNotExists() throws SQLException {
        logService.createTableIfNotExists();
    }

    public List<String> getLastSessionOrDiagnostic() throws SQLException {
        return logService.getLastSessionOrDiagnostic();
    }

    public List<Log> getLastRecords(int amount) throws SQLException {
        return logService.getLastRecords(amount);
    }

    public List<Log> getLastRecords(int amount, LogsFilter filter) throws SQLException {
        return logService.getLastLogs(filter, amount);
    }

    public void clearAllData() {
        logService.clearAllData();
    }

    public void dropTables() {
        logService.dropTable();
    }
}
