package bot.gsr.handlers;

import bot.gsr.events.GetNewTelegraphPagesEvent;
import bot.gsr.model.Log;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.telegraph.TelegraphController;
import bot.gsr.telegraph.model.Page;
import bot.gsr.utils.Utils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;

@Component
public class GetNewTelegraphPagesHandler {
    private final TelegraphController telegraphController;

    public GetNewTelegraphPagesHandler(TelegraphController telegraphController) {
        this.telegraphController = telegraphController;
    }

    @EventListener
    public void handleEvent(GetNewTelegraphPagesEvent event) {
        List<Page> newPages = telegraphController.getNewPages(event.lastSessionOrDiagnostic());
        List<LogWithUrl> logs = newPages.stream()
                .map(this::pageToLogWithUrl)
                .sorted(Comparator.comparing(l -> l.log().date()))
                .toList();
        event.result().complete(logs);
    }

    private LogWithUrl pageToLogWithUrl(@NotNull Page page) {
        Log log = Utils.predictLog(page.getTitle(), null, page.getCreated());
        return new LogWithUrl(log, page.getUrl());
    }

}
