package bot.gsr.telegram;

import bot.gsr.model.Category;
import bot.gsr.model.SessionType;
import bot.gsr.telegram.model.Decision;
import bot.gsr.telegram.model.LogDecision;
import bot.gsr.telegram.model.LogWithUrl;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import javax.validation.constraints.NotNull;

import static bot.gsr.telegram.MarkupFactory.*;

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

    public void sendMeMessage(@NotNull String message,
                              @Nullable ReplyKeyboard keyboard,
                              @Nullable AnswerListener listener,
                              boolean formatted) {
        service.sendMeMessage(message, keyboard, listener, formatted);
    }

    public void verifyLog(LogWithUrl log) {
        service.verifyLog(log, answer -> {
            Decision decision = Decision.valueOf((String) answer);
            switch (decision) {
                case APPROVE -> listener.processAnswer(new LogDecision(log, Decision.APPROVE));
                case DECLINE -> listener.processAnswer(new LogDecision(log, Decision.DECLINE));
                case EDIT -> editLog(log); // отправить на доработку
            }
        });
    }

    private void deleteMarkupAndVerifyLog(LogWithUrl log) {
        deleteMarkup();
        verifyLog(log);
    }

    private void editLog(LogWithUrl log) {
        service.editLog(answer -> {
            switch ((String) answer) {
                case EDIT_SESSION_PRICE -> waitNewPrice(log);
                case EDIT_CATEGORY -> waitNewCategory(log);
                case EDIT_SESSION_TYPE -> waitNewSessionType(log);
                case EDIT_FINISHED -> deleteMarkupAndVerifyLog(log);
            }
        });
    }

    private void deleteMarkup() {
        service.deleteMarkup("Ваш ответ принят.");
    }

    private void waitNewPrice(LogWithUrl log) {
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

    private void waitNewCategory(LogWithUrl log) {
        service.waitNewCategory(answer -> {
            if (answer.equals(EDIT_FINISHED)) {
                deleteMarkupAndVerifyLog(log);
                return;
            }
            Category newCategory = Category.getCategory((String) answer);
            SessionType sessionType = log.log().sessionType();
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

    private void waitNewSessionType(LogWithUrl log) {
        service.waitNewSessionType(answer -> {
            if (answer.equals(EDIT_FINISHED)) {
                deleteMarkupAndVerifyLog(log);
                return;
            }
            SessionType newSessionType = SessionType.getSessionType((String) answer);
            Category category = Category.SESSION;
            deleteMarkupAndVerifyLog(LogWithUrl.getLogWithNewCategoryAndSessionType(log, category, newSessionType));
        });
    }

}
