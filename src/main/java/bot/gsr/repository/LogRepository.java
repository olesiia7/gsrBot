package bot.gsr.repository;

import bot.gsr.SQLite.LogsFilter;
import bot.gsr.model.Log;
import bot.gsr.repository.impl.LogRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface LogRepository {
    Logger logger = LoggerFactory.getLogger(LogRepository.class);

    void createTableIfNotExists();

    boolean isTableExists();

    void clearAllData();

    void dropTableIfExists();

    void addLog(@NotNull Log log);

    List<Log> getLogs(@NotNull LogsFilter filter);

    List<Log> getLastLogs(@NotNull LogsFilter filter, int amount);

    List<String> getLastSessionOrDiagnostic();

    //ToDo GSRBOT-7 миграция
//    List<Log> getLastRecords(int amount);
//
//    List<YearMonth> getAllPeriods();
//
//    List<CategorySummary> getCategorySummary(@Nullable String period);
//
//    List<MonthlyCategorySummary> getExtendedMonthlySummary(int months);
//
//    List<MonthlySummary> getMonthlySummary(int months);

    void makeDump(@NotNull String backupFilePath);

    void applyDump(@NotNull String backupFilePath);

    default <T> T executeQuery(@NotNull DataSource dataSource,
                               @NotNull String SQL,
                               @NotNull Function<ResultSet, T> resultSetProcessor) {
        try (Connection con = dataSource.getConnection()) {
            Statement statement = con.createStatement();
            ResultSet resultSet = statement.executeQuery(SQL);
            return resultSetProcessor.apply(resultSet);
        } catch (SQLException e) {
            logError(SQL, e);
        }
        return null;
    }

    default void execute(@NotNull DataSource dataSource, @NotNull String SQL) {
        try (Connection con = dataSource.getConnection()) {
            Statement statement = con.createStatement();
            statement.execute(SQL);
        } catch (SQLException e) {
            logError(SQL, e);
        }
    }

    private static void logError(@NotNull String SQL, @NotNull SQLException e) {
        Optional<String> calledMethod = Arrays.stream(e.getStackTrace())
                .filter(stack -> stack.getClassName().equals(LogRepositoryImpl.class.getName()))
                .map(stack -> {
                    String[] className = stack.getClassName().split("\\.");
                    String simpleName = className[className.length - 1];
                    return "метод вызван из .(" + simpleName + ".java:" + stack.getLineNumber() + ")";
                })
                .findFirst();
        logger.error(SQL + "\n\t" + e.getMessage() + (calledMethod.map(s -> "\n" + s).orElse("")));
    }

}
