package bot.gsr.handlers;

import bot.gsr.events.GetMonthlyReportEvent;
import bot.gsr.events.SendMeTelegramMessageEvent;
import bot.gsr.model.MonthlyReport;
import bot.gsr.model.MonthlyReportType;
import bot.gsr.service.LogService;
import bot.gsr.telegram.AnswerListener;
import bot.gsr.telegram.MarkupFactory;
import bot.gsr.telegram.commands.QueryCommand;
import bot.gsr.utils.Utils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static bot.gsr.telegram.MarkupFactory.REMOVE_MARKUP;
import static bot.gsr.utils.Utils.getMonth;

@Component
public class GetMonthlyReportHandler {
    private final LogService logService;
    private final EventManager manager;

    private final String CHOSE_PERIOD_TEXT = "Выберите период или введите нужное кол-во месяцев, \nгде 0 - текущий месяц, 1 – текущий + предыдущий и т.д.";

    private final String CURRENT_MONTH = "текущий месяц";
    private final String THREE_MONTHS = "3 месяца";
    private final String SIX_MONTHS = "6 месяцев";
    private final String YEAR = "год";

    private final String SHORT_FORM = "короткая";
    private final String EXTENDED_FORM = "подробная";

    private final ReplyKeyboardMarkup PERIOD_MARKUP = getPeriodMarkup();
    private final ReplyKeyboardMarkup FORM_MARKUP = getFormMarkup();

    public GetMonthlyReportHandler(LogService logService, EventManager manager) {
        this.logService = logService;
        this.manager = manager;
    }

    @EventListener
    public void handleEvent(GetMonthlyReportEvent event) {
        confirmPeriod();
    }

    /**
     * Уточняет период (за сколько месяцев нужен отчет)
     */
    private void confirmPeriod() {
        manager.handleEvent(new SendMeTelegramMessageEvent(CHOSE_PERIOD_TEXT, PERIOD_MARKUP, checkPeriodAndConfirmFormReport(), false));
    }

    private AnswerListener checkPeriodAndConfirmFormReport() {
        return answer -> {
            String answerPeriod = (String) answer;
            Integer months = switch (answerPeriod) {
                case CURRENT_MONTH -> 0;
                case THREE_MONTHS -> 3;
                case SIX_MONTHS -> 6;
                case YEAR -> 12;
                default -> {
                    try {
                        yield Integer.parseInt(answerPeriod);
                    } catch (NumberFormatException ex) {
                        yield null;
                    }
                }
            };
            if (months == null || months < 0) {
                invalidPeriod();
                return;
            }
            confirmFormReport(months);
        };
    }

    private void invalidPeriod() {
        String text = "Период, который вы выбрали, не валидный." +
                "\nПожалуйста, выберите один из вариантов или введите положительное целое число.\n\n"
                + CHOSE_PERIOD_TEXT;
        manager.handleEvent(new SendMeTelegramMessageEvent(text, PERIOD_MARKUP, checkPeriodAndConfirmFormReport(), false));
    }

    /**
     * Уточняет вид отчета: краткий (только сумма за месяц) или полный (траты по категориям)
     */
    private void confirmFormReport(int months) {
        String text = """
                Выберите форму отчёта:
                Краткая – только общая сумма за месяц
                Подробная – общая сумма за месяц + траты по категориям""";
        AnswerListener listener = answer -> {
            String answerForm = (String) answer;
            MonthlyReportType reportForm = switch (answerForm) {
                case SHORT_FORM -> MonthlyReportType.SHORT;
                case EXTENDED_FORM -> MonthlyReportType.EXTENDED;
                default -> null;
            };
            if (reportForm == null) {
                confirmFormReport(months);
                return;
            }
            provideFormReport(months, reportForm);
        };
        manager.handleEvent(new SendMeTelegramMessageEvent(text, FORM_MARKUP, listener, false));
    }


    private void provideFormReport(int months, MonthlyReportType reportForm) {
        StringBuilder sb = new StringBuilder("*Отчёт за " + declineMonth(months) + ":*\n\n");
        String text = switch (reportForm) {
            case SHORT -> {
                sb.append("```\n");
                List<MonthlyReport> monthlySummary = logService.getShortMonthlySummary(months);
                monthlySummary.forEach(report -> sb.append(String.format("%-14s: %s\n", getDate(report.getPeriod()), Utils.formatPrice(report.getTotalSpent()))));
                sb.append("```");
                yield sb.toString();
            }
            case EXTENDED -> {
                List<MonthlyReport> extendedMonthlySummary = logService.getExtendedMonthlySummary(months);
                extendedMonthlySummary
                        .forEach(report -> {
                            sb.append("*").append(getDate(report.getPeriod())).append(":* ")
                                    .append(Utils.formatPrice(report.getTotalSpent()))
                                    .append("\n\n");
                            sb.append(QueryCommand.getReportByCategories(report.getSummaries(), false))
                                    .append("\n\n");
                        });
                yield sb.toString();
            }
        };

        manager.handleEvent(new SendMeTelegramMessageEvent(text, REMOVE_MARKUP, null, true));
    }

    /**
     * Склоняет слово месяцев в зависимости от кол-ва
     *
     * @param number кол-во месяцев
     * @return {@code number} месяц/а/ев
     */
    private static String declineMonth(int number) {
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


    /**
     * @param date YYYY-dd
     * @return дата вида Сентябрь 2023
     */
    private String getDate(String date) {
        String[] split = date.split("-");
        String year = split[0];
        int monthId = Integer.parseInt(split[1]);
        String month = getMonth(monthId - 1);
        return month + " " + year;
    }

    private ReplyKeyboardMarkup getPeriodMarkup() {
        List<String> periods = new ArrayList<>();
        periods.add(CURRENT_MONTH);
        periods.add(THREE_MONTHS);
        periods.add(SIX_MONTHS);
        periods.add(YEAR);
        return MarkupFactory.getReplyMarkup(periods.toArray(String[]::new));
    }

    private ReplyKeyboardMarkup getFormMarkup() {
        List<String> forms = new ArrayList<>();
        forms.add(SHORT_FORM);
        forms.add(EXTENDED_FORM);
        return MarkupFactory.getReplyMarkup(forms.toArray(String[]::new));
    }
}
