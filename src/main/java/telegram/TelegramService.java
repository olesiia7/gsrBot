package telegram;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@PropertySource("classpath:telegram.properties")
public class TelegramService {
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.name}")
    private String botName;
    @Value("${telegram.bot.chatId}")
    private String chatId;

    private Bot bot;

    public TelegramService() {
    }

    @PostConstruct
    private void init() {
        this.bot = new Bot(botToken, botName);
    }

    public boolean connectToBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            return true;
        } catch (TelegramApiException e) {
            System.out.printf("Error while connecting to telegram: %s\n", e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            SendMessage sendMessage = new SendMessage(chatId, message);
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.printf("%s: Error while sending message '%s': %s\n", new Date(), message, e.getMessage());
        }
    }
}
