package bot.gsr.repository;

import bot.gsr.model.CategorySummary;
import bot.gsr.model.Log;
import bot.gsr.model.LogFilter;
import bot.gsr.model.MonthlyReport;
import bot.gsr.telegram.model.YearMonth;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface LogRepository extends Repository {

    void addLog(@NotNull Log log);

    List<Log> getLogs(@NotNull LogFilter filter);

    List<Log> getLastLogs(@NotNull LogFilter filter, int amount);

    List<String> getLastSessionOrDiagnostic();

    List<YearMonth> getAllPeriods();

    List<CategorySummary> getCategorySummary(@Nullable String year, @Nullable String month);

    List<MonthlyReport> getShortMonthlySummary(int months);

    List<MonthlyReport> getExtendedMonthlySummary(int months);

    void createDump(@NotNull String backupFilePath);

    void applyDump(@NotNull String backupFilePath);

}
