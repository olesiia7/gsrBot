package bot.gsr.handlers;

import bot.gsr.events.GetLastSessionOrDiagnosticEvent;
import bot.gsr.service.LogService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Получает сессии/диагностики на последнюю дату в БД
 */
@Component
public class GetLastSessionOrDiagnosticHandler {
    private final LogService logService;

    public GetLastSessionOrDiagnosticHandler(LogService logService) {
        this.logService = logService;
    }

    @EventListener
    public void handleEvent(GetLastSessionOrDiagnosticEvent event) {
        List<String> lastSessionOrDiagnostic = logService.getLastSessionOrDiagnostic();
        event.result().complete(lastSessionOrDiagnostic);
    }
}
