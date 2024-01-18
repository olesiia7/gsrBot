package bot.gsr.telegram.commands;


import bot.gsr.service.LogService;
import bot.gsr.telegram.ReportUtils;
import bot.gsr.telegram.model.ReportType;
import bot.gsr.telegram.model.YearMonth;
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
import java.util.*;

import static bot.gsr.telegram.MarkupFactory.getInlineMarkup;
import static bot.gsr.telegram.TelegramUtils.*;
import static bot.gsr.utils.Utils.BACK_TEXT;
import static bot.gsr.utils.Utils.getShortMonth;

@Component
public class QueryCommand extends BotCommand implements UpdateHandler {
    private static final Logger logger = LoggerFactory.getLogger(QueryCommand.class);

    private static final Set<Integer> msgsToDelete = new HashSet<>();
    private static final String CHOOSE_PERIOD_TEXT = "Выберите период или введите нужное кол-во месяцев, \nгде 0 - текущий месяц, 1 – текущий + предыдущий и т.д.";
    private final InlineKeyboardMarkup reportTypeMarkup = getReportType();
    private final InlineKeyboardMarkup periodMarkup = getPeriodMarkup();
    private final InlineKeyboardMarkup formMarkup = getFormMarkup();
    private final LogService logService;

    private Stage stage;
    private Integer firstMsg;

    // для MONTHLY
    private int monthlyYear;
    private List<YearMonth> cachedAllPeriods;

    // для MONEY_BY_MONTH
    private int moneyByMonthMonth;

    public QueryCommand(LogService logService) {
        super("query", "Посмотреть записи");
        this.logService = logService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        firstMsg = null;
        stage = Stage.GET_TYPE;
        processGetters(chat.getId().toString(), null, absSender);
    }

    private void processGetters(@NotNull String chatId, @Nullable Integer callbackMsgId, @NotNull AbsSender absSender) {
        switch (stage) {
            case GET_TYPE -> {
                clearMsgToDelete(chatId, absSender);
                String text = "Выберите тип отчета";
                if (firstMsg == null) { // первый раз
                    Message message = sendMessage(text, reportTypeMarkup, false, chatId, absSender);
                    firstMsg = message.getMessageId();
                } else {
                    editMessage(chatId, callbackMsgId, text, reportTypeMarkup, false, absSender);
                }
                stage = Stage.SET_TYPE;
            }
            case GET_MONTHLY_YEAR -> {
                String text = "Выберите год, затем месяц:";
                // получаем список всех месяцев в gsr
                cachedAllPeriods = logService.getAllPeriods();

                List<Pair<String, String>> buttons = new ArrayList<>();
                cachedAllPeriods.stream() // уникальные годы
                        .map(YearMonth::year)
                        .distinct()
                        .sorted(Comparator.reverseOrder())
                        .forEach(year -> buttons.add(Pair.of(year.toString(), getCallback(year.toString()))));
                buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));

                InlineKeyboardMarkup chooseYearMarkup = getInlineMarkup(buttons, 2);
                stage = Stage.SET_MONTHLY_YEAR;
                editMessage(chatId, callbackMsgId, text, chooseYearMarkup, true, absSender);
            }
            case GET_MONTHLY_MONTH -> {
                String text = "*Год*: " + monthlyYear + "\nВыберите месяц:";
                List<Pair<String, String>> buttons = new ArrayList<>();
                cachedAllPeriods.stream()
                        .filter(yearMonth -> yearMonth.year() == monthlyYear)
                        .map(YearMonth::month)
                        .sorted()
                        .forEach(month -> buttons.add(Pair.of(getShortMonth(month), getCallback(month.toString()))));
                buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));

                InlineKeyboardMarkup chooseYearMarkup = getInlineMarkup(buttons, 4);
                stage = Stage.SET_MONTHLY_MONTH;
                editMessage(chatId, callbackMsgId, text, chooseYearMarkup, true, absSender);
            }
            case GET_MONEY_BY_MONTH_PERIOD -> {
                stage = Stage.SET_MONEY_BY_MONTH_PERIOD;
                editMessage(chatId, callbackMsgId, CHOOSE_PERIOD_TEXT, periodMarkup, false, absSender);
            }
            case GET_MONEY_BY_MONTH_FORM -> {
                String text = cleanText("""
                        *Период*: %s
                        Выберите форму отчёта:
                        Краткая – только общая сумма за месяц
                        Подробная – общая сумма за месяц + траты по категориям"""
                        .replace("%s", ReportUtils.declineMonth(moneyByMonthMonth)));

                stage = Stage.SET_MONEY_BY_MONTH_FORM;
                editMessage(chatId, callbackMsgId, text, formMarkup, true, absSender);
                clearMsgToDelete(chatId, absSender);
            }
            default -> logger.error("{} не может быть обработана в processGetters", stage);
        }
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
            case SET_TYPE -> {
                ReportType reportType = ReportType.valueOf(callback);
                switch (reportType) {
                    case LAST_ALL -> {
                        String text = ReportUtils.getLastAllReport(logService);
                        firstMsg = null;
                        editMessage(chatId, callbackMsgId, text, null, true, absSender);
                    }
                    case MONTHLY -> {
                        stage = Stage.GET_MONTHLY_YEAR;
                        processGetters(chatId, callbackMsgId, absSender);
                    }
                    case MONEY_BY_MONTH -> {
                        stage = Stage.GET_MONEY_BY_MONTH_PERIOD;
                        processGetters(chatId, callbackMsgId, absSender);
                    }
                    case MONEY_BY_CATEGORY -> {
                        String text = ReportUtils.getMoneyByCategoryReport(logService);
                        firstMsg = null;
                        editMessage(chatId, callbackMsgId, text, null, true, absSender);
                    }
                }
            }
            case SET_MONTHLY_YEAR -> {
                monthlyYear = Integer.parseInt(callback);
                stage = Stage.GET_MONTHLY_MONTH;
                processGetters(chatId, callbackMsgId, absSender);
            }
            case SET_MONTHLY_MONTH -> {
                int month = Integer.parseInt(callback);
                String text = ReportUtils.getMonthlyReport(logService, monthlyYear, month);
                editMessage(chatId, callbackMsgId, text, null, true, absSender);
            }
            case SET_MONEY_BY_MONTH_PERIOD -> {
                moneyByMonthMonth = switch (MoneyByMonthPeriods.valueOf(callback)) {
                    case CURRENT_MONTH -> 0;
                    case THREE_MONTHS -> 3;
                    case SIX_MONTHS -> 6;
                    case YEAR -> 12;
                };
                stage = Stage.GET_MONEY_BY_MONTH_FORM;
                processGetters(chatId, callbackMsgId, absSender);
            }
            case SET_MONEY_BY_MONTH_FORM -> {
                boolean extended = MoneyByMonthForm.valueOf(callback) == MoneyByMonthForm.EXTENDED;
                String text = ReportUtils.getMoneyByMonthReport(logService, moneyByMonthMonth, extended);
                editMessage(chatId, callbackMsgId, text, null, true, absSender);
            }
            default -> logger.error("{} не может быть обработана в processCallback", stage);
        }
    }

    private InlineKeyboardMarkup getPeriodMarkup() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        Arrays.stream(MoneyByMonthPeriods.values())
                .forEach(period -> buttons.add(Pair.of(period.getName(), getCallback(period.name()))));
        buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));
        return getInlineMarkup(buttons);
    }

    private InlineKeyboardMarkup getFormMarkup() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        Arrays.stream(MoneyByMonthForm.values())
                .forEach(form -> buttons.add(Pair.of(form.getName(), getCallback(form.name()))));
        buttons.add(Pair.of(BACK_TEXT, getCallback(BACK_TEXT)));
        return getInlineMarkup(buttons);
    }

    @Override
    public void processAction(Update update, AbsSender absSender) {
        if (stage != Stage.SET_MONEY_BY_MONTH_PERIOD) {
            logger.error("{} не может обрабатываться в processAction", stage);
            return;
        }
        String messageText = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        Integer messageId = update.getMessage().getMessageId();
        int months;
        try {
            months = Integer.parseInt(messageText);
        } catch (NumberFormatException ex) {
            months = -1;
        }
        if (months < 0) {
            String text = "Период, который вы выбрали (%s), не валидный.".replace("%s", messageText) +
                    "\nПожалуйста, выберите один из вариантов или введите положительное целое число.\n\n"
                    + CHOOSE_PERIOD_TEXT;
            editMessage(chatId, firstMsg, text, periodMarkup, false, absSender);
            deleteMessage(chatId, messageId, absSender);
            return;
        }
        msgsToDelete.add(messageId);
        moneyByMonthMonth = months;
        stage = Stage.GET_MONEY_BY_MONTH_FORM;
        processGetters(chatId, firstMsg, absSender);
    }

    private InlineKeyboardMarkup getReportType() {
        List<Pair<String, String>> buttons = new ArrayList<>();
        Arrays.stream(ReportType.values())
                .forEach(reportType -> buttons.add(Pair.of(reportType.getName(), getCallback(reportType.name()))));
        buttons.add(Pair.of(BACK_TEXT, getCallbackName() + CALLBACK_DELIMITER + BACK_TEXT));
        return getInlineMarkup(buttons);
    }

    private static void clearMsgToDelete(String chatId, AbsSender absSender) {
        msgsToDelete.forEach(id -> deleteMessage(chatId, id, absSender));
        msgsToDelete.clear();
    }

    @Override
    public String getCallbackName() {
        return "QUERY";
    }

    private enum MoneyByMonthPeriods {
        CURRENT_MONTH("Текущий месяц"),
        THREE_MONTHS("3 месяца"),
        SIX_MONTHS("6 месяцев"),
        YEAR("Год");

        private final String name;

        MoneyByMonthPeriods(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private enum MoneyByMonthForm {
        SHORT("Краткая"), // только общая сумма за месяц
        EXTENDED("Расширенная"); // траты по категориям + общая сумма за месяц

        private final String name;

        MoneyByMonthForm(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private enum Stage {
        GET_TYPE, // тип отчёта
        SET_TYPE,

        // MONTHLY
        GET_MONTHLY_YEAR,
        SET_MONTHLY_YEAR,
        GET_MONTHLY_MONTH,
        SET_MONTHLY_MONTH,

        // MONEY_BY_MONTH
        GET_MONEY_BY_MONTH_PERIOD,
        SET_MONEY_BY_MONTH_PERIOD,
        GET_MONEY_BY_MONTH_FORM,
        SET_MONEY_BY_MONTH_FORM,

        CANCELED
    }

    private void setPrevStage() {
        stage = switch (stage) {
            case SET_TYPE -> Stage.CANCELED;
            case SET_MONTHLY_MONTH -> Stage.GET_MONTHLY_YEAR;
            case SET_MONTHLY_YEAR, SET_MONEY_BY_MONTH_PERIOD -> Stage.GET_TYPE;
            case SET_MONEY_BY_MONTH_FORM -> Stage.GET_MONEY_BY_MONTH_PERIOD;
            default -> {
                logger.error("У {} нет предыдущей фазы", stage);
                throw new IllegalArgumentException(String.format("У %s нет предыдущей фазы", stage));
            }
        };
    }
}
