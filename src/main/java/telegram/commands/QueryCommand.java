package telegram.commands;


import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import SQLite.DbController;
import SQLite.LogsFilter;
import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;
import utils.Utils;

import static telegram.TelegramUtils.formatPriceForChannel;

@Component
public class QueryCommand extends BotCommand {
    private final DbController db;

    public QueryCommand(DbController db) {
        super("query", "Посмотреть записи БД");
        this.db = db;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("*Последняя запись:*\n");
            List<Log> lastRecords = db.getLastRecords(1);
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последняя сессия:*\n");
            LogsFilter.Builder builder = new LogsFilter.Builder();
            builder.setCategory(Category.SESSION);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последняя ранговая:*\n");
            builder = new LogsFilter.Builder();
            builder.setSessionType(SessionType.RANG);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последняя диагностика:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.DIAGNOSTIC);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последний продукт GSR:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.GSR_PRODUCT);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Оплата экспертного:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.EXPERT_SUPPORT);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Оплата 1+:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.ONE_PLUS);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            SendMessage message = new SendMessage();
            message.enableMarkdown(true);
            message.setChatId(chat.getId());
            message.setText(sb.toString());
            absSender.execute(message);
        } catch (SQLException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String toString(Log log) {
        String formattedDate = Utils.getDate(log.date());
        String formattedPrice = formatPriceForChannel(log.price());
        return String.format("%-12s%-40s%-11s", formattedDate, log.description(), formattedPrice);
    }
}
