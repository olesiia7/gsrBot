package bot.gsr.telegram.commands;

import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import bot.gsr.service.LogService;
import bot.gsr.telegram.TelegramUtils;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.telegram.service.VerifyLogService;
import bot.gsr.telegraph.TelegraphController;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;

class CheckNewLogsCommandTest {
    private static final LogService logService = mock(LogService.class);
    private static final TelegraphController telegraphController = mock(TelegraphController.class);
    private static final VerifyLogService verifyLogService = mock(VerifyLogService.class);
    private static final CheckNewLogsCommand command = new CheckNewLogsCommand(logService, telegraphController, verifyLogService);
    private static final AbsSender absSender = mock(AbsSender.class);
    private static final MockedStatic<TelegramUtils> telegramUtilsMock = mockStatic(TelegramUtils.class);

    private static final LogWithUrl logWithUrl = new LogWithUrl(
            new Log(Date.valueOf(LocalDate.of(2023, Month.NOVEMBER, 21)), "desc", 1_000, Category.SESSION, SessionType.RANG),
            "url");
    private static final LogWithUrl logWithUrl2 = new LogWithUrl(
            new Log(Date.valueOf(LocalDate.of(2023, Month.NOVEMBER, 20)), "desc2", 2_000, Category.EXPERT_SUPPORT, null),
            "url2");
    private static final List<LogWithUrl> pages = List.of(logWithUrl, logWithUrl2);
    private static final String CHAT_ID = "101";

    @BeforeEach
    void setUpEach() {
        when(telegraphController.getNewLogs(emptyList())).thenReturn(pages);
        telegramUtilsMock.when(() -> TelegramUtils.cleanText(any())).thenCallRealMethod();
        Message msg = mock(Message.class);
        when(msg.getMessageId()).thenReturn(10); // устанавливаем особое значение (чтобы проверить, что меняется verify значение)
        telegramUtilsMock.when(() -> TelegramUtils.sendMessage(any(), notNull(), eq(true), any(), eq(absSender))).thenReturn(msg);
    }

    @AfterEach
    void tearDownEach() {
        reset(logService, telegraphController, verifyLogService);
        telegramUtilsMock.reset();
    }

    @AfterAll
    public static void tearDown() {
        telegramUtilsMock.close();
    }

    @Test
    @DisplayName("Получение и вывод новых записей")
    void executeTest() {
        CompletableFuture<Void> verified1 = new CompletableFuture<>();
        when(verifyLogService.verify(logWithUrl, CHAT_ID, absSender)).thenReturn(verified1);
        CompletableFuture<Void> verified2 = new CompletableFuture<>();
        when(verifyLogService.verify(logWithUrl2, CHAT_ID, absSender)).thenReturn(verified2);

        Chat chat = mock(Chat.class);
        when(chat.getId()).thenReturn(Long.parseLong(CHAT_ID));
        command.execute(absSender, null, chat, null);

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                        eq("\uD83D\uDD0D Проверяю новые записи telegraph"),
                        any(),
                        eq(true),
                        eq(CHAT_ID),
                        eq(absSender)),
                times(1));
        verify(logService, times(1)).getLastPageNames();
        verify(telegraphController, times(1)).getNewLogs(emptyList());

        telegramUtilsMock.verify(() -> TelegramUtils.sendMessage(
                eq("Новых статей: 2"),
                eq(null),
                eq(false),
                eq(CHAT_ID),
                eq(absSender)
        ), times(1));

        verify(verifyLogService, times(1)).verify(logWithUrl, CHAT_ID, absSender);

        // ждем, пока не выполнился предыдущий лог
        verifyNoMoreInteractions(logService, telegraphController);
        telegramUtilsMock.verifyNoMoreInteractions();

        verified1.complete(null);

        verify(verifyLogService, times(1)).verify(logWithUrl2, CHAT_ID, absSender);

        verifyNoMoreInteractions(logService, telegraphController);
        telegramUtilsMock.verifyNoMoreInteractions();

        verified2.complete(null);

        // больше нет элементов
        verifyNoMoreInteractions(logService, telegraphController);
        telegramUtilsMock.verifyNoMoreInteractions();
    }
}