package bot.gsr.telegram;

import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.validation.constraints.NotNull;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class TelegramTest {
    protected static Integer CALLBACK_MESSAGE_ID = 3;
    protected static String CALLBACK_CHAT_ID = "2";

    protected static void setCallbackToUpdate(@NotNull Update update, @NotNull String callback) {
        CallbackQuery query = mock(CallbackQuery.class);
        when(update.getCallbackQuery()).thenReturn(query);
        when(query.getData()).thenReturn(callback);

        Message message = mock(Message.class);
        when(query.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(Long.parseLong(CALLBACK_CHAT_ID));
        when(message.getMessageId()).thenReturn(CALLBACK_MESSAGE_ID);
        when(message.getText()).thenReturn("text");
    }

    @Nullable
    protected static String getCallBack(@NotNull InlineKeyboardMarkup markup, @NotNull String btnName) {
        return markup.getKeyboard().stream()
                .flatMap(List::stream)
                .filter(button -> btnName.equals(button.getText()))
                .findFirst()
                .map(InlineKeyboardButton::getCallbackData)
                .orElse(null);
    }
}
