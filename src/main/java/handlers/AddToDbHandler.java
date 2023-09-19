package handlers;

import SQLite.DbController;
import SQLite.model.Log;
import events.AddToDbEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import utils.Utils;

import java.sql.SQLException;

/**
 *  Добавляет событие в БД
 */
@Component
public class AddToDbHandler {
    private final Logger logger = LoggerFactory.getLogger(AddToDbHandler.class);
    private final DbController dbController;

    public AddToDbHandler(DbController dbController) {
        this.dbController = dbController;
    }

    @EventListener
    public void handleEvent(AddToDbEvent event) throws SQLException {
        Log log = event.log();
        dbController.addLog(log);
        logger.info(Utils.getCSV(event.log()));
    }
}
