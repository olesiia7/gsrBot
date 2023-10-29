package bot.gsr.handlers;

import bot.gsr.events.ConvertDbToCSVEvent;
import bot.gsr.model.Log;
import bot.gsr.model.LogFilter;
import bot.gsr.service.LogService;
import bot.gsr.utils.Utils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Публикует в канале статью
 */
@Component
public class ConvertDbToCSVHandler {
    private final LogService logService;

    public ConvertDbToCSVHandler(LogService logService) {
        this.logService = logService;
    }

    @EventListener
    public void handleEvent(ConvertDbToCSVEvent event) throws IOException {
        List<Log> logs = logService.getLogs(LogFilter.EMPTY);
        if (logs.isEmpty()) {
            return;
        }
        try (FileWriter csvWriter = new FileWriter(event.pathForResult())) {
            String titles = "Дата,Описание,Цена,Категория,Тип сессии\n";
            csvWriter.append(titles);

            for (Log log : logs) {
                csvWriter.append(Utils.getCSV(log)).append("\n");
            }

            csvWriter.flush();
        }
        System.out.printf("%d логов успешно экспортированы в CSV: %s\n", logs.size(), event.pathForResult());
    }
}
