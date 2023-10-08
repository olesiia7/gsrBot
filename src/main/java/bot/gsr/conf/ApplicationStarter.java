package bot.gsr.conf;

import bot.gsr.SQLite.DbController;
import bot.gsr.SQLite.LogsFilter;
import bot.gsr.SQLite.model.Log;
import bot.gsr.events.ConvertDbToCSVEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.telegram.TelegramController;
import bot.gsr.utils.CSVLogParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Component
@PropertySource("classpath:application.properties")
public class ApplicationStarter implements ApplicationRunner {
    @Value("${create.from.scratch}")
    private boolean createFromScratch;
    @Value("${convert.db.to.csv}")
    private boolean convertDbToCsv;
    @Value("${connect.to.bot}")
    private boolean connectToBot;

    private final DbController dbController;
    private final TelegramController telegramController;
    private final EventManager eventManager;

    public ApplicationStarter(DbController dbController, TelegramController telegramController, EventManager eventManager) {
        this.dbController = dbController;
        this.telegramController = telegramController;
        this.eventManager = eventManager;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException, IOException {
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
