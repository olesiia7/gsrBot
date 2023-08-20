package telegram.commands;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import events.GetLastSessionOrDiagnosticEvent;
import events.GetNewTelegraphPagesEvent;
import events.VerifyAndPublishLogEvent;
import handlers.EventManager;
import telegram.model.LogWithUrl;

import static telegram.MarkupFactory.REMOVE_MARKUP;

/**
 * Проверяет новые статьи в telegraph и публикует их
 */
@Component
public class CheckNewLogsCommand extends BotCommand {
    private final EventManager eventManager;

    public CheckNewLogsCommand(EventManager eventManager) {
        super("check_new", "Проверить новые записи");
        this.eventManager = eventManager;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chat.getId());
            message.setText("\uD83D\uDD0D Проверяю новые записи telegraph");
            message.setReplyMarkup(REMOVE_MARKUP);
            message.enableMarkdown(true);
            absSender.execute(message);

            // получаем последние записанные сессии
            CompletableFuture<List<String>> lastSessionOrDiagnosticResult = new CompletableFuture<>();
            GetLastSessionOrDiagnosticEvent event = new GetLastSessionOrDiagnosticEvent(lastSessionOrDiagnosticResult);
            eventManager.handleEvent(event);
            List<String> lastSessionOrDiagnostic = lastSessionOrDiagnosticResult.get();

            // получаем новые статьи
            CompletableFuture<List<LogWithUrl>> newPagesResult = new CompletableFuture<>();
            eventManager.handleEvent(new GetNewTelegraphPagesEvent(lastSessionOrDiagnostic, newPagesResult));
            List<LogWithUrl> newPages = newPagesResult.get();
            message = new SendMessage();
            message.setChatId(chat.getId());
            message.setText("Новых статей: " + newPages.size());
            absSender.execute(message);

            if (newPages.size() == 0) {
                return;
            }

            for (LogWithUrl logWithUrl : newPages) {
                CompletableFuture<Void> promise = new CompletableFuture<>();
                eventManager.handleEvent(new VerifyAndPublishLogEvent(logWithUrl, promise));
                promise.get(); // чтобы не посылать новые запросы, пока не принято решение по текущему
            }
        } catch (TelegramApiException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
