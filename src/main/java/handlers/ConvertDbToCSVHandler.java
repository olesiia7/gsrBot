package handlers;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Log;
import events.ConvertDbToCSVEvent;
import utils.Utils;

/**
 * Публикует в канале статью
 */
@Component
public class ConvertDbToCSVHandler {
    private final DbController dbController;

    public ConvertDbToCSVHandler(DbController dbController) {
        this.dbController = dbController;
    }

    @EventListener
    public void handleEvent(ConvertDbToCSVEvent event) throws SQLException, IOException {
        List<Log> logs = dbController.getLogs(LogsFilter.EMPTY);
        if (logs.isEmpty()) {
            return;
        }
        FileWriter csvWriter = new FileWriter(event.pathForResult());
        String titles = "Дата,Описание,Цена,Категория,Тип сессии\n";
        csvWriter.append(titles);

        for (Log log : logs) {
            csvWriter.append(Utils.getCSV(log));
        }

        csvWriter.flush();
        System.out.printf("%d логов успешно экспортированы в CSV: %s\n", logs.size(), event.pathForResult());
    }
}
