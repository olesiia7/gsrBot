package bot.gsr.telegram.commands;

import bot.gsr.service.LogService;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.telegram.service.VerifyLogService;
import bot.gsr.telegraph.TelegraphController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static bot.gsr.telegram.MarkupFactory.REMOVE_MARKUP;
import static bot.gsr.telegram.TelegramUtils.sendMessage;

/**
 * Проверяет новые статьи в telegraph
 */
@Component
public class CheckNewLogsCommand extends BotCommand {
    private static final Logger logger = LoggerFactory.getLogger(CheckNewLogsCommand.class);
    private final LogService logService;
    private final TelegraphController telegraphController;
    private final VerifyLogService verifyLogService;

    private final List<LogWithUrl> pages = new ArrayList<>();

    public CheckNewLogsCommand(LogService logService,
                               TelegraphController telegraphController,
                               VerifyLogService verifyLogService) {
        super("check_new", "Проверить новые записи");
        this.logService = logService;
        this.telegraphController = telegraphController;
        this.verifyLogService = verifyLogService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String text = "\uD83D\uDD0D Проверяю новые записи telegraph";
        String chatId = chat.getId().toString();
        sendMessage(text, REMOVE_MARKUP, true, chatId, absSender);

        // получаем последние записанные сессии
        List<String> lastPageNames = logService.getLastPageNames();

        // получаем новые статьи
        pages.clear();
        pages.addAll(telegraphController.getNewLogs(lastPageNames));

        text = "Новых статей: " + pages.size();
        sendMessage(text, null, false, chatId, absSender);

        verifyNext(chatId, absSender);
    }

    private void verifyNext(@NotNull String chatId, @NotNull AbsSender absSender) {
        if (pages.isEmpty()) {
            return;
        }

        LogWithUrl logWithUrl = pages.get(0);

        CompletableFuture<Void> verify = verifyLogService.verify(logWithUrl, chatId, absSender);
        verify.whenComplete((unused, throwable) -> {
            if (throwable != null) {
                logger.error("Ошибка при verify: {}", throwable.toString());
                return;
            }
            pages.remove(0);
            verifyNext(chatId, absSender);
        });
    }
}
