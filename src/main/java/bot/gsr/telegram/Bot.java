package bot.gsr.telegram;

import bot.gsr.telegram.commands.UpdateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public final class Bot extends TelegramLongPollingCommandBot {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);

    private final String botName;
    private final List<UpdateHandler> updateHandlers;
    private final Map<Long, String> lastCallbacks = new HashMap<>();

    public Bot(@Value("${telegram.bot.token}") String botToken,
               @Value("${telegram.bot.name}") String botName,
               List<BotCommand> commands,
               List<UpdateHandler> updateHandlers) {
        super(botToken);
        this.botName = botName;
        this.updateHandlers = updateHandlers;

        commands.forEach(this::register);
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        Long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            List<UpdateHandler> foundCommandHandlers = updateHandlers.stream()
                    .filter(commandHandler -> commandHandler.canProcessUpdate(update.getCallbackQuery().getData()))
                    .toList();

            if (foundCommandHandlers.size() != 1) {
                logger.error("Не удалось правильно определить commandHandler. Список подходящих: {}", foundCommandHandlers.stream()
                        .map(UpdateHandler::getCallbackName)
                        .collect(Collectors.joining(", ")));
                return;
            }

            lastCallbacks.put(chatId, update.getCallbackQuery().getData());
            foundCommandHandlers.get(0).processCallback(update, this);
        }

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            String lastCallback = lastCallbacks.get(chatId);

            List<UpdateHandler> foundCommandHandlers = updateHandlers.stream()
                    .filter(commandHandler -> commandHandler.canProcessUpdate(lastCallback))
                    .toList();

            if (foundCommandHandlers.size() != 1) {
                logger.error("Не удалось правильно определить commandHandler. Список подходящих: {}", foundCommandHandlers.stream()
                        .map(UpdateHandler::getCallbackName)
                        .collect(Collectors.joining(", ")));
                return;
            }

            foundCommandHandlers.get(0).processAction(lastCallback, update, this);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}