package telegram.commands;

import java.sql.Date;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;
import events.VerifyAndPublishLogEvent;
import handlers.EventManager;
import telegram.Bot;
import telegram.model.LogWithUrl;
import utils.Utils;

import static telegram.MarkupFactory.EDIT_CATEGORY_MARKUP;
import static telegram.MarkupFactory.REMOVE_MARKUP;

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
                enterDescription(Category.findByName((String) answer));
            });
            absSender.execute(message);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
                Log log = buildLog(category, date, description);
                CompletableFuture<Void> promise = new CompletableFuture<>();
                eventManager.handleEvent(new VerifyAndPublishLogEvent(new LogWithUrl(log, null), promise));
            } catch (DateTimeParseException ex) {
                enterDate(category, description);
            }
        });
        abs.execute(message);
    }

    private Log buildLog(Category category, Date date, String description) {
        int price = 0;
        SessionType sessionType = null;
        switch (category) {
            case SESSION -> {
                price = 2600;
                sessionType = SessionType.SR;
                if (description.contains("СЧ1")) {
                    price = 5000;
                    sessionType = SessionType.SCH1;
                } else if (description.contains("СЧ2")) {
                    price = 5000;
                    sessionType = SessionType.SCH2;
                } else if (description.contains("С#") || description.contains("C#")) {
                    price = 5000;
                    sessionType = SessionType.STRUCTURE;
                } else if (description.contains("СЧ#1")) {
                    price = 8000;
                    sessionType = SessionType.STRUCTURE_SCH1;
                } else if (description.contains("СЧ#2")) {
                    price = 8000;
                    sessionType = SessionType.STRUCTURE_SCH2;
                }
            }
            case EXPERT_SUPPORT -> price = 10000;
            case ONE_PLUS -> price = 4000;
        }
        return new Log(null, date, description, price, category, sessionType);
    }
}
