package handlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import events.PublishInChannelEvent;
import telegram.TelegramController;
import telegram.model.LogWithUrl;

import static telegram.TelegramUtils.formatPageMessage;

@Component
public class PublishInChannelHandler {
    private final TelegramController telegramController;

    public PublishInChannelHandler(TelegramController telegramController) {
        this.telegramController = telegramController;
    }

    @EventListener
    public void handleEvent(PublishInChannelEvent event) {
        LogWithUrl logWithUrl = event.logWithUrl();
        String url = logWithUrl.url();
        if (url == null || url.isEmpty()) {
            return;
        }
        String formattedMessage = formatPageMessage(logWithUrl.log().description(), logWithUrl.log().date(), url);
        telegramController.sendMessage(formattedMessage);
    }
}
