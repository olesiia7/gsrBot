package bot.gsr.repository.impl;

import bot.gsr.model.*;
import bot.gsr.repository.LogRepository;
import bot.gsr.telegram.model.YearMonth;
import bot.gsr.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public List<Log> getLogs(LogFilter filter) {
        String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME +
                buildWhere(filter) +
                "\nORDER BY " + C_DATE + " ASC;";

        Function<ResultSet, List<Log>> resultSetProcessor = getResultSetProcessor(sql);
        return executeQuery(dataSource, sql, resultSetProcessor);
    }

    private Function<ResultSet, List<Log>> getResultSetProcessor(String sql) {
        return resultSet -> {
            List<Log> logs = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    logs.add(getLogFromResultSet(resultSet));
                }
            } catch (SQLException e) {
                logError(sql, e);
            }

            return logs;
        };
    }

    private String buildWhere(LogFilter filter) {
        if (filter.isEmpty()) {
            return "";
        }
        StringBuilder where = new StringBuilder();
        boolean needAnd = false;
        where.append("\nWHERE ");
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
    public List<Log> getLastLogs(LogFilter filter, int amount) {
        String sql = String.format("""
                        SELECT %s FROM %s %s
                        ORDER BY %s DESC
                        LIMIT %d;""",
                ALL_COLUMNS, TABLE_NAME, buildWhere(filter), C_DATE, amount);
        return executeQuery(dataSource, sql, getResultSetProcessor(sql));
    }

    @Override
    public List<String> getLastSessionOrDiagnostic() {
        String sql = "SELECT " + C_DESCRIPTION + "\n" +
                "FROM " + TABLE_NAME + "\n" +
                "WHERE " + C_DATE + " = (SELECT MAX(" + C_DATE + ") FROM " + TABLE_NAME + "\n" +
                "WHERE " + C_CATEGORY + " IN('" + Category.DIAGNOSTIC.getName() + "','" + Category.SESSION.getName() + "'))\n" +
                "AND " + C_CATEGORY + " IN('" + Category.DIAGNOSTIC.getName() + "','" + Category.SESSION.getName() + "');";
        Function<ResultSet, List<String>> rsProcessor = resultSet -> {
            List<String> lastDescriptions = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    lastDescriptions.add(resultSet.getString(1));
                }
            } catch (SQLException e) {
                logError(sql, e);
            }
            return lastDescriptions;
        };
        return executeQuery(dataSource, sql, rsProcessor);
    }

    private void logError(String sql, SQLException e) {
        logger.error(sql + "\n\t" + e.getMessage());
    }

    @Override
    public List<YearMonth> getAllPeriods() {
        String sql = "SELECT DISTINCT EXTRACT(YEAR FROM " + C_DATE + ") AS year,\n" +
                "EXTRACT(MONTH FROM " + C_DATE + ") AS month\n" +
                "FROM " + TABLE_NAME + "\n" +
                "ORDER BY year DESC, month DESC;";
        Function<ResultSet, List<YearMonth>> rsProcessor = resultSet -> {
            List<YearMonth> result = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    int year = resultSet.getInt(1);
                    int month = resultSet.getInt(2);
                    result.add(new YearMonth(year, month));
                }
            } catch (SQLException e) {
                logger.error(sql, e);
            }
            return result;
        };
        return executeQuery(dataSource, sql, rsProcessor);
    }

    @Override
    public List<CategorySummary> getCategorySummary(@Nullable String year, @Nullable String month) {
        String sql = "SELECT " + C_CATEGORY + ", COUNT(*) AS count, SUM(" + C_PRICE + ") AS total_price " +
                "FROM logs\n" +
                "WHERE (" + year + " IS NULL OR EXTRACT(YEAR FROM " + C_DATE + ") = " + year + ")\n" +
                "AND (" + month + " IS NULL OR EXTRACT(MONTH FROM " + C_DATE + ") = " + month + ")\n" +
                "GROUP BY " + C_CATEGORY;
        Function<ResultSet, List<CategorySummary>> rsProcessor = resultSet -> {
            List<CategorySummary> result = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    Category category = Category.getCategory(resultSet.getString(C_CATEGORY));
                    int count = resultSet.getInt(2);
                    int priceSum = resultSet.getInt(3);
                    result.add(new CategorySummary(category, count, priceSum));
                }
            } catch (SQLException e) {
                logger.error(sql, e);
            }
            return result;
        };
        return executeQuery(dataSource, sql, rsProcessor);
    }

    @Override
    public List<MonthlyReport> getShortMonthlySummary(int months) {
        String monthColumn = "month";
        String totalPriceColumn = "total_price";
        String sql = "SELECT TO_CHAR(" + C_DATE + ", 'YYYY-MM') AS " + monthColumn + ",\n" +
                "SUM(" + C_PRICE + ") AS " + totalPriceColumn + "\n" +
                "FROM " + TABLE_NAME + "\n" +
                "WHERE date >= DATE_TRUNC('MONTH', CURRENT_DATE - INTERVAL '" + months + " MONTHS')::date\n" +
                "AND " + C_DATE + " <= CURRENT_DATE::date\n" +
                "GROUP BY " + monthColumn + "\n" +
                "ORDER BY " + monthColumn + " DESC";

        Function<ResultSet, List<MonthlyReport>> rsProcessor = resultSet -> {
            List<MonthlyReport> result = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    String period = resultSet.getString(monthColumn);
                    int totalPrice = resultSet.getInt(totalPriceColumn);
                    result.add(MonthlyReport.shortMonthlyReport(period, totalPrice));
                }

            } catch (SQLException e) {
                logger.error(sql, e);
            }
            return result;
        };
        return executeQuery(dataSource, sql, rsProcessor);
    }

    @Override
    public List<MonthlyReport> getExtendedMonthlySummary(int months) {
        String monthColumn = "month";
        String countColumn = "count";
        String totalPriceColumn = "total_price";
        String sql = "SELECT TO_CHAR(" + C_DATE + ", 'YYYY-MM') AS " + monthColumn + ",\n" +
                C_CATEGORY + ",\n" +
                "COUNT(*) AS " + countColumn + ",\n" +
                "SUM(" + C_PRICE + ") AS " + totalPriceColumn + "\n" +
                "FROM " + TABLE_NAME + "\n" +
                "WHERE date >= DATE_TRUNC('MONTH', CURRENT_DATE - INTERVAL '" + months + " MONTHS')::date\n" +
                "AND " + C_DATE + " <= CURRENT_DATE::date\n" +
                "GROUP BY " + monthColumn + ", " + C_CATEGORY + "\n" +
                "ORDER BY " + monthColumn + " DESC, " + C_CATEGORY;

        Function<ResultSet, List<MonthlyReport>> rsProcessor = resultSet -> {
            List<MonthlyReport> result = new ArrayList<>();
            List<CategorySummary> categorySummaries = new ArrayList<>();
            String prevPeriod = null;
            try {
                while (resultSet.next()) {
                    // month, category, count, total_price
                    String currentPeriod = resultSet.getString(monthColumn);
                    Category category = Category.getCategory(resultSet.getString(C_CATEGORY));
                    int count = resultSet.getInt(countColumn);
                    int priceSum = resultSet.getInt(totalPriceColumn);
                    if (!currentPeriod.equals(prevPeriod)) {
                        if (prevPeriod != null) {
                            result.add(MonthlyReport.extendedMonthlyReport(prevPeriod, categorySummaries));
                            categorySummaries = new ArrayList<>();
                        }
                        categorySummaries.add(new CategorySummary(category, count, priceSum));
                        prevPeriod = currentPeriod;
                    } else {
                        categorySummaries.add(new CategorySummary(category, count, priceSum));
                    }
                }
                if (prevPeriod != null) {
                    result.add(MonthlyReport.extendedMonthlyReport(prevPeriod, categorySummaries));
                }
                return result;

            } catch (SQLException e) {
                logger.error(sql, e);
            }
            return result;
        };
        return executeQuery(dataSource, sql, rsProcessor);
    }

    @Override
    public void makeDump(String backupFilePath) {
        List<Log> logs = getLogs(LogFilter.EMPTY);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFilePath))) {
            writer.write(ALL_COLUMNS);
            writer.newLine();

            for (Log log : logs) {
                writer.write(Utils.getCSV(log));
                writer.newLine();
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void applyDump(String backupFilePath) {
        String sql = String.format("COPY %s(%s) FROM '%s' WITH CSV HEADER;", TABLE_NAME, ALL_COLUMNS, backupFilePath);
        execute(dataSource, sql);
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
                logError(sql, e);
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
