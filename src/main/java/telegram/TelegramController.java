package telegram;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import SQLite.model.Category;
import SQLite.model.SessionType;
import telegram.model.Decision;
import telegram.model.LogDecision;
import telegram.model.LogWithUrl;

import static telegram.MarkupFactory.EDIT_FINISHED;
import static telegram.MarkupFactory.EDIT_SESSION_PRICE;
import static telegram.MarkupFactory.EDIT_CATEGORY;
import static telegram.MarkupFactory.EDIT_SESSION_TYPE;

@Component
@Scope(value = "singleton")
public class TelegramController {
    private final TelegramService service;
    private AnswerListener listener;

    public TelegramController(TelegramService service) {
        this.service = service;
    }

    public void setListener(AnswerListener listener) {
        this.listener = listener;
    }

    public boolean connectToBot() {
        return service.connectToBot();
    }

    public void sendMessage(String message) {
        service.sendMessage(message);
    }

    public void verifyLog(LogWithUrl log) throws TelegramApiException {
        service.verifyLog(log, answer -> {
            Decision decision = Decision.valueOf((String) answer);
            switch (decision) {
                case APPROVE -> listener.processAnswer(new LogDecision(log, Decision.APPROVE));
                case DECLINE -> listener.processAnswer(new LogDecision(log, Decision.DECLINE));
                case EDIT -> editLog(log); // отправить на доработку
            }
        });
    }

    private void deleteMarkupAndVerifyLog(LogWithUrl log) throws TelegramApiException {
        deleteMarkup();
        verifyLog(log);
    }

    public void editLog(LogWithUrl log) throws TelegramApiException {
        service.editLog(answer -> {
            switch ((String) answer) {
                case EDIT_SESSION_PRICE -> waitNewPrice(log);
                case EDIT_CATEGORY -> waitNewCategory(log);
                case EDIT_SESSION_TYPE -> waitNewSessionType(log);
                case EDIT_FINISHED -> deleteMarkupAndVerifyLog(log);
            }
        });
    }

    private void deleteMarkup() throws TelegramApiException {
        service.deleteMarkup("Ваш ответ принят.");
    }

    public void waitNewPrice(LogWithUrl log) throws TelegramApiException {
        service.waitNewPrice(answer -> {
            if (answer.equals(EDIT_FINISHED)) {
                deleteMarkupAndVerifyLog(log);
                return;
            }
            try {
                int newPrice = Integer.parseInt((String) answer);
                deleteMarkupAndVerifyLog(LogWithUrl.getLogWithNewPrice(log, newPrice));
            } catch (NumberFormatException ex) {
                service.sendMeMessage("Неправильный формат цены: введите цифры без знаков и пробелов", false);
            }
        });
    }

    public void waitNewCategory(LogWithUrl log) throws TelegramApiException {
        service.waitNewCategory(answer -> {
            if (answer.equals(EDIT_FINISHED)) {
                deleteMarkupAndVerifyLog(log);
                return;
            }
            Category newCategory = Category.findByName((String) answer);
            SessionType sessionType = log.sessionType();
            if (newCategory != Category.SESSION) {
                sessionType = null;
            } else {
                if (sessionType == null) {
                    sessionType = SessionType.SR;
                }
            }
            deleteMarkupAndVerifyLog(LogWithUrl.getLogWithNewCategoryAndSessionType(log, newCategory, sessionType));
        });
    }

    public void waitNewSessionType(LogWithUrl log) throws TelegramApiException {
        service.waitNewSessionType(answer -> {
            if (answer.equals(EDIT_FINISHED)) {
                deleteMarkupAndVerifyLog(log);
                return;
            }
            SessionType newSessionType = SessionType.findByName((String) answer);
            Category category = Category.SESSION;
            deleteMarkupAndVerifyLog(LogWithUrl.getLogWithNewCategoryAndSessionType(log, category, newSessionType));
        });
    }

}
