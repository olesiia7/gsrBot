package bot.gsr.telegram.service;

import bot.gsr.events.AddToDbEvent;
import bot.gsr.events.PublishInChannelEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import bot.gsr.telegram.commands.MultiPhase;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static bot.gsr.telegram.MarkupFactory.createChooseCategoryMarkup;
import static bot.gsr.telegram.MarkupFactory.getInlineMarkup;
import static bot.gsr.telegram.TelegramUtils.*;
import static bot.gsr.utils.Utils.BACK_TEXT;

@Component
// ToDo переработать
public class VerifyLogService implements MultiPhase {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Set<Integer> msgToDelete = new HashSet<>();

    private final InlineKeyboardMarkup verifyingMarkup = getVerifyingMarkup();
    private final InlineKeyboardMarkup editingMarkup = getEditingMarkup();
    public final InlineKeyboardMarkup editCategoryMarkup = getEditCategoryMarkup();
    public final InlineKeyboardMarkup editSessionTypeMarkup = getEditSessionTypeMarkup();
    public final InlineKeyboardMarkup editingFinishedMarkup = getEditingFinished();

    private final EventManager eventManager;

    private static CompletableFuture<Void> result;
    private static Integer lastVerifyingMsg;
    private static Integer lastEditingMsg;
    private static LogWithUrl currentLogWithUrl;

    public VerifyLogService(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    private enum Phase {
        APPROVE,
        EDIT,
        DECLINE,
        EDIT_CATEGORY,
        EDIT_SESSION_TYPE,
        EDIT_PRICE,
        EDIT_FINISHED
    }

    public CompletableFuture<Void> verify(@NotNull LogWithUrl logWithUrl,
                                          @NotNull String chatId,
                                          @NotNull AbsSender absSender) {
        result = new CompletableFuture<>();
        currentLogWithUrl = logWithUrl;
        String text = getVerifyingMsg(currentLogWithUrl);
        Message message = sendMessage(text, verifyingMarkup, true, chatId, absSender);
        lastVerifyingMsg = message.getMessageId();
        return result;
    }

    public CompletableFuture<Void> verify(@NotNull Log log,
                                          @NotNull String chatId,
                                          @NotNull AbsSender absSender) {
        return verify(new LogWithUrl(log, null), chatId, absSender);
    }

    private void changeVerifyingLog(String chatId, Integer messageId, LogWithUrl newLog, AbsSender absSender) {
        deleteMessage(chatId, messageId, absSender);
        if (!currentLogWithUrl.equals(newLog)) {
            currentLogWithUrl = newLog;

            String message = getVerifyingMsg(currentLogWithUrl);
            editMessage(chatId, lastVerifyingMsg, message, verifyingMarkup, true, absSender);
        }
    }

    @Override
    public String getCallbackName() {
        return "VERIFY_LOG";
    }

    @Override
    public void processCallback(Update update, AbsSender absSender) {
        String callback = update.getCallbackQuery().getData();
        Phase phase = getPhase(callback);
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        Integer callbackMsgId = update.getCallbackQuery().getMessage().getMessageId();
        switch (phase) {
            case APPROVE, DECLINE -> {
                if (phase == Phase.APPROVE) {
                    eventManager.handleEvent(new PublishInChannelEvent(currentLogWithUrl));
                    eventManager.handleEvent(new AddToDbEvent(currentLogWithUrl.log()));
                }
                addDecision(update.getCallbackQuery().getMessage(), phase, absSender);
                currentLogWithUrl = null;
                result.complete(null);
            }
            case EDIT -> {
                String text = "Выберите, что вы хотите изменить:";
                Message message = sendMessage(text, editingMarkup, false, chatId, absSender);
                lastEditingMsg = message.getMessageId();
            }
            case EDIT_CATEGORY -> {
                if (firstPhase(callback)) {
                    editMessage(chatId, callbackMsgId, "Выберите новую категорию:", editCategoryMarkup, false, absSender);
                    return;
                }
                Category newCategory = Category.valueOf(getSecondPhase(callback));
                Log log = currentLogWithUrl.log();
                // если изменилась категория, может поменяться цена и подтип
                log = Utils.predictLog(log.description(), newCategory, log.date());
                LogWithUrl newLogWithUrl = new LogWithUrl(new Log(log.date(), log.description(), log.price(), newCategory, log.sessionType()), currentLogWithUrl.url());
                changeVerifyingLog(chatId, callbackMsgId, newLogWithUrl, absSender);
            }
            case EDIT_SESSION_TYPE -> {
                if (firstPhase(callback)) {
                    editMessage(chatId, callbackMsgId, "Выберите новый тип сессии:", editSessionTypeMarkup, false, absSender);
                    return;
                }
                SessionType newSessionType = SessionType.valueOf(getSecondPhase(callback));
                LogWithUrl logWithUrl = currentLogWithUrl;
                Log log = logWithUrl.log();
                // если изменился тип сессии, то категория должна стать SESSION, цена может измениться
                int newPrice = Utils.getSessionTypePrice(newSessionType);
                LogWithUrl newLogWithUrl = new LogWithUrl(new Log(log.date(), log.description(), newPrice, Category.SESSION, newSessionType), logWithUrl.url());
                changeVerifyingLog(chatId, callbackMsgId, newLogWithUrl, absSender);
            }
            case EDIT_PRICE ->
                    editMessage(chatId, callbackMsgId, "Введите новую цену (цифры без знаков и пробелов)", editingFinishedMarkup, false, absSender);
            case EDIT_FINISHED -> deleteMessage(chatId, callbackMsgId, absSender);
        }
    }

    @Override
    public void processAction(String lastCallback, Update update, AbsSender absSender) {
        // ToDo удалить все сообщения про цену
        Phase phase = getPhase(lastCallback);
        String text = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        if (phase == Phase.EDIT_PRICE) {
            Integer messageId = update.getMessage().getMessageId();
            try {
                int newPrice = Integer.parseInt(text);
                LogWithUrl logWithUrl = currentLogWithUrl;
                Log log = logWithUrl.log();
                LogWithUrl newLogWithUrl = new LogWithUrl(new Log(log.date(), log.description(), newPrice, log.category(), log.sessionType()), logWithUrl.url());
                msgToDelete.add(lastEditingMsg);
                clearMsgToDelete(absSender, chatId);
                changeVerifyingLog(chatId, messageId, newLogWithUrl, absSender);
            } catch (NumberFormatException ex) {
                Message message = sendMessage("Неправильный формат цены: введите цифры без знаков и пробелов", null, false, chatId, absSender);
                logger.error("Введён неправильный формат цены (нужны цифры без знаков и пробелов): {}", text);
                msgToDelete.add(message.getMessageId());
                msgToDelete.add(messageId);
            }
        }
    }

    private static void clearMsgToDelete(AbsSender absSender, String chatId) {
        msgToDelete.forEach(id -> deleteMessage(chatId, id, absSender));
        msgToDelete.clear();
    }

    private Phase getPhase(String callback) {
        String[] split = callback.split(CALLBACK_DELIMITER);
        return Phase.valueOf(split[1]);
    }

    private void addDecision(Message message, Phase phase, AbsSender absSender) {
        String text = message.getText();
        text = addDecisionToMsg(text, phase);
        text = cleanText(text);

        editMessage(message.getChatId().toString(), message.getMessageId(), text, null, true, absSender);
    }

    /**
     * Сессия: Адекватизация др. Актуальное
     * 23.02.2023, Сессия по Судьбе Рода
     * 2 600 ₽
     * https://telegra.ph/Aktualnoe-dr-07-10
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    private static String getVerifyingMsg(LogWithUrl log) {
        String msg = log.log().category().getName() + ": *" + log.log().description() + "*\n" +
                "_" + Utils.getDate(log.log().date());
        if (log.log().sessionType() != null) {
            msg += ", " + log.log().sessionType().getName();
        }
        msg += "_\n" +
                Utils.formatPrice(log.log().price());
        if (log.url() != null && !log.url().isEmpty()) {
            msg += "\n" + log.url();
        }
        msg = cleanText(msg);
        return msg;
    }

    private static String addDecisionToMsg(String msg, @NotNull Phase phase) {
        msg += "\n\n";
        if (phase == Phase.APPROVE) {
            msg += "✅ Одобрено";
        } else if (phase == Phase.DECLINE) {
            msg += "❌ Пропущено";
        }
        return msg;
    }

    private InlineKeyboardMarkup getVerifyingMarkup() {
        String approveButtonText = "✅ Одобрить";
        String declineButtonText = "❌ Пропустить";
        String editButtonText = "✏️ Изменить";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row1btns = new ArrayList<>();
        InlineKeyboardButton approveBtn = new InlineKeyboardButton();
        approveBtn.setText(approveButtonText);
        approveBtn.setCallbackData(getCallback(Phase.APPROVE.name()));
        row1btns.add(approveBtn);

        InlineKeyboardButton declineBtn = new InlineKeyboardButton();
        declineBtn.setText(declineButtonText);
        declineBtn.setCallbackData(getCallback(Phase.DECLINE.toString()));

        row1btns.add(declineBtn);

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText(editButtonText);
        editBtn.setCallbackData(getCallback(Phase.EDIT.name()));
        row1btns.add(editBtn);

        buttons.add(row1btns);
        markup.setKeyboard(buttons);
        return markup;
    }

    private InlineKeyboardMarkup getEditingMarkup() {
        String editCategoryButtonText = "Категория";
        String editSessionTypeButtonText = "Тип сессии";
        String editSessionPriceButtonText = "Цена";

        List<Pair<String, String>> buttons = new ArrayList<>();
        buttons.add(Pair.of(editCategoryButtonText, getCallback(Phase.EDIT_CATEGORY.name())));
        buttons.add(Pair.of(editSessionTypeButtonText, getCallback(Phase.EDIT_SESSION_TYPE.name())));
        buttons.add(Pair.of(editSessionPriceButtonText, getCallback(Phase.EDIT_PRICE.name())));
        buttons.add(Pair.of(BACK_TEXT, getCallback(Phase.EDIT_FINISHED.name())));

        return getInlineMarkup(buttons);
    }

    private InlineKeyboardMarkup getEditCategoryMarkup() {
        return createChooseCategoryMarkup(
                category -> getCallback(Phase.EDIT_CATEGORY.name(), category),
                getCallbackName(),
                Collections.emptyList());
    }

    private InlineKeyboardMarkup getEditSessionTypeMarkup() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        Arrays.stream(SessionType.values())
                .forEach(sessionType -> buttons.add(Pair.of(sessionType.getName(),
                        getCallback(Phase.EDIT_SESSION_TYPE.name(), sessionType.name()))));

        buttons.add(Pair.of(BACK_TEXT, getCallback(Phase.EDIT_FINISHED.name())));
        return getInlineMarkup(buttons);
    }

    private InlineKeyboardMarkup getEditingFinished() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        buttons.add(Pair.of(BACK_TEXT, getCallback(Phase.EDIT_FINISHED.name())));
        return getInlineMarkup(buttons);
    }
}
