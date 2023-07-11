package conf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Log;
import googleCloud.CSVLogParser;
import telegram.TelegramController;
import telegraph.TelegraphController;
import telegraph.model.Page;
import telegraph.model.PageList;

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

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void start() throws IOException, SQLException {
        System.out.println("started");

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

        List<Page> newPages = getNewPages(lastSessionOrDiagnostic);

    }

    private List<Page> getNewPages(final List<String> lastSessionOrDiagnostic) {
        int pagesToLoad = 0;
        int newPagesAmount = 0;
        List<Page> newPages = new ArrayList<>();
        search: while (true) {
            pagesToLoad +=5;
            PageList pageList = telegraphController.getPageList(pagesToLoad);
            List<Page> pages = pageList.getPages();
            for (; newPagesAmount < pages.size(); newPagesAmount++) {
                Page page = pages.get(newPagesAmount);
                if (lastSessionOrDiagnostic.contains(page.getTitle())) {
                    break search;
                }
                newPages.add(page);
            }
        }
        System.out.printf("Новых статей: %d\n", newPagesAmount);
        return newPages;
    }

}
