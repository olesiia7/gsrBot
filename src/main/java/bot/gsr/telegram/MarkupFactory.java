package bot.gsr.telegram;

import bot.gsr.model.Category;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static bot.gsr.telegram.TelegramUtils.CALLBACK_DELIMITER;
import static bot.gsr.utils.Utils.BACK_TEXT;

public final class MarkupFactory {
    public static final ReplyKeyboardRemove REMOVE_MARKUP = removeMarkup();

    private MarkupFactory() {
    }

    private static ReplyKeyboardRemove removeMarkup() {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        return remove;
    }

    public static InlineKeyboardMarkup getInlineMarkup(@NotNull List<Pair<String, String>> buttonsWithCallback) {
        return getInlineMarkup(buttonsWithCallback, 1);
    }

    /**
     * @param buttonsWithCallback список из названий кнопки и callback
     * @return {@link InlineKeyboardMarkup} c кнопками по одной в строке
     */
    public static InlineKeyboardMarkup getInlineMarkup(@NotNull List<Pair<String, String>> buttonsWithCallback, int buttonsPerRow) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> buttons = buttonsWithCallback.stream()
                .collect(Collectors.groupingBy(e -> buttonsWithCallback.indexOf(e) / buttonsPerRow))
                .values().stream()
                .map(sublist -> sublist.stream()
                        .map(pair -> {
                            InlineKeyboardButton button = new InlineKeyboardButton();
                            button.setText(pair.getKey());
                            button.setCallbackData(pair.getValue());
                            return button;
                        })
                        .toList())
                .toList();

        markup.setKeyboard(buttons);
        return markup;
    }

    public static InlineKeyboardMarkup getBackMarkup(@NotNull String callback) {
        List<Pair<String, String>> buttons = new ArrayList<>();
        buttons.add(Pair.of(BACK_TEXT, callback + CALLBACK_DELIMITER + BACK_TEXT));
        return getInlineMarkup(buttons);
    }

    public static InlineKeyboardMarkup createChooseCategoryMarkup(@NotNull UnaryOperator<String> getCallbackFunction,
                                                                  @Nullable String callback,
                                                                  @NotNull List<Category> excludeCategory) {
        List<Pair<String, String>> buttons = new ArrayList<>();
        Arrays.stream(Category.values())
                .filter(category -> !excludeCategory.contains(category))
                .forEach(category -> buttons.add(Pair.of(category.getName(), getCallbackFunction.apply(category.name()))));
        if (callback != null) {
            buttons.add(Pair.of(BACK_TEXT, callback + CALLBACK_DELIMITER + BACK_TEXT));
        }
        return getInlineMarkup(buttons);
    }
}
