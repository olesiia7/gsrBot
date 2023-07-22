package SQLite;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.Log;

import static SQLite.model.LogItemFactory.toLogItem;

@Component
public class DbController {
    private final LogsService logService;
    private final CategoriesService categoriesService;
    private final SessionTypesService sessionTypesService;
    private final LogSessionTypesService logSessionTypesService;

    public DbController(LogsService logService,
                        CategoriesService categoriesService,
                        SessionTypesService sessionTypesService,
                        LogSessionTypesService logSessionTypesService) {
        this.logService = logService;
        this.categoriesService = categoriesService;
        this.sessionTypesService = sessionTypesService;
        this.logSessionTypesService = logSessionTypesService;
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        return logService.getLogs(filter);
    }

    public int addLog(Log log) throws SQLException {
        int id = logService.addLog(toLogItem(log));
        logSessionTypesService.addLogSessionTypes(id, log.sessionTypes());
        return id;
    }

    public void createTablesIfNotExists() throws SQLException {
        categoriesService.createTableIfNotExists();
        logService.createTableIfNotExists();
        sessionTypesService.createTableIfNotExists();
        logSessionTypesService.createTableIfNotExists();
    }

    public void checkCategoriesAndSessionTypes() throws SQLException {
        categoriesService.init();
        sessionTypesService.init();
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
        logSessionTypesService.clearAllData();
        logService.clearAllData();
        categoriesService.clearAllData();
        sessionTypesService.clearAllData();
    }

    public void dropTables() {
        logSessionTypesService.dropTable();
        logService.dropTable();
        categoriesService.dropTable();
        sessionTypesService.dropTable();
    }
}
