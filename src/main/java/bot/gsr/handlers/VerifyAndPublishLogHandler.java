package bot.gsr.handlers;

import bot.gsr.events.AddToDbEvent;
import bot.gsr.events.PublishInChannelEvent;
import bot.gsr.events.VerifyAndPublishLogEvent;
import bot.gsr.telegram.TelegramController;
import bot.gsr.telegram.model.Decision;
import bot.gsr.telegram.model.LogDecision;
import bot.gsr.telegram.model.LogWithUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@PropertySource("classpath:application.properties")
public class VerifyAndPublishLogHandler {
    @Value("${log.add.to.channel}")
    private boolean addToChannel;
    @Value("${log.add.to.db}")
    private boolean addToDb;

    private final TelegramController telegramController;
    private final EventManager eventManager;

    @Autowired
    public VerifyAndPublishLogHandler(TelegramController telegramController, EventManager eventManager) {
        this.telegramController = telegramController;
        this.eventManager = eventManager;
    }

    @EventListener
    public CompletableFuture<Void> handleEvent(VerifyAndPublishLogEvent event) {
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
                }
            }
            promise.complete(null);
        });

        telegramController.verifyLog(event.logWithUrl());
        return promise;
    }

}
