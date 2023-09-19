package SQLite;

import SQLite.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import telegram.model.CategorySummary;
import telegram.model.MonthlyCategorySummary;
import telegram.model.MonthlySummary;
import telegram.model.YearMonth;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Component
public class DbController {
    private final LogsService logService;
    private final Logger logger = LoggerFactory.getLogger(DbController.class);

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

    public List<YearMonth> getAllPeriods() throws SQLException {
        return logService.getAllPeriods();
    }

    public List<MonthlyCategorySummary> getExtendedMonthlySummary(int months) {
        try {
            return logService.getExtendedMonthlySummary(months);
        } catch (SQLException e) {
            logger.error("getExtendedMonthlySummary: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<MonthlySummary> getMonthlySummary(int months) {
        try {
            return logService.getMonthlySummary(months);
        } catch (SQLException e) {
            logger.error("getMonthlySummary: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<CategorySummary> getCategorySummary(@Nullable String period) throws SQLException {
        return logService.getCategorySummary(period);
    }

    public void clearAllData() {
        logService.clearAllData();
    }

    public void dropTables() {
        logService.dropTable();
    }
}
