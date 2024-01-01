package bot.gsr.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Date;

@Component
@PropertySource("classpath:telegram.properties")
public class TelegramService {
    @Value("${telegram.bot.chatId}")
    private String chatId;
    @Value("${telegram.bot.myId}")
    private String myId;

    private final Bot bot;
    private final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    public TelegramService(Bot bot) {
        this.bot = bot;
    }

    public boolean connectToBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            logger.info("Подключились к боту {}", bot.getBotUsername());
            return true;
        } catch (TelegramApiException e) {
            logger.error("Error while connecting to telegram: {}", e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            SendMessage sendMessage = new SendMessage(chatId, message);
            sendMessage.setParseMode("MarkdownV2");
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("{}: Error while sending message '{}': {}", new Date(), message, e.getMessage());
        }
    }
}
