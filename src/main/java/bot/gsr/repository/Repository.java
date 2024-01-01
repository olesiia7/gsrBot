package bot.gsr.repository;

import bot.gsr.repository.impl.LogRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public interface Repository {
    Logger logger = LoggerFactory.getLogger(Repository.class);

    String tableName();

    DataSource dataSource();

    default boolean isTableExists() {
        String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables\n" +
                "WHERE table_schema = 'public'\n" +
                "AND table_name = '" + tableName() + "'\n" +
                ") AS table_exists;";
        return getBooleanResult(sql);
    }

    void createTableIfNotExists();

    default void clearAllData() {
        String sql = "DELETE FROM " + tableName() + ";";
        execute(sql);
    }

    default void dropTableIfExists() {
        String sql = "DROP TABLE IF EXISTS " + tableName() + ";";
        execute(sql);
    }

    InputStream getDump();

    default <T> T executeQuery(@NotNull String sql,
                               @NotNull Function<ResultSet, T> resultSetProcessor) {
        try (Connection con = dataSource().getConnection()) {
            Statement statement = con.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            return resultSetProcessor.apply(resultSet);
        } catch (SQLException e) {
            logError(sql, e);
        }
        return null;
    }

    default void execute(@NotNull String sql) {
        try (Connection con = dataSource().getConnection()) {
            Statement statement = con.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            logError(sql, e);
        }
    }

    default boolean getBooleanResult(String sql) {
        Function<ResultSet, Boolean> resultSetProcessor = resultSet -> {
            try {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            } catch (SQLException e) {
                logError(sql, e);
            }
            return false;
        };
        return executeQuery(sql, resultSetProcessor);
    }

    //ToDo: убрать костыль: сейчас парсится строчка лога только LogRepositoryImpl. Мб через лог вычислять?
    private static void logError(@NotNull String sql, @NotNull SQLException e) {
        Optional<String> calledMethod = Arrays.stream(e.getStackTrace())
                .filter(stack -> stack.getClassName().equals(LogRepositoryImpl.class.getName()))
                .map(stack -> {
                    String[] className = stack.getClassName().split("\\.");
                    String simpleName = className[className.length - 1];
                    return "метод вызван из .(" + simpleName + ".java:" + stack.getLineNumber() + ")";
                })
                .findFirst();
        logger.error("{}\n\t{}{}", sql, e.getMessage(), (calledMethod.map(s -> "\n" + s).orElse("")));
    }
}
