package bot.gsr.telegram.commands;

import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.Log;
import bot.gsr.events.VerifyAndPublishLogEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.telegram.Bot;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.utils.Utils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;

import static bot.gsr.telegram.MarkupFactory.*;

@Component
public class AddLogCommand extends BotCommand {
    private final EventManager eventManager;
    private Bot bot;
    private AbsSender abs;
    private Long chatId;

    public AddLogCommand(EventManager eventManager) {
        super("add", "Добавить запись в БД");
        this.eventManager = eventManager;
    }

    public void setListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        abs = absSender;
        chatId = chat.getId();
        try {
            String text = "Выберите категорию добавляемой записи:";

            SendMessage message = new SendMessage();
            message.enableMarkdown(true);
            message.setChatId(chat.getId());
            message.setText(text);
            message.setReplyMarkup(EDIT_CATEGORY_MARKUP);
            bot.setListener(answer -> {
                Category category = Category.findByName((String) answer);
                if (category == Category.EXPERT_SUPPORT
                        || category == Category.ONE_PLUS) {
                    enterPeriod(category);
                } else {
                    enterDescription(category);
                }
            });
            absSender.execute(message);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void enterPeriod(Category category) throws TelegramApiException {
        String text = "Выберите период оплаты";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(MONTHS_MARKUP);
        bot.setListener(answer -> {
            String monthName = (String) answer;
            int month = Utils.getMonthNumber(monthName);
            int year = LocalDate.now().getYear();

            Date date = Utils.getDate(1, month, year);
            String description = monthName + " " + year;

            Log log = Utils.predictLog(description, category, date);
            CompletableFuture<Void> promise = new CompletableFuture<>();
            eventManager.handleEvent(new VerifyAndPublishLogEvent(new LogWithUrl(log, null), promise));
        });
        abs.execute(message);
    }

    private void enterDescription(Category category) throws TelegramApiException {
        String text = "Выбранная категория: " + category.getName() +
                "\nВведите описание";
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(chatId);
        message.setReplyMarkup(REMOVE_MARKUP);
        bot.setListener(answer -> {
            String description = (String) answer;
            enterDate(category, description);
        });
        abs.execute(message);
    }

    private void enterDate(Category category, String description) throws TelegramApiException {
        String text = "Выбранная категория: " + category.getName() +
                "\nОписание: " + description +
                "\nВведите дату в формате dd.MM.yyyy";
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(chatId);
        message.setReplyMarkup(REMOVE_MARKUP);
        bot.setListener(answer -> {
            try {
                Date date = Utils.toDate((String) answer);
                Log log = Utils.predictLog(description, category, date);
                CompletableFuture<Void> promise = new CompletableFuture<>();
                eventManager.handleEvent(new VerifyAndPublishLogEvent(new LogWithUrl(log, null), promise));
            } catch (DateTimeParseException ex) {
                enterDate(category, description);
            }
        });
        abs.execute(message);
    }
}
