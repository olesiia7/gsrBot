package bot.gsr.telegram;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;

public interface AnswerListener {
    void processAnswer(Object answer) throws TelegramApiException, SQLException;
}
