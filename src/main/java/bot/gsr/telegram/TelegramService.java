package bot.gsr.telegram;

import bot.gsr.telegram.commands.AddLogCommand;
import bot.gsr.telegram.commands.CheckNewLogsCommand;
import bot.gsr.telegram.commands.QueryCommand;
import bot.gsr.telegram.model.LogWithUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.Date;

import static bot.gsr.telegram.MarkupFactory.*;
import static bot.gsr.telegram.TelegramUtils.getVerifyingMsg;

@Component
@PropertySource("classpath:telegram.properties")
public class TelegramService {
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.name}")
    private String botName;
    @Value("${telegram.bot.chatId}")
    private String chatId;
    @Value("${telegram.bot.myId}")
    private String myId;

    private Bot bot;
    private final QueryCommand queryCommand;
    private final AddLogCommand addLogCommand;
    private final CheckNewLogsCommand checkNewLogsCommand;
    private final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    public TelegramService(QueryCommand queryCommand,
                           AddLogCommand addLogCommand,
                           CheckNewLogsCommand checkNewLogsCommand) {
        this.queryCommand = queryCommand;
        this.addLogCommand = addLogCommand;
        this.checkNewLogsCommand = checkNewLogsCommand;
    }

    @PostConstruct
    private void init() {
        this.bot = new Bot(botToken, botName, queryCommand, addLogCommand, checkNewLogsCommand);
    }

    public boolean connectToBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            logger.info("Подключились к боту " + bot.getBotUsername());
            return true;
        } catch (TelegramApiException e) {
            System.out.printf("Error while connecting to telegram: %s\n", e.getMessage());
            return false;
        }
    }

    public void sendMeMessage(String message, boolean formatted) {
        sendMeMessage(message, null, null, formatted);
    }

    /**
     * Для удаления клавиатуры сейчас нет решения лучше, чем выпустить сообщение
     */
    public void deleteMarkup(String message) {
        sendMeMessage(message, REMOVE_MARKUP, null, false);
    }

    public void sendMessage(String message) {
        try {
            SendMessage sendMessage = new SendMessage(chatId, message);
            sendMessage.setParseMode("MarkdownV2");
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.printf("%s: Error while sending message '%s': %s\n", new Date(), message, e.getMessage());
        }
    }

    public void verifyLog(LogWithUrl log, AnswerListener listener) {
        String message = getVerifyingMsg(log);
        sendMeMessage(message, VERIFYING_MARKUP, listener, true);
    }

    public void editLog(AnswerListener listener) {
        String message = "Выберите, что вы хотите изменить:";
        sendMeMessage(message, EDITING_MARKUP, listener, false);
    }

    public void waitNewPrice(AnswerListener listener) {
        String message = "Введите новую цену (цифры без знаков и пробелов)";
        sendMeMessage(message, BACK_MARKUP, listener, false);
    }

    public void waitNewCategory(AnswerListener listener) {
        String message = "Выберите новую категорию:";
        sendMeMessage(message, EDIT_CATEGORY_MARKUP, listener, false);
    }

    public void waitNewSessionType(AnswerListener listener) {
        String message = "Выберите новый тип:";
        sendMeMessage(message, EDIT_SESSION_TYPE_MARKUP, listener, false);
    }

    public void sendMeMessage(@NotNull String message,
                               @Nullable ReplyKeyboard keyboard,
                               @Nullable AnswerListener listener,
                               boolean formatted) {
        SendMessage sendMessage = new SendMessage(myId, message);
        if (formatted) {
            sendMessage.setParseMode("MarkdownV2");
        }
        if (keyboard != null) {
            sendMessage.setReplyMarkup(keyboard);
        }
        if (listener != null) {
            bot.setListener(listener);
        }
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при попытке отправить сообщение: " + message);
            logger.error(e.getMessage());
        }
    }
}
