package bot.gsr.handlers;

import bot.gsr.events.AddToDbEvent;
import bot.gsr.model.Log;
import bot.gsr.service.LogService;
import bot.gsr.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Добавляет событие в БД
 */
@Component
@PropertySource("classpath:application.properties")
public class AddToDbHandler {
    private final Logger logger = LoggerFactory.getLogger(AddToDbHandler.class);
    private final boolean isEnabled;
    private final LogService logService;

    public AddToDbHandler(@Value("${log.add.to.db}") boolean isEnabled,
                          LogService logService) {
        this.isEnabled = isEnabled;
        this.logService = logService;
    }

    @EventListener
    public void handleEvent(AddToDbEvent event) {
        if (!isEnabled) {
            return;
        }
        Log log = event.log();
        logService.addLog(log);
        String csv = Utils.getCSV(event.log());
        logger.debug(csv);
    }
}
