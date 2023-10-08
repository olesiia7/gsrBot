package bot.gsr.handlers;

import bot.gsr.events.PublishInChannelEvent;
import bot.gsr.telegram.TelegramController;
import bot.gsr.telegram.model.LogWithUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static bot.gsr.telegram.TelegramUtils.formatPageMessage;

@Component
public class PublishInChannelHandler {
    private final Logger logger = LoggerFactory.getLogger(PublishInChannelHandler.class);
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
        logger.info(logWithUrl.channelLog());
    }
}
