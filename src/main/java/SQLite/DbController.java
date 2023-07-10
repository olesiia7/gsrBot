package SQLite;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.LogItem;

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

    public List<LogItem> getLogs() {
        return logService.getLogs();
    }

    public void createTablesIfNotExists() throws SQLException {
        categoriesService.createTableIfNotExists();
        logService.createTableIfNotExists();
        sessionTypesService.createTableIfNotExists();
        logSessionTypesService.createTableIfNotExists();
    }

    public void deleteAllData() {
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
