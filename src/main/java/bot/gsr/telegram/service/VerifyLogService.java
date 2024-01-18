package bot.gsr.telegram.service;

import bot.gsr.events.AddToDbEvent;
import bot.gsr.events.PublishInChannelEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import bot.gsr.telegram.commands.UpdateHandler;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
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
public class VerifyLogService implements UpdateHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Set<Integer> msgToDelete = new HashSet<>();

    private final InlineKeyboardMarkup verifyingMarkup = getVerifyingMarkup();
    private final InlineKeyboardMarkup editingMarkup = getEditingMarkup();
    public final InlineKeyboardMarkup editCategoryMarkup = getEditCategoryMarkup();
    public final InlineKeyboardMarkup editSessionTypeMarkup = getEditSessionTypeMarkup();
    public final InlineKeyboardMarkup editingFinishedMarkup = getEditingFinished();

    private final EventManager eventManager;

    private CompletableFuture<Void> result;
    private Integer lastVerifyingMsg;
    private Integer lastEditingMsg;
    private LogWithUrl currentLogWithUrl;

    private final Stage stage = new Stage();

    public VerifyLogService(EventManager eventManager) {
        this.eventManager = eventManager;
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

    public void verify(@NotNull Log log,
                       @NotNull String chatId,
                       @NotNull AbsSender absSender) {
        verify(new LogWithUrl(log, null), chatId, absSender);
    }

    private void updateLog(String chatId, Integer messageId, LogWithUrl newLog, AbsSender absSender) {
        stage.isFinished = true;
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
        String callback = getSecondCallback(update.getCallbackQuery().getData());
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        Integer callbackMsgId = update.getCallbackQuery().getMessage().getMessageId();

        if (stage.isFinished) {
            Decision decision = Decision.valueOf(callback);
            switch (decision) {
                case APPROVE, DECLINE -> {
                    if (decision == Decision.APPROVE) {
                        eventManager.handleEvent(new PublishInChannelEvent(currentLogWithUrl));
                        eventManager.handleEvent(new AddToDbEvent(currentLogWithUrl.log()));
                    }
                    addDecision(update.getCallbackQuery().getMessage(), decision, absSender);
                    currentLogWithUrl = null;
                    result.complete(null);
                    stage.isFinished = true;
                }
                case EDIT -> {
                    stage.isFinished = false;
                    stage.get();
                    stage.item = Item.EDIT_ATTRIBUTE;
                    processGetters(chatId, callbackMsgId, absSender);
                }
            }
        } else if (callback.equals(BACK_TEXT)) {
            stage.isFinished = true;
            deleteMessage(chatId, callbackMsgId, absSender);
        } else {
            switch (stage.method) {
                case GET -> processGetters(chatId, callbackMsgId, absSender);
                case SET -> processSetters(callback, chatId, callbackMsgId, absSender);
            }
        }
    }

    @Override
    public void processAction(Update update, AbsSender absSender) {
        if (stage.item != Item.PRICE || stage.method != Method.SET) {
            logger.error("{} {} не может быть обработан в processAction", stage.method, stage.item);
            return;
        }
        String text = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        Integer messageId = update.getMessage().getMessageId();
        processSetters(text, chatId, messageId, absSender);
    }

    private void processGetters(@NotNull String chatId, @Nullable Integer callbackMsgId, @NotNull AbsSender absSender) {
        if (stage.method != Method.GET) {
            logger.error("{} не может быть обработан в processAction", stage.method);
        }
        switch (stage.item) {
            case EDIT_ATTRIBUTE -> {
                String text = "Выберите, что вы хотите изменить:";
                Message message = sendMessage(text, editingMarkup, false, chatId, absSender);
                lastEditingMsg = message.getMessageId();
            }
            case CATEGORY ->
                    editMessage(chatId, callbackMsgId, "Выберите новую категорию:", editCategoryMarkup, false, absSender);
            case SESSION_TYPE ->
                    editMessage(chatId, callbackMsgId, "Выберите новый тип сессии:", editSessionTypeMarkup, false, absSender);
            case PRICE ->
                    editMessage(chatId, callbackMsgId, "Введите новую цену (цифры без знаков и пробелов)", editingFinishedMarkup, false, absSender);
            default -> logger.error("{} не может быть обработан в processGetters", stage.item);
        }
        stage.set();
    }

    private void processSetters(@NotNull String callbackOrText, @NotNull String chatId, @Nullable Integer callbackOrMsgId, @NotNull AbsSender absSender) {
        if (stage.method != Method.SET) {
            logger.error("{} не может быть обработан в processSetters", stage.method);
        }
        if (callbackOrText.equals(BACK_TEXT)) {
            stage.isFinished = true;
            deleteMessage(chatId, callbackOrMsgId, absSender);
            return;
        }

        switch (stage.item) {
            case EDIT_ATTRIBUTE -> {
                Item item = Item.valueOf(callbackOrText);
                stage.get();
                stage.item = item;
                processGetters(chatId, callbackOrMsgId, absSender);
            }
            case CATEGORY -> {
                Category newCategory = Category.valueOf(callbackOrText);
                Log log = currentLogWithUrl.log();
                // если изменилась категория, может поменяться цена и подтип
                log = Utils.predictLog(log.description(), newCategory, log.date());
                LogWithUrl newLogWithUrl = new LogWithUrl(new Log(log.date(), log.description(), log.price(), newCategory, log.sessionType()), currentLogWithUrl.url());
                updateLog(chatId, callbackOrMsgId, newLogWithUrl, absSender);
            }
            case SESSION_TYPE -> {
                SessionType newSessionType = SessionType.valueOf(callbackOrText);
                LogWithUrl logWithUrl = currentLogWithUrl;
                Log log = logWithUrl.log();
                // если изменился тип сессии, то категория должна стать SESSION, цена может измениться
                int newPrice = Utils.getSessionTypePrice(newSessionType);
                LogWithUrl newLogWithUrl = new LogWithUrl(new Log(log.date(), log.description(), newPrice, Category.SESSION, newSessionType), logWithUrl.url());
                updateLog(chatId, callbackOrMsgId, newLogWithUrl, absSender);
            }
            case PRICE -> {
                try {
                    int newPrice = Integer.parseInt(callbackOrText);
                    LogWithUrl logWithUrl = currentLogWithUrl;
                    Log log = logWithUrl.log();
                    LogWithUrl newLogWithUrl = new LogWithUrl(new Log(log.date(), log.description(), newPrice, log.category(), log.sessionType()), logWithUrl.url());
                    msgToDelete.add(lastEditingMsg);
                    clearMsgToDelete(absSender, chatId);
                    updateLog(chatId, callbackOrMsgId, newLogWithUrl, absSender);
                } catch (NumberFormatException ex) {
                    Message message = sendMessage("Неправильный формат цены: введите цифры без знаков и пробелов", null, false, chatId, absSender);
                    logger.error("Введён неправильный формат цены (нужны цифры без знаков и пробелов): {}", callbackOrText);
                    msgToDelete.add(message.getMessageId());
                    msgToDelete.add(callbackOrMsgId);
                }
            }
            default -> logger.error("{} не может быть обработан в processSetters", stage.item);
        }
    }

    private static void clearMsgToDelete(AbsSender absSender, String chatId) {
        msgToDelete.forEach(id -> deleteMessage(chatId, id, absSender));
        msgToDelete.clear();
    }

    private void addDecision(Message message, Decision decision, AbsSender absSender) {
        String text = message.getText();
        text = addDecisionToMsg(text, decision);
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

    private static String addDecisionToMsg(String msg, @NotNull Decision phase) {
        msg += "\n\n";
        if (phase == Decision.APPROVE) {
            msg += "✅ Одобрено";
        } else if (phase == Decision.DECLINE) {
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
        approveBtn.setCallbackData(getCallback(Decision.APPROVE.name()));
        row1btns.add(approveBtn);

        InlineKeyboardButton declineBtn = new InlineKeyboardButton();
        declineBtn.setText(declineButtonText);
        declineBtn.setCallbackData(getCallback(Decision.DECLINE.toString()));

        row1btns.add(declineBtn);

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText(editButtonText);
        editBtn.setCallbackData(getCallback(Decision.EDIT.name()));
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
        buttons.add(Pair.of(editCategoryButtonText, getCallback(Item.CATEGORY.name())));
        buttons.add(Pair.of(editSessionTypeButtonText, getCallback(Item.SESSION_TYPE.name())));
        buttons.add(Pair.of(editSessionPriceButtonText, getCallback(Item.PRICE.name())));
        buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));

        return getInlineMarkup(buttons);
    }

    private InlineKeyboardMarkup getEditCategoryMarkup() {
        return createChooseCategoryMarkup(
                this::getCallback,
                getCallbackName(),
                Collections.emptyList());
    }

    private InlineKeyboardMarkup getEditSessionTypeMarkup() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        Arrays.stream(SessionType.values())
                .forEach(sessionType -> buttons.add(Pair.of(sessionType.getName(),
                        getCallback(sessionType.name()))));

        buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));
        return getInlineMarkup(buttons);
    }

    private InlineKeyboardMarkup getEditingFinished() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));
        return getInlineMarkup(buttons);
    }

    private enum Method {
        GET, // вывести сообщение для получения объекта
        SET // распарсить объект
    }

    private enum Item {
        EDIT_ATTRIBUTE,
        CATEGORY,
        SESSION_TYPE,
        PRICE
    }

    private enum Decision {
        APPROVE,
        DECLINE,
        EDIT
    }

    private static class Stage {
        VerifyLogService.Method method;
        Item item;
        boolean isFinished = true; // если true – то нет предыдущих стадий

        protected void set() {
            this.method = Method.SET;
        }

        protected void get() {
            this.method = Method.GET;
        }
    }
}
