package bot.gsr.telegram;

import bot.gsr.model.*;
import bot.gsr.service.LogService;
import bot.gsr.utils.Utils;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;

import static bot.gsr.telegram.TelegramUtils.cleanText;
import static bot.gsr.utils.Utils.getDate;

public final class ReportUtils {
    private ReportUtils() {
    }

    public static String getLastAllReport(@NotNull LogService logService) {
        StringBuilder sb = new StringBuilder();

        sb.append("*Последняя сессия:*\n");
        LogFilter.Builder builder = new LogFilter.Builder();
        builder.setCategory(Category.SESSION);
        List<Log> lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        sb.append("*Последняя диагностика:*\n");
        builder = new LogFilter.Builder();
        builder.setCategory(Category.DIAGNOSTIC);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        sb.append("*Последняя ранговая:*\n");
        builder = new LogFilter.Builder();
        builder.setSessionType(SessionType.RANG);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        sb.append("*Последняя ранговая в СЧ1:*\n");
        builder = new LogFilter.Builder();
        builder.setSessionType(SessionType.RANG_SCH1);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        builder = new LogFilter.Builder();
        builder.setSessionType(SessionType.RANG_SCH2);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        if (!lastRecords.isEmpty()) {
            sb.append("*Последняя ранговая в СЧ2:*\n");
            sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");
        }

        sb.append("*Последний продукт GSR:*\n");
        builder = new LogFilter.Builder();
        builder.setCategory(Category.GSR_PRODUCT);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        // ToDo убрать дату (по дескрипшну понятно)
        sb.append("*Оплата экспертного:*\n");
        builder = new LogFilter.Builder();
        builder.setCategory(Category.EXPERT_SUPPORT);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        sb.append("*Последние ПГ1:*\n");
        builder = new LogFilter.Builder();
        builder.setCategory(Category.PG1);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        sb.append("*Последние ПГ2:*\n");
        builder = new LogFilter.Builder();
        builder.setCategory(Category.PG2);
        lastRecords = logService.getLastLogs(builder.build(), 1);
        sb.append(getPrettyLog(lastRecords.get(0))).append("\n\n");

        return cleanText(sb.toString());
    }

    /**
     * 30.12.2023 Сессия на любовь, 2 600 ₽
     */
    private static String getPrettyLog(Log log) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDate(log.date())).append(" ");
        sb.append(log.description());
        if (log.price() != 0) {
            sb.append(", ").append(Utils.formatPrice(log.price()));
        }
        return sb.toString();
    }

    public static String getMonthlyReport(@NotNull LogService logService, int year, int month) {
        List<CategorySummary> categorySummary = logService.getCategorySummary(year, month);
        String date = Utils.getMonth(month) + " " + year;
        return cleanText("*Отчёт за " + date + ":*\n\n" +
                getReportByCategories(categorySummary, true));
    }

    public static String getMoneyByCategoryReport(@NotNull LogService logService) {
        List<CategorySummary> categorySummary = logService.getCategorySummary(null, null);
        return cleanText("*Отчёт по категориям:*\n\n" +
                getReportByCategories(categorySummary, true));
    }

    private static String getReportByCategories(List<CategorySummary> categorySummary, boolean addTotalCount) {
        // ToDo: сделать ровненько табличкой
        //Сессии(13): 57 200 ₽
        //Диагностика(1): 0 ₽
        //Всего потрачено: 57 200 ₽
        StringBuilder sb = new StringBuilder();
        int totalPrice = 0;
        categorySummary = categorySummary.stream()
                .sorted(Comparator.comparingInt(CategorySummary::priceSum).reversed())
                .toList();
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

    public static String getMoneyByMonthReport(@NotNull LogService logService, int months, boolean extended) {
        StringBuilder sb = new StringBuilder("*Отчёт за " + declineMonth(months) + ":*\n\n");
        if (extended) {
            List<MonthlyReport> extendedMonthlySummary = logService.getExtendedMonthlySummary(months);
            extendedMonthlySummary
                    .forEach(report -> {
                        sb.append("*").append(getDate(report.getPeriod())).append(":* ")
                                .append(Utils.formatPrice(report.getTotalSpent()))
                                .append("\n\n");
                        sb.append(getReportByCategories(report.getSummaries(), false))
                                .append("\n\n");
                    });
        } else {
            sb.append("```\n");
            List<MonthlyReport> monthlySummary = logService.getShortMonthlySummary(months);
            monthlySummary.forEach(report -> sb.append(String.format("%-14s: %s%n", getDate(report.getPeriod()), Utils.formatPrice(report.getTotalSpent()))));
            sb.append("```");
        }
        return cleanText(sb.toString());
    }

    /**
     * Склоняет слово месяцев в зависимости от кол-ва
     *
     * @param number кол-во месяцев
     * @return {@code number} месяц/а/ев
     */
    public static String declineMonth(int number) {
        // 11-19 - всегда "месяцев"
        if (number >= 11 && number <= 19) {
            return number + " месяцев";
        }

        // Используем остаток от деления на 10 для определения склонения
        int remainder = number % 10;

        return switch (remainder) {
            case 1 -> number + " месяц";
            case 2, 3, 4 -> number + " месяца";
            default -> number + " месяцев";
        };
    }
}
