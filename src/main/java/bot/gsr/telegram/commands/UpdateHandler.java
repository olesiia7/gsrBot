package bot.gsr.telegram.commands;

import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.validation.constraints.NotNull;

import static bot.gsr.telegram.TelegramUtils.CALLBACK_DELIMITER;

public interface UpdateHandler {

    default boolean canProcessUpdate(@Nullable String callback) {
        return callback != null && callback.startsWith(getCallbackName());
    }

    void processCallback(@NotNull Update update, @NotNull AbsSender absSender);

    void processAction(@NotNull Update update, @NotNull AbsSender absSender);

    default String getCallback(@NotNull String stage) {
        return getCallbackName() + CALLBACK_DELIMITER + stage;
    }

    default String getSecondCallback(@NotNull String callback) {
        return callback.contains(CALLBACK_DELIMITER)
                ? callback.substring(callback.indexOf(CALLBACK_DELIMITER) + 1)
                : callback;
    }

    String getCallbackName();
}
