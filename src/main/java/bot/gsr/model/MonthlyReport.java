package bot.gsr.model;

import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;


public class MonthlyReport {
    private final String period;
    private final List<CategorySummary> summaries;
    private final int totalSpent;

    /**
     * @param period     период в формате YYYY-dd (2023-09)
     * @param summaries  траты по категориям (для {@link MonthlyReportType#EXTENDED})
     * @param totalSpent сумма трат за весь период
     */
    private MonthlyReport(@NotNull String period,
                          @Nullable List<CategorySummary> summaries,
                          int totalSpent) {

        this.period = period;
        this.summaries = summaries == null ? Collections.emptyList() : Collections.unmodifiableList(summaries);
        this.totalSpent = totalSpent;
    }

    /**
     * @param period     период в формате YYYY-dd (2023-09)
     * @param totalSpent сумма трат за весь период
     * @return {@link MonthlyReport}
     */
    public static MonthlyReport shortMonthlyReport(String period, int totalSpent) {
        return new MonthlyReport(period, Collections.emptyList(), totalSpent);
    }

    /**
     * @param period    период в формате YYYY-dd (2023-09)
     * @param summaries траты по категориям
     * @return {@link MonthlyReport}
     */
    public static MonthlyReport extendedMonthlyReport(String period, @NotNull List<CategorySummary> summaries) {
        int totalSpent = summaries.stream()
                .mapToInt(CategorySummary::priceSum)
                .sum();
        return new MonthlyReport(period, summaries, totalSpent);
    }

    /**
     * @return период в формате YYYY-dd (2023-09)
     */
    @NotNull
    public String getPeriod() {
        return period;
    }

    @NotNull
    public List<CategorySummary> getSummaries() {
        return summaries;
    }

    public int getTotalSpent() {
        return totalSpent;
    }
}
