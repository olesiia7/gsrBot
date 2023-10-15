package bot.gsr.service;

import bot.gsr.SQLite.LogsFilter;
import bot.gsr.model.Log;
import bot.gsr.repository.LogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    private final LogRepository repository;

    public LogService(LogRepository repository) {
        this.repository = repository;
    }

    public List<Log> getLogs(LogsFilter filter) {
        return repository.getLogs(filter);
    }

    List<Log> getLastLogs(LogsFilter filter, int amount) {
        return repository.getLastLogs(filter, amount);
    }

    public void addLog(Log log) {
        repository.addLog(log);
    }

    //ToDo GSRBOT-7 миграция
//    /**
//     * Получаем названия последних (по дате) публикаций (их может быть несколько в одну дату)
//     */
//    public List<String> getLastSessionOrDiagnostic() {
//        return repository.getLastSessionOrDiagnostic();
//    }
//
//    /**
//     * Получаем {@code amount} последних записей
//     */
//    public List<Log> getLastRecords(int amount) {
//        return repository.getLastRecords(amount);
//    }
//
//    /**
//     * Получаем все месяцы, которые есть в отчете (2023, 8; 2023, 7; ...)
//     */
//    public List<YearMonth> getAllPeriods() {
//        return repository.getAllPeriods();
//    }
//
//    /**
//     * Получаем расширенный отчет (с тратами по категориям) за {@code months}
//     *
//     * @param months кол-во месяцев, начиная с 0
//     */
//    public List<MonthlyCategorySummary> getExtendedMonthlySummary(int months) {
//        return repository.getExtendedMonthlySummary(months);
//    }
//
//    /**
//     * Получаем отчет (сумма потраченного) за {@code months}
//     *
//     * @param months кол-во месяцев, начиная с 0
//     */
//    public List<MonthlySummary> getMonthlySummary(int months) {
//        return repository.getMonthlySummary(months);
//    }
//
//    /**
//     * @param period период в виде yyyy-mm (если null, то за всё время)
//     * @return Получаем категорию + кол-во в ней + сумма расходов
//     */
//    public List<CategorySummary> getCategorySummary(@Nullable String period) {
//        return repository.getCategorySummary(period);
//    }

    public void createTableIfNotExists() {
        repository.createTableIfNotExists();
    }
}
