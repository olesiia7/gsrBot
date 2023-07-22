package conf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;
import googleCloud.CSVLogParser;
import telegram.TelegramController;
import telegram.model.Decision;
import telegram.model.LogDecision;
import telegram.model.LogWithUrl;
import telegraph.TelegraphController;
import telegraph.model.Page;

import static telegram.TelegramUtils.formatPageMessage;

@Component
@PropertySource("classpath:application.properties")
public class Manager {
    @Value("${create.from.scratch}")
    private boolean createFromScratch;
    @Autowired
    private TelegraphController telegraphController;
    @Autowired
    private DbController dbController;
    @Autowired
    private TelegramController telegramController;

    public void start() throws IOException, SQLException, TelegramApiException, InterruptedException {
        dbController.createTablesIfNotExists();
        dbController.checkCategoriesAndSessionTypes();
        if (createFromScratch) {
            dbController.clearAllData();
            List<Log> logs = CSVLogParser.parseLogs();
            for (Log log : logs) {
                dbController.addLog(log);
            }
            List<Log> dbLogs = dbController.getLogs(LogsFilter.EMPTY);
            System.out.printf("логи: %d, логи из бд: %d\n", logs.size(), dbLogs.size());
        }

        List<String> lastSessionOrDiagnostic = dbController.getLastSessionOrDiagnostic();
        System.out.printf("Последняя сессия/диагностика: %s\n", lastSessionOrDiagnostic);

        List<Page> newPages = telegraphController.getNewPages(lastSessionOrDiagnostic);
        List<LogWithUrl> logs = newPages.stream()
                .map(this::pageToLog)
                .sorted(Comparator.comparing(LogWithUrl::date))
                .toList();

        telegramController.connectToBot();

        for (LogWithUrl log : logs) {
            verifyLog(log);
        }
    }

    private synchronized void verifyLog(LogWithUrl log) throws TelegramApiException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        telegramController.setListener(answer -> {
            LogDecision logDecision = (LogDecision) answer;
            if (logDecision.decision() == Decision.APPROVE) {
                publishLogInChannel(logDecision.log());
                System.out.println("опубликовано в канале " + logDecision.log());
                // todo: добавить запись в БД
            }
            latch.countDown();
        });

        telegramController.verifyLog(log);
        latch.await(); // ждем результата по запросу
    }

    private void publishLogInChannel(LogWithUrl log) {
        String formattedMessage = formatPageMessage(log.description(), log.date(), log.url());
        telegramController.sendMessage(formattedMessage);
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

        return new LogWithUrl(page.getCreated(), description, price, category, sessionType, page.getUrl());
    }

}