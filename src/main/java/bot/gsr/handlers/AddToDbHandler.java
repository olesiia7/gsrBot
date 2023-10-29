package bot.gsr.handlers;

import bot.gsr.events.AddToDbEvent;
import bot.gsr.model.Log;
import bot.gsr.service.LogService;
import bot.gsr.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 *  Добавляет событие в БД
 */
@Component
public class AddToDbHandler {
    private final Logger logger = LoggerFactory.getLogger(AddToDbHandler.class);
    private final LogService logService;

    public AddToDbHandler(LogService logService) {
        this.logService = logService;
    }

    @EventListener
    public void handleEvent(AddToDbEvent event) {
        Log log = event.log();
        logService.addLog(log);
        logger.info(Utils.getCSV(event.log()));
    }
}
