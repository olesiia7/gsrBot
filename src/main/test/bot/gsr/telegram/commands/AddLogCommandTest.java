package bot.gsr.telegram.commands;

import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.telegram.TelegramTest;
import bot.gsr.telegram.TelegramUtils;
import bot.gsr.telegram.service.VerifyLogService;
import bot.gsr.utils.Utils;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static bot.gsr.utils.Utils.BACK_TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AddLogCommandTest extends TelegramTest {
    private static final VerifyLogService verifyLogService = mock(VerifyLogService.class);
    private static final MockedStatic<TelegramUtils> telegramUtilsMock = mockStatic(TelegramUtils.class);
    private static final AbsSender absSender = mock(AbsSender.class);
    private static final Chat chat = mock(Chat.class);
    private static final AddLogCommand command = new AddLogCommand(verifyLogService);

    private static final String CHAT_ID = "101";
    private static final String CHOOSE_CATEGORY_TEXT = "Выберите категорию добавляемой записи";
    private static final String CHOOSE_PERIOD_TEXT = "Выберите период оплаты экспертного сопровождения";
    private static final String CHOOSE_DESCRIPTION_TEXT = "*Категория*: %s\nВведите описание";


    @BeforeAll
    static void setUp() {
        when(chat.getId()).thenReturn(Long.parseLong(CHAT_ID));
    }

    @BeforeEach
    void setUpEach() {
        Chat chat = mock(Chat.class);
        when(chat.getId()).thenReturn(Long.parseLong(CHAT_ID));

        Message msg = mock(Message.class);
        when(msg.getMessageId()).thenReturn(20);
        telegramUtilsMock.when(() -> TelegramUtils.sendMessage(any(), any(), anyBoolean(), any(), eq(absSender))).thenReturn(msg);
    }

    @AfterEach
    void tearDownEach() {
        reset(verifyLogService);
        telegramUtilsMock.reset();
    }

    @AfterAll
    public static void tearDown() {
        telegramUtilsMock.close();
    }

    @Test
    @DisplayName("Добавление лога в БД")
    void createNewLogTest() {
        command.execute(absSender, null, chat, null);
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                any(),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup markup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(markup, Category.SESSION.getName()));

        telegramUtilsMock.verifyNoMoreInteractions();
        //ToDo дописать
    }

    @Test
    @DisplayName("Добавление оплаты экспертного в БД")
    void addExpertSupportTest() {
        command.execute(absSender, null, chat, null);
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq(CHOOSE_CATEGORY_TEXT),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup markup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(markup, Category.EXPERT_SUPPORT.getName()));
        command.processCallback(update, absSender);

        // выбор периода
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq(CALLBACK_CHAT_ID),
                eq(CALLBACK_MESSAGE_ID),
                eq(CHOOSE_PERIOD_TEXT),
                markupCaptor.capture(),
                eq(false),
                eq(absSender)));

        InlineKeyboardMarkup chooseMonthsMarkup = markupCaptor.getValue();

        List<String> expectedButtonTexts = new ArrayList<>(Utils.SHORT_MONTH_NAMES);
        expectedButtonTexts.add(BACK_TEXT);
        List<String> buttonTexts = chooseMonthsMarkup.getKeyboard().stream()
                .flatMap(List::stream)
                .map(InlineKeyboardButton::getText)
                .toList();
        assertEquals(expectedButtonTexts, buttonTexts);

        setCallbackToUpdate(update, getCallBack(chooseMonthsMarkup, "Авг"));
        command.processCallback(update, absSender);

        int year = LocalDate.now().getYear();
        Date date = Date.valueOf(LocalDate.of(year, 8, 1));
        String description = "Август " + year;
        Log log = new Log(date, description, 10_000, Category.EXPERT_SUPPORT, null);

        verify(verifyLogService, times(1)).verify(eq(log), any(), eq(absSender));

        telegramUtilsMock.verify(() -> TelegramUtils.deleteMessage(any(), any(), eq(absSender)));

        verifyNoMoreInteractions(verifyLogService);
        telegramUtilsMock.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Исключенные категории при добавлении лога")
    void excludedCategoriesTest() {
        command.execute(absSender, null, chat, null);
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq(CHOOSE_CATEGORY_TEXT),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup markup = markupCaptor.getValue();

        Set<Category> excludedCategory = Set.of(Category.ONE_PLUS, Category.PG1, Category.PG2, Category.SELF_SESSION);
        Set<String> expectedExcludedCategory = excludedCategory.stream().map(Category::getName).collect(Collectors.toSet());

        Set<String> notExcludedCategory = markup.getKeyboard().stream()
                .flatMap(List::stream)
                .filter(button -> !button.getText().equals(BACK_TEXT))
                .map(InlineKeyboardButton::getCallbackData)
                .map(command::getSecondCallback)
                .filter(expectedExcludedCategory::contains)
                .collect(Collectors.toSet());
        assertTrue(notExcludedCategory.isEmpty());
    }

    @Test
    @DisplayName("Возврат из категории – выход")
    void backFromCategoryTest() {
        command.execute(absSender, null, chat, null);
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq(CHOOSE_CATEGORY_TEXT),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup chooseCategoryMarkup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(chooseCategoryMarkup, BACK_TEXT));
        command.processCallback(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.deleteMessage(CALLBACK_CHAT_ID, CALLBACK_MESSAGE_ID, absSender));
        telegramUtilsMock.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Возврат из периода – категория")
    void backFromPeriodTest() {
        command.execute(absSender, null, chat, null);
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq(CHOOSE_CATEGORY_TEXT),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup chooseCategoryMarkup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(chooseCategoryMarkup, Category.EXPERT_SUPPORT.getName()));
        command.processCallback(update, absSender);

        // выбор периода
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq(CALLBACK_CHAT_ID),
                eq(CALLBACK_MESSAGE_ID),
                eq(CHOOSE_PERIOD_TEXT),
                markupCaptor.capture(),
                eq(false),
                eq(absSender)));

        setCallbackToUpdate(update, getCallBack(markupCaptor.getValue(), BACK_TEXT));
        command.processCallback(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq(CALLBACK_CHAT_ID),
                eq(CALLBACK_MESSAGE_ID),
                eq(CHOOSE_CATEGORY_TEXT),
                any(),
                eq(false),
                eq(absSender)));
        telegramUtilsMock.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("Возврат из описания – категория")
    void backFromDescriptionTest() {
        command.execute(absSender, null, chat, null);
        ArgumentCaptor<InlineKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq(CHOOSE_CATEGORY_TEXT),
                markupCaptor.capture(),
                eq(true),
                eq(CHAT_ID),
                eq(absSender)));
        InlineKeyboardMarkup chooseCategoryMarkup = markupCaptor.getValue();
        Update update = mock(Update.class);
        setCallbackToUpdate(update, getCallBack(chooseCategoryMarkup, Category.SESSION.getName()));
        command.processCallback(update, absSender);

        // получение текста для ввода описания
        String descriptionText = CHOOSE_DESCRIPTION_TEXT.replace("%s", Category.SESSION.getName());
        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq(CALLBACK_CHAT_ID),
                eq(CALLBACK_MESSAGE_ID),
                eq(descriptionText),
                markupCaptor.capture(),
                eq(true),
                eq(absSender)));

        setCallbackToUpdate(update, getCallBack(markupCaptor.getValue(), BACK_TEXT));
        command.processCallback(update, absSender);

        telegramUtilsMock.verify(() -> TelegramUtils.editMessage(
                eq(CALLBACK_CHAT_ID),
                eq(CALLBACK_MESSAGE_ID),
                eq(CHOOSE_CATEGORY_TEXT),
                any(),
                eq(false),
                eq(absSender)));
        telegramUtilsMock.verifyNoMoreInteractions();
    }

    //ToDo дописать тест выход из даты – описание
}