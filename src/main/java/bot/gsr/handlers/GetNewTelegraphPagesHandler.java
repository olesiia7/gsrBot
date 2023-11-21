package bot.gsr.handlers;

import bot.gsr.events.GetNewTelegraphPagesEvent;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.telegraph.TelegraphController;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetNewTelegraphPagesHandler {
    private final TelegraphController telegraphController;

    public GetNewTelegraphPagesHandler(TelegraphController telegraphController) {
        this.telegraphController = telegraphController;
    }

    @EventListener
    public void handleEvent(GetNewTelegraphPagesEvent event) {
        List<LogWithUrl> logs = telegraphController.getNewLogs(event.lastPageNames());
        event.result().complete(logs);
    }

}
