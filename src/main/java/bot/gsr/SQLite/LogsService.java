package bot.gsr.SQLite;

import bot.gsr.SQLite.model.Log;
import bot.gsr.telegram.model.CategorySummary;
import bot.gsr.telegram.model.MonthlyCategorySummary;
import bot.gsr.telegram.model.MonthlySummary;
import bot.gsr.telegram.model.YearMonth;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
public class LogsService extends Service<LogsDAO> {

    public LogsService(LogsDAO dao) {
        super(dao);
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        return dao.getLogs(filter);
    }

    public List<Log> getLastLogs(LogsFilter filter, int amount) throws SQLException {
        return dao.getLastLogs(filter, amount);
    }

    public int addLog(Log log) throws SQLException {
        return dao.addLog(log);
    }

    /**
     * Получаем названия последних (по дате) публикаций (их может быть несколько в одну дату)
     */
    public List<String> getLastSessionOrDiagnostic() throws SQLException {
        return dao.getLastSessionOrDiagnostic();
    }

    /**
     * Получаем {@code amount} последних записей
     */
    public List<Log> getLastRecords(int amount) throws SQLException {
        return dao.getLastRecords(amount);
    }

    /**
     * Получаем все месяцы, которые есть в отчете (2023, 8; 2023, 7; ...)
     */
    public List<YearMonth> getAllPeriods() throws SQLException {
        return dao.getAllPeriods();
    }

    /**
     * Получаем расширенный отчет (с тратами по категориям) за {@code months}
     *
     * @param months кол-во месяцев, начиная с 0
     */
    public List<MonthlyCategorySummary> getExtendedMonthlySummary(int months) throws SQLException {
        return dao.getExtendedMonthlySummary(months);
    }

    /**
     * Получаем отчет (сумма потраченного) за {@code months}
     *
     * @param months кол-во месяцев, начиная с 0
     */
    public List<MonthlySummary> getMonthlySummary(int months) throws SQLException {
        return dao.getMonthlySummary(months);
    }

    /**
     * @param period период в виде yyyy-mm (если null, то за всё время)
     * @return Получаем категорию + кол-во в ней + сумма расходов
     */
    public List<CategorySummary> getCategorySummary(@Nullable String period) throws SQLException {
        return dao.getCategorySummary(period);
    }
}
