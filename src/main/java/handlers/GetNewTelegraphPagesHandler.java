package handlers;

import java.util.Comparator;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import SQLite.model.Log;
import events.GetNewTelegraphPagesEvent;
import telegram.model.LogWithUrl;
import telegraph.TelegraphController;
import telegraph.model.Page;
import utils.Utils;

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
