package conf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;
import events.VerifyAndPublishLogEvent;
import googleCloud.CSVLogParser;
import handlers.EventManager;
import telegram.TelegramController;
import telegram.model.LogWithUrl;
import telegraph.TelegraphController;
import telegraph.model.Page;

@Component
@PropertySource("classpath:application.properties")
public class Manager {
    @Value("${create.from.scratch}")
    private boolean createFromScratch;
    @Value("${add.new.logs}")
    private boolean addNewLogs;

    @Autowired
    private TelegraphController telegraphController;
    @Autowired
    private DbController dbController;
    @Autowired
    private TelegramController telegramController;
    @Autowired
    private EventManager eventManager;

    public void start() throws IOException, SQLException, ExecutionException, InterruptedException {
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
        telegramController.connectToBot();

        if (addNewLogs) {
            List<String> lastSessionOrDiagnostic = dbController.getLastSessionOrDiagnostic();
            System.out.printf("Последняя сессия/диагностика: %s\n", lastSessionOrDiagnostic);

            List<Page> newPages = telegraphController.getNewPages(lastSessionOrDiagnostic);
            List<LogWithUrl> logs = newPages.stream()
                    .map(this::pageToLog)
                    .sorted(Comparator.comparing(l -> l.log().date()))
                    .toList();

            for (LogWithUrl logWithUrl : logs) {
                CompletableFuture<Void> promise = new CompletableFuture<>();
                eventManager.handleEvent(new VerifyAndPublishLogEvent(logWithUrl, promise));
                promise.get(); // чтобы не посылать новые запросы, пока не принято решение по текущему
            }
        }
    }

    private LogWithUrl pageToLog(Page page) {
        String description = page.getTitle();
        int price = 2600;
        Category category = Category.SESSION;
        SessionType sessionType = SessionType.SR;
        if (description.contains("Диагностика")) {
            price = 0;
            category = Category.DIAGNOSTIC;
            sessionType = null;
        } else if (description.contains("СЧ1")) {
            price = 5000;
            sessionType = SessionType.SCH1;
        } else if (description.contains("СЧ2")) {
            price = 5000;
            sessionType = SessionType.SCH2;
        } else if (description.contains("С#") || description.contains("C#")) {
            price = 5000;
            sessionType = SessionType.STRUCTURE;
        } else if (description.contains("СЧ#1")) {
            price = 8000;
            sessionType = SessionType.STRUCTURE_SCH1;
        } else if (description.contains("СЧ#2")) {
            price = 8000;
            sessionType = SessionType.STRUCTURE_SCH2;
        }

        return new LogWithUrl(new Log(page.getCreated(), description, price, category, sessionType), page.getUrl());
    }

}
