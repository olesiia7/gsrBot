package events;

import javax.validation.constraints.NotNull;

import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import telegram.AnswerListener;

/**
 * Отправляет сообщение мне в телеграм.
 * Если сообщение форматированное, предварительно очищает его
 */
public record SendMeTelegramMessageEvent(@NotNull String message,
                                         @Nullable ReplyKeyboard keyboard,
                                         @Nullable AnswerListener listener,
                                         boolean formatted) implements Event {
}
