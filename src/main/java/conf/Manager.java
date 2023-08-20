package conf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Log;
import events.ConvertDbToCSVEvent;
import handlers.EventManager;
import telegram.TelegramController;
import utils.CSVLogParser;

@Component
@PropertySource("classpath:application.properties")
public class Manager {
    @Value("${create.from.scratch}")
    private boolean createFromScratch;
    @Value("${convert.db.to.csv}")
    private boolean convertDbToCsv;
    @Value("${connect.to.bot}")
    private boolean connectToBot;

    @Autowired
    private DbController dbController;
    @Autowired
    private TelegramController telegramController;
    @Autowired
    private EventManager eventManager;

    public void start() throws IOException, SQLException {
        dbController.createTablesIfNotExists();

        if (createFromScratch) {
            dbController.clearAllData();
            List<Log> logs = CSVLogParser.parseLogs();
            for (Log log : logs) {
                dbController.addLog(log);
            }
            List<Log> dbLogs = dbController.getLogs(LogsFilter.EMPTY);
            System.out.printf("логи: %d, логи из бд: %d\n", logs.size(), dbLogs.size());
        }

        if (connectToBot) {
            telegramController.connectToBot();
        }

        if (convertDbToCsv) {
            eventManager.handleEvent(new ConvertDbToCSVEvent("src/main/resources/db.csv"));
        }
    }

}
