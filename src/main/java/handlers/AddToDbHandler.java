package handlers;

import java.sql.SQLException;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.model.Log;
import events.AddToDbEvent;

/**
 * Публикует в канале статью
 */
@Component
public class AddToDbHandler {
    private final DbController dbController;

    public AddToDbHandler(DbController dbController) {
        this.dbController = dbController;
    }

    @EventListener
    public void handleEvent(AddToDbEvent event) throws SQLException {
        Log log = event.log();
        dbController.addLog(log);
    }
}
