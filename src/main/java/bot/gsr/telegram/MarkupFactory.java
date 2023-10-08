package bot.gsr.telegram;

import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.SessionType;
import bot.gsr.telegram.model.Decision;
import bot.gsr.telegram.model.ReportType;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static bot.gsr.utils.Utils.MONTH_NAMES;

//ToDo: провести рефакторинг: вынести из класса готовые MARKUP
public final class MarkupFactory {
    private static final String APPROVE_BUTTON_TEXT = "✅ Одобрить";
    private static final String DECLINE_BUTTON_TEXT = "❌ Пропустить";
    private static final String EDIT_BUTTON_TEXT = "✏️ Изменить";

    public static final InlineKeyboardMarkup VERIFYING_MARKUP = getVerifyingMarkup();

    public static final String EDIT_CATEGORY = "Категория";
    public static final String EDIT_SESSION_TYPE = "Тип сессии";
    public static final String EDIT_SESSION_PRICE = "Цена";
    public static final String EDIT_FINISHED = "Назад";

    public static final ReplyKeyboardMarkup EDITING_MARKUP = getReplyMarkup(EDIT_SESSION_PRICE, EDIT_CATEGORY, EDIT_SESSION_TYPE, EDIT_FINISHED);
    public static final ReplyKeyboardMarkup BACK_MARKUP = getReplyMarkup(EDIT_FINISHED);

    public static final ReplyKeyboardMarkup EDIT_CATEGORY_MARKUP = getEditCategoryMarkup();
    public static final ReplyKeyboardMarkup EDIT_SESSION_TYPE_MARKUP = getEditSessionTypeMarkup();
    public static final ReplyKeyboardMarkup MONTHS_MARKUP = getMonthsMarkup();
    public static final ReplyKeyboardMarkup REPORT_TYPE_MARKUP = getReportType();

    public static final ReplyKeyboardRemove REMOVE_MARKUP = removeMarkup();

    private static ReplyKeyboardRemove removeMarkup() {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        return remove;
    }

    private static ReplyKeyboardMarkup getEditCategoryMarkup() {
        List<String> buttons = Arrays.stream(Category.values())
                .map(Category::getName)
                .collect(Collectors.toList());
        buttons.add(EDIT_FINISHED);
        return getReplyMarkup(buttons.toArray(new String[0]));
    }

    private static ReplyKeyboardMarkup getEditSessionTypeMarkup() {
        List<String> buttons = Arrays.stream(SessionType.values())
                .map(SessionType::getName)
                .collect(Collectors.toList());
        buttons.add(EDIT_FINISHED);
        return getReplyMarkup(buttons.toArray(new String[0]));
    }

    private static ReplyKeyboardMarkup getMonthsMarkup() {
        return getReplyMarkup(MONTH_NAMES.toArray(new String[0]));
    }

    private static ReplyKeyboardMarkup getReportType() {
        return getReplyMarkup(Arrays.stream(ReportType.values())
                .map(ReportType::getName)
                .toArray(String[]::new));
    }

    public static ReplyKeyboardMarkup getReplyMarkup(String... buttons) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        for (String button : buttons) {
            KeyboardRow row = new KeyboardRow();
            row.add(button);
            keyboardRows.add(row);
        }
        markup.setKeyboard(keyboardRows);
        return markup;
    }

    private static InlineKeyboardMarkup getVerifyingMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row1btns = new ArrayList<>();
        InlineKeyboardButton approveBtn = new InlineKeyboardButton();
        approveBtn.setText(APPROVE_BUTTON_TEXT);
        approveBtn.setCallbackData(Decision.APPROVE.toString());
        row1btns.add(approveBtn);

        InlineKeyboardButton declineBtn = new InlineKeyboardButton();
        declineBtn.setText(DECLINE_BUTTON_TEXT);
        declineBtn.setCallbackData(Decision.DECLINE.toString());

        row1btns.add(declineBtn);

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText(EDIT_BUTTON_TEXT);
        editBtn.setCallbackData(Decision.EDIT.toString());
        row1btns.add(editBtn);

        buttons.add(row1btns);
        markup.setKeyboard(buttons);
        return markup;
    }

}
