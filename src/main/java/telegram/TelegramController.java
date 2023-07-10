package telegram;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "singleton")
public class TelegramController {
    private final TelegramService service;

    public TelegramController(TelegramService service) {
        this.service = service;
    }

    public boolean connectToBot() {
        return service.connectToBot();
    }

    public void sendMessage(String message) {
        service.sendMessage(message);
    }

}
