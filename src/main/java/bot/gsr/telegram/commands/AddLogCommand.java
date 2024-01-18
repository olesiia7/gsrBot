package bot.gsr.telegram.commands;

import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.telegram.service.VerifyLogService;
import bot.gsr.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.validation.constraints.NotNull;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

import static bot.gsr.telegram.MarkupFactory.*;
import static bot.gsr.telegram.TelegramUtils.*;

@Component
public class AddLogCommand extends BotCommand implements UpdateHandler {
    private static final Logger logger = LoggerFactory.getLogger(AddLogCommand.class);

    private final InlineKeyboardMarkup backMarkup = getBackMarkup(getCallbackName());
    private final InlineKeyboardMarkup chooseCategoryMarkup = getChooseCategoryMarkup();
    private final InlineKeyboardMarkup choosePeriodMarkup = getChoosePeriodMarkup();
    private final VerifyLogService verifyLogService;

    private final LogBuilder logBuilder = new LogBuilder();
    private Stage stage;
    private Integer firstMsg;
    private static final Set<Integer> msgsToDelete = new HashSet<>();

    public AddLogCommand(VerifyLogService verifyLogService) {
        super("add", "Добавить запись в БД");
        this.verifyLogService = verifyLogService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String chatId = chat.getId().toString();
        logBuilder.clear();
        firstMsg = null;
        stage = Stage.GET_CATEGORY;
        processGetters(chatId, null, absSender);
    }

    @Override
    public void processCallback(Update update, AbsSender absSender) {
        String callback = getSecondCallback(update.getCallbackQuery().getData());
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        Integer callbackMsgId = update.getCallbackQuery().getMessage().getMessageId();

        if (callback.equals(Utils.BACK_TEXT)) {
            setPrevStage();
            if (stage == Stage.CANCELED) {
                msgsToDelete.add(callbackMsgId);
                clearMsgToDelete(chatId, absSender);
                return;
            }
            processGetters(chatId, callbackMsgId, absSender);
            return;
        }

        switch (stage) {
            case SET_CATEGORY -> {
                logBuilder.category = Category.valueOf(callback);
                stage = logBuilder.category == Category.EXPERT_SUPPORT
                        ? Stage.GET_PERIOD
                        : Stage.GET_DESCRIPTION;
                processGetters(chatId, callbackMsgId, absSender);
            }
            case SET_PERIOD -> {
                int month = Integer.parseInt(callback);
                int year = LocalDate.now().getYear();
                Date date = Utils.getDate(1, month, year);
                String monthInRussian = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru", "RU"));
                monthInRussian = monthInRussian.substring(0, 1).toUpperCase() + monthInRussian.substring(1);
                String description = monthInRussian + " " + year;

                deleteMessage(chatId, callbackMsgId, absSender);
                Log log = Utils.predictLog(description, logBuilder.category, date);
                sendToVerify(log, chatId, absSender);
            }
            default -> logger.error("{} не может быть обработана в processCallback", stage);
        }
    }

    @Override
    // вводятся только описание и дата
    //ToDo добавить запрос цены, если продукт гср и сопутствующие расходы
    public void processAction(String lastCallback, Update update, AbsSender absSender) {
        String messageText = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        Integer messageId = update.getMessage().getMessageId();
        switch (stage) {
            case SET_DESCRIPTION -> {
                logBuilder.description = messageText;
                deleteMessage(chatId, messageId, absSender);
                stage = Stage.GET_DATE;
                processGetters(chatId, null, absSender);
            }
            case SET_DATE -> {
                try {
                    // Это последний шаг
                    Date date = Utils.toDate(messageText);
                    msgsToDelete.add(messageId);
                    msgsToDelete.add(firstMsg);
                    Log log = Utils.predictLog(logBuilder.description, logBuilder.category, date);
                    sendToVerify(log, chatId, absSender);
                } catch (DateTimeParseException ex) {
                    // если ошибка – удаляем предыдущие сообщения,
                    // пишем сообщения о неправильном вводе
                    // неправильный ввод + сообщение об этом сохраняем в msgToDelete
                    clearMsgToDelete(chatId, absSender);
                    Message message = sendMessage("Неправильный формат ввода.\nВведите дату в формате dd.MM.yyyy",
                            null, false, chatId, absSender);
                    msgsToDelete.add(messageId);
                    msgsToDelete.add(message.getMessageId());
                }
            }
            default -> logger.error("{} не может обрабатываться в processAction", stage);
        }
    }

    private void processGetters(@NotNull String chatId, @Nullable Integer callbackMsgId, @NotNull AbsSender absSender) {
        switch (stage) {
            case GET_CATEGORY -> {
                String text = "Выберите категорию добавляемой записи";
                if (firstMsg == null) { // первый раз
                    Message message = sendMessage(text, chooseCategoryMarkup, true, chatId, absSender);
                    firstMsg = message.getMessageId();
                } else {
                    editMessage(chatId, callbackMsgId, text, chooseCategoryMarkup, false, absSender);
                }
                stage = Stage.SET_CATEGORY;
            }
            case GET_DESCRIPTION -> {
                String text = String.format("*Категория*: %s%nВведите описание", logBuilder.category.getName());
                editMessage(chatId, callbackMsgId, text, backMarkup, true, absSender);
                stage = Stage.SET_DESCRIPTION;
            }
            case GET_DATE -> {
                String text = "*Категория*: " + logBuilder.category.getName() +
                        "\n*Описание*: _" + logBuilder.description +
                        "_\nВведите дату в формате dd.MM.yyyy";
                text = cleanText(text);
                editMessage(chatId, firstMsg, text, backMarkup, true, absSender);
                stage = Stage.SET_DATE;
            }
            case GET_PERIOD -> {
                editMessage(chatId, callbackMsgId, "Выберите период оплаты экспертного сопровождения", choosePeriodMarkup, false, absSender);
                stage = Stage.SET_PERIOD;
            }
            default -> logger.error("{} не может быть обработан в processAction", stage);
        }
    }

    //ToDo поменять на массовое удаление, когда подъедет новый релиз
    // https://github.com/rubenlagus/TelegramBots (6.8 старый)
    private static void clearMsgToDelete(String chatId, AbsSender absSender) {
        msgsToDelete.forEach(id -> deleteMessage(chatId, id, absSender));
        msgsToDelete.clear();
    }

    @Override
    public String getCallbackName() {
        return "ADD_LOG";
    }

    private InlineKeyboardMarkup getChooseCategoryMarkup() {
        return createChooseCategoryMarkup(
                this::getCallback,
                getCallbackName(),
                List.of(Category.ONE_PLUS, Category.SELF_SESSION, Category.PG1, Category.PG2));
    }

    private InlineKeyboardMarkup getChoosePeriodMarkup() {
        int i = 1;
        List<Pair<String, String>> buttons = new ArrayList<>();
        for (String month : Utils.SHORT_MONTH_NAMES) {
            buttons.add(Pair.of(month, getCallback(String.valueOf(i++))));
        }
        buttons.add(Pair.of(Utils.BACK_TEXT, getCallback(Utils.BACK_TEXT)));
        return getInlineMarkup(buttons, 4);
    }

    private void sendToVerify(Log log, String chatId, AbsSender absSender) {
        stage = Stage.DONE;
        logBuilder.clear();
        firstMsg = null;
        clearMsgToDelete(chatId, absSender);
        verifyLogService.verify(log, chatId, absSender);
    }

    private enum Stage {
        GET_CATEGORY, // сообщение с выбором категории
        SET_CATEGORY, // распарсить выбранную категорию
        GET_PERIOD,
        SET_PERIOD,
        GET_DESCRIPTION,
        SET_DESCRIPTION,
        GET_DATE,
        SET_DATE,
        DONE,
        CANCELED
    }

    private void setPrevStage() {
        this.stage = switch (stage) {
            case GET_CATEGORY, SET_CATEGORY -> Stage.CANCELED;
            case SET_DESCRIPTION, SET_PERIOD -> Stage.GET_CATEGORY;
            case SET_DATE -> Stage.GET_DESCRIPTION;
            default -> {
                logger.error("У {} нет предыдущей фазы", stage);
                throw new IllegalArgumentException(String.format("У %s нет предыдущей фазы", stage));
            }
        };
    }

    private static class LogBuilder {
        Category category;
        String description;

        private void clear() {
            category = null;
            description = null;
        }
    }
}
