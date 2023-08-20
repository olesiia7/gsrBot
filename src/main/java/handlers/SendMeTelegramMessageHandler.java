package handlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import events.SendMeTelegramMessageEvent;
import telegram.TelegramController;
import telegram.TelegramUtils;

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
