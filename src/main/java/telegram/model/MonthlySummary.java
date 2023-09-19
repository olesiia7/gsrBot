package telegram.model;

/**
 * @param period     период в формате YYYY-dd (2023-09)
 * @param totalPrice сумма потраченных денег в этот период
 */
public record MonthlySummary(String period, int totalPrice) {
}
