package bot.gsr.handlers;

import bot.gsr.SQLite.DbController;
import bot.gsr.events.GetLastSessionOrDiagnosticEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

/**
 * Получает сессии/диагностики на последнюю дату в БД
 */
@Component
public class GetLastSessionOrDiagnosticHandler {
    private final DbController dbController;

    public GetLastSessionOrDiagnosticHandler(DbController dbController) {
        this.dbController = dbController;
    }

    @EventListener
    public void handleEvent(GetLastSessionOrDiagnosticEvent event) throws SQLException {
        List<String> lastSessionOrDiagnostic = dbController.getLastSessionOrDiagnostic();
        event.result().complete(lastSessionOrDiagnostic);
    }
}
