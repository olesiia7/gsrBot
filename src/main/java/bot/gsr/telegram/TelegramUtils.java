package bot.gsr.telegram;

import bot.gsr.telegram.model.Decision;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.utils.Utils;

import java.sql.Date;

public final class TelegramUtils {

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
        text = text.replace("#", "\\#");
        text = text.replace("+", "\\+");
        text = text.replace("|", "\\|");
        return text;
    }

    //  Сессия: Адекватизация др. Актуальное
//      23.02.2023, Сессия по Судьбе Рода
//      2 600 ₽
//      https://telegra.ph/Aktualnoe-dr-07-10
    public static String getVerifyingMsg(LogWithUrl log) {
        String msg = log.log().category().getName() + ": *" + log.log().description() + "*\n" +
                "_" + Utils.getDate(log.log().date());
        if (log.log().sessionType() != null) {
            msg += ", " + log.log().sessionType().getName();
        }
        msg += "_\n" +
                Utils.formatPrice(log.log().price());
        if (log.url() != null && !log.url().isEmpty()) {
            msg += "\n" + log.url();
        }
        msg = cleanText(msg);
        return msg;
    }

    public static String formatPageMessage(String title, Date created, String url) {
        String message = "*" + title + "*\n" +
                "_" + Utils.getDate(created) + "_\n" +
                url;
        message = cleanText(message);
        return message;
    }
}
