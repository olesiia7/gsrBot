package bot.gsr.handlers;

import bot.gsr.events.GetBackupEvent;
import bot.gsr.service.LogService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class GetBackupHandler {
    private final LogService logService;


    public GetBackupHandler(LogService logService) {
        this.logService = logService;
    }

    @EventListener
    public void handleEvent(GetBackupEvent event) {
        InputStream dump = logService.getDump();
        event.resultPromise().complete(dump);
    }
}
