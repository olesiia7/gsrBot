package bot.gsr.repository;

import bot.gsr.SQLite.LogsFilter;
import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bot.gsr.SQLite.model.LogsTable.C_ID;
import static bot.gsr.SQLite.model.LogsTable.getLogField;

@Repository
public class LogRepositoryImpl implements LogRepository {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String TABLE_NAME = "logs";

    private static final String C_DATE = "date";
    private static final String C_DESCRIPTION = "description";
    private static final String C_PRICE = "price";
    private static final String C_CATEGORY = "category";
    private static final String C_SESSION_TYPE = "session_type";

    private static final String T_CATEGORY = "category_enum";
    private static final String T_SESSION_TYPE = "session_type_enum";

    private static final String ALL_COLUMNS = C_DATE + "," + C_DESCRIPTION + "," + C_PRICE + "," + C_CATEGORY + "," + C_SESSION_TYPE;

    private final DataSource dataSource;

    public LogRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addLog(Log log) {
        String query = "INSERT INTO " + TABLE_NAME + " ("
                + C_DATE + ", "
                + C_DESCRIPTION + ", "
                + C_PRICE + ", "
                + C_CATEGORY + ", "
                + C_SESSION_TYPE
                + ") VALUES (?, ?, ?, ?, ?)";
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query);
            ps.setDate(1, log.date());
            ps.setString(2, log.description());
            ps.setInt(3, log.price());
            ps.setObject(4, log.category().getName(), Types.OTHER);
            if (log.sessionType() == null) {
                ps.setNull(5, Types.OTHER);
            } else {
                ps.setObject(5, log.sessionType().getName(), Types.OTHER);
            }
            ps.execute();
        } catch (SQLException e) {
            logger.error(query + "\n" + log.toString() + "\n" + e.getMessage());
        }
    }

    @Override
    public List<Log> getLogs(LogsFilter filter) {
        String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME +
                buildWhere(filter) +
                "\nORDER BY " + C_DATE + " ASC;";

        Function<ResultSet, List<Log>> resultSetProcessor = resultSet -> {
            List<Log> logs = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    logs.add(getLogFromResultSet(resultSet));
                }
            } catch (SQLException e) {
                logger.error(sql + "\n\t" + e.getMessage());
            }

            return logs;
        };
        return executeQuery(dataSource, sql, resultSetProcessor);
    }

    private String buildWhere(LogsFilter filter) {
        if (filter.isEmpty()) {
            return "";
        }
        StringBuilder where = new StringBuilder();
        boolean needAnd = false;
        where.append(" WHERE ");
        if (filter.getId() != null) {
            where.append(getLogField(C_ID)).append("=").append(filter.getId());
            needAnd = true;
        }
        if (filter.getDate() != null) {
            if (needAnd) {
                where.append(" AND ");
            }
            where.append(C_DATE).append("='").append(filter.getDate()).append("'");
            needAnd = true;
        }
        if (filter.getDescription() != null) {
            if (needAnd) {
                where.append(" AND ");
            }
            where.append(C_DESCRIPTION).append("='").append(filter.getDescription()).append("'");
            needAnd = true;
        }
        if (filter.getCategory() != null) {
            if (needAnd) {
                where.append(" AND ");
            }
            where.append(C_CATEGORY).append("='").append(filter.getCategory().getName()).append("'");
            needAnd = true;
        }
        if (filter.getSessionType() != null) {
            if (needAnd) {
                where.append(" AND ");
            }
            where.append(C_SESSION_TYPE).append("='").append(filter.getSessionType().getName()).append("'");
        }
        return where.toString();
    }

    private Log getLogFromResultSet(ResultSet rs) throws SQLException {
        Date date = rs.getDate(C_DATE);
        String description = rs.getString(C_DESCRIPTION);
        int price = rs.getInt(C_PRICE);
        String categoryName = rs.getString(C_CATEGORY);
        Category category = Category.getCategory(categoryName);
        String sessionTypeName = rs.getString(C_SESSION_TYPE);
        SessionType sessionType = sessionTypeName == null ? null : SessionType.getSessionType(sessionTypeName);
        return new Log(date, description, price, category, sessionType);
    }

    @Override
    public void createTableIfNotExists() {
        if (!isTableExists()) {
            logger.warn("База данных не существует. Создаю с 0");

            if (!isCategoryExists()) {
                logger.warn("Типа " + T_CATEGORY + " не существует. Создаю с 0");
                createCategoryEnum();
            }

            if (!isSessionTypeExists()) {
                logger.warn("Типа " + T_SESSION_TYPE + " не существует. Создаю с 0");
                createSessionTypeEnum();
            }
            createTable();
        }
    }

    public boolean isTableExists() {
        String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables\n" +
                "WHERE table_schema = 'public'\n" +
                "AND table_name = '" + TABLE_NAME + "'\n" +
                ") AS table_exists;";
        return getBooleanResult(sql);
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "id SERIAL PRIMARY KEY, "
                + C_DATE + " DATE NOT NULL, "
                + C_DESCRIPTION + " TEXT NOT NULL, "
                + C_PRICE + " INTEGER NOT NULL, "
                + C_CATEGORY + " " + T_CATEGORY + " NOT NULL, "
                + C_SESSION_TYPE + " " + T_SESSION_TYPE
                + ")";
        execute(dataSource, sql);
    }

    private boolean isCategoryExists() {
        String sql = "SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = '" + T_CATEGORY + "');";
        return getBooleanResult(sql);
    }

    private void createCategoryEnum() {
        String sql = "CREATE TYPE " + T_CATEGORY + " AS ENUM (" +
                Arrays.stream(Category.values())
                        .map(Category::getName)
                        .map(name -> "'" + name + "'")
                        .collect(Collectors.joining(",")) +
                ");";
        execute(dataSource, sql);
    }

    private boolean isSessionTypeExists() {
        String sql = "SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = '" + T_SESSION_TYPE + "');";
        return getBooleanResult(sql);
    }

    private void createSessionTypeEnum() {
        String sql = "CREATE TYPE " + T_SESSION_TYPE + " AS ENUM (" +
                Arrays.stream(SessionType.values())
                        .map(SessionType::getName)
                        .map(name -> "'" + name + "'")
                        .collect(Collectors.joining(",")) +
                ");";
        execute(dataSource, sql);
    }

    private boolean getBooleanResult(String sql) {
        Function<ResultSet, Boolean> resultSetProcessor = resultSet -> {
            try {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            } catch (SQLException e) {
                logger.error(sql + "\n\t" + e.getMessage());
            }
            return false;
        };
        return executeQuery(dataSource, sql, resultSetProcessor);
    }

    @Override
    public void dropTableIfExists() {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
        execute(dataSource, sql);

        sql = "DROP TYPE IF EXISTS " + T_CATEGORY + ";";
        execute(dataSource, sql);

        sql = "DROP TYPE IF EXISTS " + T_SESSION_TYPE + ";";
        execute(dataSource, sql);
    }

    @Override
    public void clearAllData() {
        String sql = "DELETE FROM " + TABLE_NAME + ";";
        execute(dataSource, sql);
    }
}
