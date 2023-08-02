package SQLite;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;

import static SQLite.model.LogsTable.C_CATEGORY_ID;
import static SQLite.model.LogsTable.C_DATE;
import static SQLite.model.LogsTable.C_DESCRIPTION;
import static SQLite.model.LogsTable.C_ID;
import static SQLite.model.LogsTable.C_PRICE;
import static SQLite.model.LogsTable.C_SESSION_TYPE_ID;
import static SQLite.model.LogsTable.TABLE_NAME;
import static SQLite.model.LogsTable.getLogField;

@Component
public class LogsDAO extends DAO {
    private static final String COLUMNS_WITHOUT_ID = C_DATE + "," + C_DESCRIPTION + "," + C_PRICE + "," + C_CATEGORY_ID + "," + C_SESSION_TYPE_ID;
    private static final String ALL_COLUMNS = C_ID + "," + COLUMNS_WITHOUT_ID;
    private static final String GET_LAST_ID_SQL = "SELECT last_insert_rowid();";
    private final String SELECT_ALL = "SELECT * FROM " + getTableName();

    public LogsDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        Statement stmt = connection.createStatement();
        final String sql = SELECT_ALL +
                buildWhere(filter);

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