package bot.gsr.service;

import bot.gsr.model.CategorySummary;
import bot.gsr.model.Log;
import bot.gsr.model.LogFilter;
import bot.gsr.model.MonthlyReport;
import bot.gsr.repository.LogRepository;
import bot.gsr.telegram.model.YearMonth;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

@Service
public class LogService {
    private final LogRepository repository;

    public LogService(LogRepository repository) {
        this.repository = repository;
    }

    public List<Log> getLogs(LogFilter filter) {
        return repository.getLogs(filter);
    }

    public List<Log> getLastLogs(LogFilter filter, int amount) {
        return repository.getLastLogs(filter, amount);
    }

    public void addLog(Log log) {
        repository.addLog(log);
    }

    /**
     * Получаем названия последних (по дате) публикаций (их может быть несколько в одну дату)
     */
    public List<String> getLastSessionOrDiagnostic() {
        return repository.getLastSessionOrDiagnostic();
    }

    /**
     * Получаем все месяцы, которые есть в отчете (2023, 8; 2023, 7; ...)
     */
    public List<YearMonth> getAllPeriods() {
        return repository.getAllPeriods();
    }

    /**
     * Получаем отчет (сумма потраченного) за {@code months}
     *
     * @param months кол-во месяцев, начиная с 0 (если 0 – то результаты будут за текущий месяц)
     */
    public List<MonthlyReport> getShortMonthlySummary(int months) {
        return repository.getShortMonthlySummary(months);
    }

    /**
     * Получаем расширенный отчет (с тратами по категориям) за {@code months}
     *
     * @param months кол-во месяцев, начиная с 0 (если 0 – то результаты будут за текущий месяц)
     */
    public List<MonthlyReport> getExtendedMonthlySummary(int months) {
        return repository.getExtendedMonthlySummary(months);
    }


    /**
     * @param year  год. Если {@code null}, то за любой год
     * @param month месяц. Если {@code null}, то за любой месяц
     * @return Получаем категорию + кол-во в ней + сумма расходов
     */
    public List<CategorySummary> getCategorySummary(@Nullable String year, @Nullable String month) {
        return repository.getCategorySummary(year, month);
    }

    public void createTableIfNotExists() {
        repository.createTableIfNotExists();
    }

    public void createDump(@NotNull String pathToDump) {
        repository.createDump(pathToDump);
    }

    public InputStream getDump() {
        return repository.getDump();
    }

    public void applyDump(@NotNull String pathToDump) {
        repository.applyDump(pathToDump);
    }
}
