package telegram;

import java.sql.SQLException;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface AnswerListener {
    void processAnswer(Object answer) throws TelegramApiException, SQLException;
}
