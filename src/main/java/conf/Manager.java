package conf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Log;
import googleCloud.CSVLogParser;
import telegraph.TelegraphController;

@Component
public class Manager {
    @Autowired
    private TelegraphController telegraphController;
    @Autowired
    private DbController dbController;

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void start() throws IOException, SQLException {
        System.out.println("started");
        List<Log> logs = CSVLogParser.parseLogs();

        dbController.createTablesIfNotExists();
        dbController.clearAllData();
        dbController.checkCategoriesAndSessionTypes();
        for (Log log : logs) {
            dbController.addLog(log);
        }

        List<Log> dbLogs = dbController.getLogs(LogsFilter.EMPTY);
        System.out.printf("логи: %d, логи из бд: %d\n", logs.size(), dbLogs.size());
    }

}
