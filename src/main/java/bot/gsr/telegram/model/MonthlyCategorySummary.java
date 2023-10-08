package bot.gsr.telegram.model;

import java.util.List;

/**
 * @param summaries информация по категориям
 * @param period    период в формате YYYY-dd (2023-09)
 */
public record MonthlyCategorySummary(List<CategorySummary> summaries, String period) {
}
