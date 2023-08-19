package handlers;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import events.AddToDbEvent;
import events.PublishInChannelEvent;
import events.VerifyAndPublishLogEvent;
import telegram.TelegramController;
import telegram.model.Decision;
import telegram.model.LogDecision;
import telegram.model.LogWithUrl;
import utils.Utils;

@Component
@PropertySource("classpath:application.properties")
public class VerifyAndPublishLogHandler {
    @Value("${log.add.to.channel}")
    private boolean addToChannel;
    @Value("${log.add.to.db}")
    private boolean addToDb;
    @Value("${print.added.csv}")
    private boolean printAddedCSV;

    private final TelegramController telegramController;
    private final EventManager eventManager;

    @Autowired
    public VerifyAndPublishLogHandler(TelegramController telegramController, EventManager eventManager) {
        this.telegramController = telegramController;
        this.eventManager = eventManager;
    }

    @EventListener
    public CompletableFuture<Void> handleEvent(VerifyAndPublishLogEvent event) throws TelegramApiException, InterruptedException {
        CompletableFuture<Void> promise = event.resultPromise();
        telegramController.setListener(answer -> {
            LogDecision logDecision = (LogDecision) answer;
            if (logDecision.decision() == Decision.APPROVE) {
                LogWithUrl logWithUrl = logDecision.logWithUrl();
                if (addToChannel) {
                    eventManager.handleEvent(new PublishInChannelEvent(logWithUrl));
                }
                if (addToDb) {
                    eventManager.handleEvent(new AddToDbEvent(logWithUrl.log()));
                    if (printAddedCSV) {
                        System.out.println(Utils.getCSV(logWithUrl.log()));
                    }
                }
            }
            promise.complete(null);
        });

        telegramController.verifyLog(event.logWithUrl());
        return promise;
    }

}
