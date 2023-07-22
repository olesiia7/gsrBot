package telegram;

import java.sql.Date;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import telegram.model.Decision;
import telegram.model.LogWithUrl;

public final class TelegramUtils {
    private static final DecimalFormat D_F = new DecimalFormat("###,###");
    private static final DateTimeFormatter CHANNEL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static String addDecisionToMsg(String msg, String callQueryJson) {
        Decision decision = Decision.valueOf(callQueryJson);
        msg += "\n\n";
        switch (decision) {
            case APPROVE -> msg += "✅ Одобрено";
            case DECLINE -> msg += "❌ Пропущено";
            case EDIT -> msg += "✏️ Изменено";
        }
        return msg;
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
        return text;
    }

    //  Сессия: Адекватизация др. Актуальное
//      23.02.2023, Сессия по Судьбе Рода
//      2 600 ₽
//      https://telegra.ph/Aktualnoe-dr-07-10
    public static String getVerifyingMsg(LogWithUrl log) {
        String msg = log.category().getName() + ": *" + log.description() + "*\n" +
                "_" + formatDateForChannel(log.date());
        if (log.sessionType() != null) {
            msg += ", " + log.sessionType().getName();
        }
        msg += "_\n" +
                formatPriceForChannel(log.price()) + "\n" +
                log.url();
        msg = cleanText(msg);
        return msg;
    }

    public static String formatPageMessage(String title, Date created, String url) {
        String message = "*" + title + "*\n" +
                "_" + formatDateForChannel(created) + "_\n" +
                url;
        message = cleanText(message);
        return message;
    }

    public static String formatPriceForChannel(int price) {
        return D_F.format(price) + " ₽";
    }

    public static String formatDateForChannel(Date date) {
        LocalDate localDate = date.toLocalDate();
        return localDate.format(CHANNEL_DATE_FORMATTER);
    }
}
