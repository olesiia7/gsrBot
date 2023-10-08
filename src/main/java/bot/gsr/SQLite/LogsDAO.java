package bot.gsr.SQLite;

import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.Log;
import bot.gsr.SQLite.model.SessionType;
import bot.gsr.telegram.model.CategorySummary;
import bot.gsr.telegram.model.MonthlyCategorySummary;
import bot.gsr.telegram.model.MonthlySummary;
import bot.gsr.telegram.model.YearMonth;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static bot.gsr.SQLite.model.LogsTable.*;

@Component
public class LogsDAO extends DAO {
    private static final String COLUMNS_WITHOUT_ID = C_DATE + "," + C_DESCRIPTION + "," + C_PRICE + "," + C_CATEGORY_ID + "," + C_SESSION_TYPE_ID;
    private static final String GET_LAST_ID_SQL = "SELECT last_insert_rowid();";
    private final String SELECT_ALL = "SELECT * FROM " + getTableName();
    private final String CONVERT_TIME_SQL = "datetime(" + C_DATE + " / 1000, 'unixepoch', 'localtime')";

    public LogsDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        Statement stmt = connection.createStatement();
        final String sql = SELECT_ALL +
                buildWhere(filter) +
                "\nORDER BY " + C_DATE + " ASC;";

        ResultSet rs = stmt.executeQuery(sql);
        List<Log> logs = new ArrayList<>();
        while (rs.next()) {
            logs.add(getLogFromResultSet(rs));
        }
        return logs;
    }

    public List<Log> getLastLogs(LogsFilter filter, int amount) throws SQLException {
        Statement stmt = connection.createStatement();
        final String sql = SELECT_ALL +
                buildWhere(filter) +
                "\nORDER BY " + C_DATE + " DESC\n" +
                "LIMIT " + amount + ";";

        ResultSet rs = stmt.executeQuery(sql);
        List<Log> logs = new ArrayList<>();
        while (rs.next()) {
            logs.add(getLogFromResultSet(rs));
        }
        return logs;
    }

    private Log getLogFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt(C_ID);
        Date date = rs.getDate(C_DATE);
        String description = rs.getString(C_DESCRIPTION);
        int price = rs.getInt(C_PRICE);
        int categoryId = rs.getInt(C_CATEGORY_ID);
        int sessionTypeId = rs.getInt(C_SESSION_TYPE_ID);
        SessionType sessionType = sessionTypeId == 0 ? null : SessionType.findById(sessionTypeId);
        return new Log(id, date, description, price, Category.findById(categoryId), sessionType);
    }


    public List<String> getLastSessionOrDiagnostic() throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "SELECT " + C_DATE + " FROM " + getTableName() + "\n" +
                "WHERE " + C_CATEGORY_ID + " IN(" + Category.SESSION.getId() + "," + Category.DIAGNOSTIC.getId() + ")\n" +
                "ORDER BY " + C_DATE + " DESC\n" +
                "LIMIT 1;";
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            Date lastDate = rs.getDate(C_DATE);
            sql = "SELECT " + C_DESCRIPTION + " FROM " + getTableName() + "\n" +
                    "WHERE " + C_CATEGORY_ID + " IN(" + Category.SESSION.getId() + "," + Category.DIAGNOSTIC.getId() + ")\n" +
                    "AND " + C_DATE + "=" + lastDate.getTime() +
                    ";";
            rs = stmt.executeQuery(sql);
            List<String> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getString(C_DESCRIPTION));
            }
            return result;
        }
        throw new SQLException("Error while getting last session/diagnostic date");
    }

    public List<Log> getLastRecords(int amount) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = SELECT_ALL +
                "\nGROUP BY " + getLogField(C_ID) +
                "\nORDER BY " + C_DATE + " DESC\n" +
                "LIMIT " + amount + ";";
        ResultSet rs = stmt.executeQuery(sql);
        List<Log> result = new ArrayList<>();
        while (rs.next()) {
            result.add(getLogFromResultSet(rs));
        }
        return result;
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
            where.append(C_DATE).append("=").append(filter.getDate().getTime());
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
            where.append(C_CATEGORY_ID).append("=").append(filter.getCategory().getId());
            needAnd = true;
        }
        if (filter.getSessionType() != null) {
            if (needAnd) {
                where.append(" AND ");
            }
            where.append(C_SESSION_TYPE_ID).append("=").append(filter.getSessionType().getId());
        }
        return where.toString();
    }

    public int addLog(Log log) throws SQLException {
        String sql = "INSERT INTO " + getTableName() + " (" + COLUMNS_WITHOUT_ID + ") VALUES (?,?,?,?,?);";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setDate(1, log.date());
        stmt.setString(2, log.description());
        stmt.setInt(3, log.price());
        stmt.setInt(4, log.category().getId());
        if (log.sessionType() == null) {
            stmt.setNull(5, Types.INTEGER);
        } else {
            stmt.setInt(5, log.sessionType().getId());
        }
        stmt.execute();

        return getLastLogId();
    }

    private int getLastLogId() throws SQLException {
        ResultSet resultSet = connection.prepareStatement(GET_LAST_ID_SQL).executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        throw new SQLException("Error while getting last log id");
    }

    public List<YearMonth> getAllPeriods() throws SQLException {
        String sql = "SELECT strftime('%Y', " + CONVERT_TIME_SQL + ") AS year,\n" +
                "strftime('%m', " + CONVERT_TIME_SQL + ") AS month\n" +
                "FROM " + getTableName() + "\n" +
                "GROUP BY year, month\n" +
                "ORDER BY year DESC, month DESC;";
        ResultSet resultSet = connection.prepareStatement(sql).executeQuery();
        List<YearMonth> result = new ArrayList<>();
        while (resultSet.next()) {
            int year = resultSet.getInt(1);
            int month = resultSet.getInt(2);
            result.add(new YearMonth(year, month));
        }
        return result;
    }

    public List<CategorySummary> getCategorySummary(@Nullable String period) throws SQLException {
        String sql = "SELECT " + C_CATEGORY_ID + ", COUNT(*) AS count, SUM(" + C_PRICE + ") AS total_price " +
                "FROM logs";
        if (period != null) {
            sql += " WHERE strftime('%Y-%m', " + CONVERT_TIME_SQL + ") = '" + period + "'";
        }
        sql += " GROUP BY " + C_CATEGORY_ID;
        List<CategorySummary> result = new ArrayList<>();
        ResultSet resultSet = connection.prepareStatement(sql).executeQuery();
        while (resultSet.next()) {
            Category category = Category.findById(resultSet.getInt(1));
            int count = resultSet.getInt(2);
            int priceSum = resultSet.getInt(3);
            result.add(new CategorySummary(category, count, priceSum));
        }
        return result;
    }

    public List<MonthlyCategorySummary> getExtendedMonthlySummary(int months) throws SQLException {
        String monthColumn = "month";
        String countColumn = "count";
        String totalPriceColumn = "total_price";
        String sql = "SELECT strftime('%Y-%m', " + CONVERT_TIME_SQL + ") AS " + monthColumn + ",\n" +
                C_CATEGORY_ID + ",\n" +
                "COUNT(*) AS " + countColumn + ",\n" +
                "SUM(" + C_PRICE + ") AS " + totalPriceColumn + "\n" +
                "FROM " + getTableName() + "\n" +
                "WHERE strftime('%Y-%m', " + CONVERT_TIME_SQL + ") >= strftime('%Y-%m', 'now', '-" + months + " months')\n" +
                "GROUP BY " + monthColumn + ", " + C_CATEGORY_ID + "\n" +
                "ORDER BY " + monthColumn + " DESC, " + C_CATEGORY_ID;

        List<MonthlyCategorySummary> result = new ArrayList<>();
        ResultSet resultSet = connection.prepareStatement(sql).executeQuery();
        List<CategorySummary> categorySummaries = new ArrayList<>();
        String prevPeriod = null;
        while (resultSet.next()) {
            // month, cat_id, count, total_price
            String currentPeriod = resultSet.getString(monthColumn);
            Category category = Category.findById(resultSet.getInt(C_CATEGORY_ID));
            int count = resultSet.getInt(countColumn);
            int priceSum = resultSet.getInt(totalPriceColumn);
            if (!currentPeriod.equals(prevPeriod)) {
                if (prevPeriod != null) {
                    result.add(new MonthlyCategorySummary(categorySummaries, prevPeriod));
                    categorySummaries = new ArrayList<>();
                }
                categorySummaries.add(new CategorySummary(category, count, priceSum));
                prevPeriod = currentPeriod;
            } else {
                categorySummaries.add(new CategorySummary(category, count, priceSum));
            }
        }
        if (prevPeriod != null) {
            result.add(new MonthlyCategorySummary(categorySummaries, prevPeriod));
        }
        return result;
    }

    public List<MonthlySummary> getMonthlySummary(int months) throws SQLException {
        String monthColumn = "month";
        String totalPriceColumn = "total_price";
        String sql = "SELECT strftime('%Y-%m', " + CONVERT_TIME_SQL + ") AS " + monthColumn + ",\n" +
                "SUM(" + C_PRICE + ") AS " + totalPriceColumn + "\n" +
                "FROM " + getTableName() + "\n" +
                "WHERE strftime('%Y-%m', " + CONVERT_TIME_SQL + ") >= strftime('%Y-%m', 'now', '-" + months + " months')\n" +
                "GROUP BY " + monthColumn + "\n" +
                "ORDER BY " + monthColumn + " DESC";

        List<MonthlySummary> result = new ArrayList<>();
        ResultSet resultSet = connection.prepareStatement(sql).executeQuery();
        while (resultSet.next()) {
            String period = resultSet.getString(monthColumn);
            int priceSum = resultSet.getInt(totalPriceColumn);
            result.add(new MonthlySummary(period, priceSum));
        }
        return result;
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        Statement stmt = connection.createStatement();

        String sql = "CREATE TABLE IF NOT EXISTS " + getTableName() + " ("
                + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + C_DATE + " DATE,"
                + C_DESCRIPTION + " TEXT,"
                + C_PRICE + " INTEGER,"
                + C_CATEGORY_ID + " INTEGER,"
                + C_SESSION_TYPE_ID + " INTEGER NULL"
                + ");";

        stmt.execute(sql);
    }
}