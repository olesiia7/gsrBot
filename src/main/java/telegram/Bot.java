package telegram;

import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.objects.Update;


public final class Bot extends TelegramLongPollingCommandBot {
    private final String botName;

    public Bot(String botToken, String botName) {
        super(botToken);
        this.botName = botName;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}