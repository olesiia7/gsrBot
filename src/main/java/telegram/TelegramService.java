package telegram;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import telegram.commands.AddLogCommand;
import telegram.commands.QueryCommand;
import telegram.model.LogWithUrl;

import static telegram.MarkupFactory.BACK_MARKUP;
import static telegram.MarkupFactory.EDITING_MARKUP;
import static telegram.MarkupFactory.EDIT_CATEGORY_MARKUP;
import static telegram.MarkupFactory.EDIT_SESSION_TYPE_MARKUP;
import static telegram.MarkupFactory.REMOVE_MARKUP;
import static telegram.MarkupFactory.VERIFYING_MARKUP;
import static telegram.TelegramUtils.getVerifyingMsg;

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

    public TelegramService(QueryCommand queryCommand, AddLogCommand addLogCommand) {
        this.queryCommand = queryCommand;
        this.addLogCommand = addLogCommand;
    }

    @PostConstruct
    private void init() {
        this.bot = new Bot(botToken, botName, queryCommand, addLogCommand);
    }

    public boolean connectToBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("Подключились к боту " + bot.getBotUsername());
            return true;
        } catch (TelegramApiException e) {
            System.out.printf("Error while connecting to telegram: %s\n", e.getMessage());
            return false;
        }
    }

    public void sendMeMessage(String message, boolean formatted) throws TelegramApiException {
        sendMeMessage(message, null, null, formatted);
    }

    /**
     * Для удаления клавиатуры сейчас нет решения лучше, чем выпустить сообщение
     */
    public void deleteMarkup(String message) throws TelegramApiException {
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

    public void verifyLog(LogWithUrl log, AnswerListener listener) throws TelegramApiException {
        String message = getVerifyingMsg(log);
        sendMeMessage(message, VERIFYING_MARKUP, listener, true);
    }

    public void editLog(AnswerListener listener) throws TelegramApiException {
        String message = "Выберите, что вы хотите изменить:";
        sendMeMessage(message, EDITING_MARKUP, listener, false);
    }

    public void waitNewPrice(AnswerListener listener) throws TelegramApiException {
        String message = "Введите новую цену (цифры без знаков и пробелов)";
        sendMeMessage(message, BACK_MARKUP, listener, false);
    }

    public void waitNewCategory(AnswerListener listener) throws TelegramApiException {
        String message = "Выберите новую категорию:";
        sendMeMessage(message, EDIT_CATEGORY_MARKUP, listener, false);
    }

    public void waitNewSessionType(AnswerListener listener) throws TelegramApiException {
        String message = "Выберите новый тип:";
        sendMeMessage(message, EDIT_SESSION_TYPE_MARKUP, listener, false);
    }

    private void sendMeMessage(@NotNull String message,
                               @Nullable ReplyKeyboard keyboard,
                               @Nullable AnswerListener listener,
                               boolean formatted) throws TelegramApiException {
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
        bot.execute(sendMessage);
    }
}
