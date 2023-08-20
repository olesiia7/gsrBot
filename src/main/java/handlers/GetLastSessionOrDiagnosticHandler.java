package handlers;

import java.sql.SQLException;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import events.GetLastSessionOrDiagnosticEvent;

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
