package bot.gsr.handlers;

import bot.gsr.events.SendMeTelegramMessageEvent;
import bot.gsr.telegram.TelegramController;
import bot.gsr.telegram.TelegramUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SendMeTelegramMessageHandler {
    private final TelegramController controller;

    public SendMeTelegramMessageHandler(TelegramController controller) {
        this.controller = controller;
    }

    @EventListener
    public void handleEvent(SendMeTelegramMessageEvent event) {
        String text = event.message();
        if (event.formatted()) {
            text = TelegramUtils.cleanText(text);
        }
        controller.sendMeMessage(text, event.keyboard(), event.listener(), event.formatted());
    }
}
