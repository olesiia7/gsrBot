package bot.gsr.telegram.commands;


import bot.gsr.SQLite.DbController;
import bot.gsr.SQLite.LogsFilter;
import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.Log;
import bot.gsr.SQLite.model.SessionType;
import bot.gsr.events.GetMonthlyReportEvent;
import bot.gsr.events.SendMeTelegramMessageEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.telegram.AnswerListener;
import bot.gsr.telegram.MarkupFactory;
import bot.gsr.telegram.model.CategorySummary;
import bot.gsr.telegram.model.ReportType;
import bot.gsr.telegram.model.YearMonth;
import bot.gsr.utils.Utils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.sql.SQLException;
import java.util.List;

import static bot.gsr.telegram.MarkupFactory.REMOVE_MARKUP;
import static bot.gsr.telegram.MarkupFactory.REPORT_TYPE_MARKUP;

@Component
public class QueryCommand extends BotCommand {
    private final EventManager manager;
    private final DbController db;

    public QueryCommand(EventManager manager, DbController db) {
        super("query", "Посмотреть записи");
        this.manager = manager;
        this.db = db;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String text = "Выберите тип отчета";
        AnswerListener listener = answer -> {
            ReportType reportType = ReportType.findByName((String) answer);
            switch (reportType) {
                case LAST_ALL -> sendLastAllReport();
                case MONTHLY -> sendMonthlyReport();
                case MONEY_BY_MONTH -> sendMoneyByMonthReport();
                case MONEY_BY_CATEGORY -> sendMoneyByCategoryReport();
            }
        };
        SendMeTelegramMessageEvent event = new SendMeTelegramMessageEvent(text, REPORT_TYPE_MARKUP, listener, false);
        manager.handleEvent(event);
    }

    private void sendLastAllReport() {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append("*Последняя сессия:*\n");
            LogsFilter.Builder builder = new LogsFilter.Builder();
            builder.setCategory(Category.SESSION);
            List<Log> lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последняя диагностика:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.DIAGNOSTIC);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последняя ранговая:*\n");
            builder = new LogsFilter.Builder();
            builder.setSessionType(SessionType.RANG);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Последняя ранговая в СЧ1:*\n");
            builder = new LogsFilter.Builder();
            builder.setSessionType(SessionType.RANG_SCH1);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            builder = new LogsFilter.Builder();
            builder.setSessionType(SessionType.RANG_SCH2);
            lastRecords = db.getLastRecords(1, builder.build());
            if (!lastRecords.isEmpty()) {
                sb.append("*Последняя ранговая в СЧ2:*\n");
                sb.append(toString(lastRecords.get(0))).append("\n\n");
            }

            sb.append("*Последний продукт GSR:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.GSR_PRODUCT);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Оплата экспертного:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.EXPERT_SUPPORT);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            sb.append("*Оплата 1+:*\n");
            builder = new LogsFilter.Builder();
            builder.setCategory(Category.ONE_PLUS);
            lastRecords = db.getLastRecords(1, builder.build());
            sb.append(toString(lastRecords.get(0))).append("\n\n");

            SendMeTelegramMessageEvent event = new SendMeTelegramMessageEvent(sb.toString(), REMOVE_MARKUP, null, true);
            manager.handleEvent(event);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendMonthlyReport() {
        try {
            String text = "Выберите период:";
            // получаем список всех месяцев в gsr
            List<YearMonth> allPeriods = db.getAllPeriods();
            List<String> periods = allPeriods.stream()
                    .map(yearMonth -> Utils.getMonth(yearMonth.month() - 1) + " " + yearMonth.year())
                    .toList();
            ReplyKeyboardMarkup markup = MarkupFactory.getReplyMarkup(periods.toArray(String[]::new));

            AnswerListener listener = answer -> {
                String answerDate = (String) answer;
                String[] parts = answerDate.split(" ");
                String month = String.format("%02d", Utils.getMonthNumber(parts[0]) + 1);
                String year = parts[1];
                String period = year + "-" + month;
                List<CategorySummary> categorySummary = db.getCategorySummary(period);

                String sb = "*Отчёт за " + answerDate + ":*\n\n" +
                        getReportByCategories(categorySummary, true);

                SendMeTelegramMessageEvent event = new SendMeTelegramMessageEvent(sb, REMOVE_MARKUP, null, true);
                manager.handleEvent(event);
            };
            SendMeTelegramMessageEvent event = new SendMeTelegramMessageEvent(text, markup, listener, false);
            manager.handleEvent(event);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getReportByCategories(List<CategorySummary> categorySummary, boolean addTotalCount) {
        //ToDo: добавить разделение сессий по подтипам
        //Сессии(13): 57 200 ₽
        //Диагностика(1): 0 ₽
        //Всего потрачено: 57 200 ₽
        StringBuilder sb = new StringBuilder();
        int totalPrice = 0;
        for (CategorySummary summary : categorySummary) {
            sb.append("*").append(summary.category().getName()).append("*")
                    .append(" (").append(summary.count()).append("): ")
                    .append(Utils.formatPrice(summary.priceSum())).append("\n");
            totalPrice += summary.priceSum();
        }
        if (addTotalCount) {
            sb.append("\n").append("*Всего потрачено:* ").append(Utils.formatPrice(totalPrice));
        }
        return sb.toString();
    }

    // посмотреть отчеты за текущий месяц, 3, 6 месяцев или произвольно
    private void sendMoneyByMonthReport() {
        manager.handleEvent(new GetMonthlyReportEvent());
    }

    private void sendMoneyByCategoryReport() throws SQLException {
        List<CategorySummary> categorySummary = db.getCategorySummary(null);

        String sb = "*Отчёт по категориям:*\n\n" +
                getReportByCategories(categorySummary, true);

        SendMeTelegramMessageEvent event = new SendMeTelegramMessageEvent(sb, REMOVE_MARKUP, null, true);
        manager.handleEvent(event);
    }

    private String toString(Log log) {
        StringBuilder sb = new StringBuilder();
        sb.append(Utils.getDate(log.date())).append(" ");
        sb.append(log.description());
        if (log.price() != 0) {
            sb.append(", ").append(Utils.formatPrice(log.price()));
        }
        return sb.toString();
    }
}