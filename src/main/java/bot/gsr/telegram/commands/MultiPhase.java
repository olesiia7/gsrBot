package bot.gsr.telegram.commands;

import javax.validation.constraints.NotNull;

import static bot.gsr.telegram.TelegramUtils.CALLBACK_DELIMITER;

@Deprecated
public interface MultiPhase extends UpdateHandler {

    default boolean firstPhase(String callback) {
        return callback.split(CALLBACK_DELIMITER).length == 2;
    }

    default String getSecondPhase(@NotNull String callback) {
        String[] split = callback.split(CALLBACK_DELIMITER);
        return split[2];
    }

    default String getCallback(@NotNull String phase, @NotNull String secondPhase) {
        return getCallbackName() + CALLBACK_DELIMITER + phase + CALLBACK_DELIMITER + secondPhase;
    }
}
