package bot.gsr.telegram;

import bot.gsr.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.sql.Date;

public final class TelegramUtils {
    public static final String CALLBACK_DELIMITER = "#";
    private static final Logger logger = LoggerFactory.getLogger(TelegramUtils.class);

    private TelegramUtils() {
    }

    public static Message sendMessage(@NotNull String message,
                                      @Nullable ReplyKeyboard keyboard,
                                      boolean formatted,
                                      @NotNull String chatId,
                                      @NotNull AbsSender absSender) {
        SendMessage sendMessage = new SendMessage(chatId, message);
        if (formatted) {
            sendMessage.setParseMode("MarkdownV2");
        }
        if (keyboard != null) {
            sendMessage.setReplyMarkup(keyboard);
        }
        try {
            return absSender.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при попытке отправить сообщение: {}", message);
            logger.error(e.getMessage());
            return null;
        }
    }

    public static void editMessage(@NotNull String chatId,
                                   @NotNull Integer messageId,
                                   @NotNull String text,
                                   @Nullable InlineKeyboardMarkup markup,
                                   boolean setMarkdown,
                                   @NotNull AbsSender absSender) {
        try {
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(messageId);
            editMessageText.setText(text);
            if (setMarkdown) {
                editMessageText.setParseMode("MarkdownV2");
            }
            editMessageText.setReplyMarkup(markup);

            absSender.execute(editMessageText);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при попытке изменить сообщение на: {}", text);
            logger.error(e.getMessage());
        }
    }

    public static void deleteMessage(@NotNull String chatId,
                                     @NotNull Integer messageId,
                                     @NotNull AbsSender absSender) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        try {
            absSender.execute(deleteMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при попытке удалить сообщение, id: {}", deleteMessage.getMessageId());
            logger.error(e.getMessage());
        }
    }

    /**
     * Экранирует символы, которые давали бы ошибку при форматировании
     */
    public static String cleanText(String text) {
        text = text.replace(".", "\\.");
        text = text.replace("-", "\\-");
        text = text.replace("(", "\\(");
        text = text.replace(")", "\\)");
        text = text.replace("!", "\\!");
        text = text.replace("#", "\\#");
        text = text.replace("+", "\\+");
        text = text.replace("|", "\\|");
        return text;
    }

    public static String formatPageMessage(String title, Date created, String url) {
        String message = "*" + title + "*\n" +
                "_" + Utils.getDate(created) + "_\n" +
                url;
        message = cleanText(message);
        return message;
    }
}
