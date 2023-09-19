package events;

/**
 * Получает отчет за указанное кол-во месяцев
 * в краткой (только общая сумма) или полной (траты по категориям) форме
 */
public record GetMonthlyReportEvent() implements Event {
}
