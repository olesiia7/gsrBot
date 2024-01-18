package bot.gsr.telegram.service;

import bot.gsr.events.AddToDbEvent;
import bot.gsr.events.PublishInChannelEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import bot.gsr.telegram.TelegramTest;
import bot.gsr.telegram.TelegramUtils;
import bot.gsr.telegram.model.LogWithUrl;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VerifyLogServiceTest extends TelegramTest {
    private static final String EDIT_TEXT = "✏️ Изменить";
    private static final String APPROVE_TEXT = "✅ Одобрить";
    private static final String DECLINE_TEXT = "❌ Пропустить";

    private static final LogWithUrl logWithUrl = new LogWithUrl(
            new Log(Date.valueOf(LocalDate.of(2023, Month.NOVEMBER, 21)), "desc", 1_000, Category.SESSION, SessionType.RANG),
            "url");
    private static final String CHAT_ID = "101";
    private static final EventManager eventManager = mock(EventManager.class);
    private static final VerifyLogService verifyLogService = new VerifyLogService(eventManager);
    private static final AbsSender absSender = mock(AbsSender.class);
    private static final MockedStatic<TelegramUtils> telegramUtilsMock = mockStatic(TelegramUtils.class);

    @BeforeEach
    void setUp() {
        telegramUtilsMock.when(() -> TelegramUtils.cleanText(any())).thenCallRealMethod();
        Message msg = mock(Message.class);
        when(msg.getMessageId()).thenReturn(10);
        telegramUtilsMock.when(() -> TelegramUtils.sendMessage(any(), notNull(), eq(true), any(), eq(absSender))).thenReturn(msg);
        Message msg2 = mock(Message.class);
        when(msg2.getMessageId()).thenReturn(20);
        telegramUtilsMock.when(() -> TelegramUtils.sendMessage(any(), any(), eq(false), any(), eq(absSender))).thenReturn(msg2);

        verifyLogService.verify(logWithUrl, CHAT_ID, absSender);
    }

    @AfterEach
    void tearDownEach() {
        reset(eventManager);
        telegramUtilsMock.reset();
    }

    @AfterAll
    public static void tearDown() {
        telegramUtilsMock.close();
    }

    @Test
    @DisplayName("Approve записи")
    void approveTest() {
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                any(),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup markup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(markup, APPROVE_TEXT));

        verifyLogService.processCallback(update, absSender);
        verify(eventManager, times(1)).handleEvent(new PublishInChannelEvent(logWithUrl));
        verify(eventManager, times(1)).handleEvent(new AddToDbEvent(logWithUrl.log()));
        verifyNoMoreInteractions(eventManager);

        String expected = """
                text
                                
                ✅ Одобрено""";
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq("2"),
                eq(3),
                eq(expected),
                eq(null),
                eq(true),
                eq(absSender)));
    }

    @Test
    @DisplayName("Decline записи")
    void declineTest() {
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                any(),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup markup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(markup, DECLINE_TEXT));

        verifyLogService.processCallback(update, absSender);
        verifyNoMoreInteractions(eventManager); // не отправляются события записи в базу данных и канал

        String expected = """
                text
                                
                ❌ Пропущено""";
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq("2"),
                eq(3),
                eq(expected),
                eq(null),
                eq(true),
                eq(absSender)));
    }

    @Test
    @DisplayName("Изменение категории")
    void editCategoryTest() {
        Update update = mock(Update.class);
        List<String> callbacks = getCallbacks();
        String editCallback = callbacks.get(2);
        setCallbackToUpdate(update, editCallback);

        verifyLogService.processCallback(update, absSender);

        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq("Выберите, что вы хотите изменить:"),
                markupCaptor.capture(),
                eq(false),
                eq("2"),
                eq(absSender)));
        InlineKeyboardMarkup editingMarkup = markupCaptor.getValue();

        // меняем категорию
        String callbackData = getCallBack(editingMarkup, "Категория");
        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        ArgumentCaptor<InlineKeyboardMarkup> editCategoryMarkupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                any(),
                eq(3),
                eq("Выберите новую категорию:"),
                editCategoryMarkupCaptor.capture(),
                eq(false),
                eq(absSender)), times(1));

        InlineKeyboardMarkup editCategoryMarkup = editCategoryMarkupCaptor.getValue();
        callbackData = getCallBack(editCategoryMarkup, Category.DIAGNOSTIC.getName());

        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.deleteMessage("2", 3, absSender));

        String expected = """
                Диагностика: *desc*
                _21\\.11\\.2023_
                0 ₽
                url""";
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                any(),
                any(),
                eq(expected),
                any(),
                eq(true),
                eq(absSender)), times(1));

        Log newLog = new Log(logWithUrl.log().date(), logWithUrl.log().description(), 0, Category.DIAGNOSTIC, null);
        checkBackAndSave(update, editCallback, callbacks.get(0), editingMarkup, editCategoryMarkup, "Категория", newLog, logWithUrl.url());
    }

    @Test
    @DisplayName("Изменение типа сессии")
    void editSessionTypeTest() {
        Update update = mock(Update.class);
        List<String> callbacks = getCallbacks();
        String editCallback = callbacks.get(2);
        setCallbackToUpdate(update, editCallback);

        verifyLogService.processCallback(update, absSender);

        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq("Выберите, что вы хотите изменить:"),
                markupCaptor.capture(),
                eq(false),
                eq("2"),
                eq(absSender)));
        InlineKeyboardMarkup editingMarkup = markupCaptor.getValue();

        String callbackData = getCallBack(editingMarkup, "Тип сессии");
        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        ArgumentCaptor<InlineKeyboardMarkup> editSessionTypeMarkupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                any(),
                eq(3),
                eq("Выберите новый тип сессии:"),
                editSessionTypeMarkupCaptor.capture(),
                eq(false),
                eq(absSender)), times(1));

        InlineKeyboardMarkup editSessionTypeMarkup = editSessionTypeMarkupCaptor.getValue();
        callbackData = getCallBack(editSessionTypeMarkup, SessionType.STRUCTURE_SCH2.getName());

        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.deleteMessage("2", 3, absSender), times(1));

        String expected = """
                Сессия: *desc*
                _21\\.11\\.2023, Структура в СЧ2_
                6 000 ₽
                url""";
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                any(),
                eq(10),
                eq(expected),
                any(),
                eq(true),
                eq(absSender)), times(1));

        Log newLog = new Log(logWithUrl.log().date(), logWithUrl.log().description(), 6_000, Category.SESSION, SessionType.STRUCTURE_SCH2);
        checkBackAndSave(update, editCallback, callbacks.get(0), editingMarkup, editSessionTypeMarkup, "Тип сессии", newLog, logWithUrl.url());
    }

    @Test
    @DisplayName("Изменение цены")
    void editPriceTest() {
        Update update = mock(Update.class);
        List<String> callbacks = getCallbacks();
        String editCallback = callbacks.get(2);
        setCallbackToUpdate(update, editCallback);

        verifyLogService.processCallback(update, absSender);

        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq("Выберите, что вы хотите изменить:"),
                markupCaptor.capture(),
                eq(false),
                eq("2"),
                eq(absSender)));
        InlineKeyboardMarkup editingMarkup = markupCaptor.getValue();

        String callbackData = getCallBack(editingMarkup, "Цена");
        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        ArgumentCaptor<InlineKeyboardMarkup> editPriceMarkupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                any(),
                eq(3),
                eq("Введите новую цену (цифры без знаков и пробелов)"),
                editPriceMarkupCaptor.capture(),
                eq(false),
                eq(absSender)), times(1));

        // только кнопка "Назад"
        InlineKeyboardMarkup editPriceMarkup = editPriceMarkupCaptor.getValue();
        assertEquals(1, editPriceMarkup.getKeyboard().size());
        assertEquals(1, editPriceMarkup.getKeyboard().get(0).size());


        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(2L);
        when(message.getMessageId()).thenReturn(3);
        when(message.getText()).thenReturn("10 000"); // неправильная цена
        when(update.getMessage()).thenReturn(message);
        verifyLogService.processAction(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq("Неправильный формат цены: введите цифры без знаков и пробелов"),
                eq(null),
                eq(false),
                eq("2"),
                eq(absSender)));

        when(message.getText()).thenReturn("10000"); // правильная цена
        when(update.getMessage()).thenReturn(message);
        verifyLogService.processAction(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.deleteMessage("2", 3, absSender), times(2));

        String expected = """
                Сессия: *desc*
                _21\\.11\\.2023, Ранг_
                10 000 ₽
                url""";
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                any(),
                eq(10),
                eq(expected),
                any(),
                eq(true),
                eq(absSender)), times(1));

        Log newLog = new Log(logWithUrl.log().date(), logWithUrl.log().description(), 10_000, logWithUrl.log().category(), logWithUrl.log().sessionType());
        checkBackAndSave(update, editCallback, callbacks.get(0), editingMarkup, editPriceMarkup, "Цена", newLog, logWithUrl.url());
    }

    private void checkBackAndSave(Update update,
                                  String editCallback,
                                  String approveCallback,
                                  InlineKeyboardMarkup editingMarkup,
                                  InlineKeyboardMarkup editSubjectMarkup,
                                  String btnName,
                                  Log newLog,
                                  String url) {
        // кнопка "назад"
        setCallbackToUpdate(update, editCallback);
        verifyLogService.processCallback(update, absSender);

        String callbackData = getCallBack(editingMarkup, btnName);
        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        callbackData = getCallBack(editSubjectMarkup, "Назад");
        setCallbackToUpdate(update, callbackData);
        verifyLogService.processCallback(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.deleteMessage("2", 3, absSender), atLeastOnce());

        // сохраняем изменения
        setCallbackToUpdate(update, approveCallback);
        verifyLogService.processCallback(update, absSender);
        LogWithUrl newLogWithUrl = new LogWithUrl(newLog, url);

        verify(eventManager, times(1)).handleEvent(new PublishInChannelEvent(newLogWithUrl));
        verify(eventManager, times(1)).handleEvent(new AddToDbEvent(newLog));
        verifyNoMoreInteractions(eventManager);
    }

    /**
     * @return список callback:
     * 0 – APPROVE
     * 1 – DECLINE
     * 2 – EDIT
     */
    private static List<String> getCallbacks() {
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                any(),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup markup = markupCaptor.getValue();
        return List.of(getCallBack(markup, APPROVE_TEXT), getCallBack(markup, DECLINE_TEXT), getCallBack(markup, EDIT_TEXT));
    }


}