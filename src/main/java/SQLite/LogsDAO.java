package SQLite;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import SQLite.model.CategoriesTable;
import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.LogItem;
import SQLite.model.LogSessionTypesTable;
import SQLite.model.LogsTable;
import SQLite.model.SessionType;
import SQLite.model.SessionTypesTable;

import static SQLite.model.LogSessionTypesTable.getLogSessionTypeField;
import static SQLite.model.LogsTable.C_CATEGORY_ID;
import static SQLite.model.LogsTable.C_DATE;
import static SQLite.model.LogsTable.C_DESCRIPTION;
import static SQLite.model.LogsTable.C_ID;
import static SQLite.model.LogsTable.C_PRICE;
import static SQLite.model.LogsTable.TABLE_NAME;
import static SQLite.model.LogsTable.getLogField;
import static SQLite.model.SessionTypesTable.getSessionTypeField;

@Component
public class LogsDAO extends DAO {
    private static final String COLUMNS_WITHOUT_ID = C_DATE + "," + C_DESCRIPTION + "," + C_PRICE + "," + C_CATEGORY_ID;
    private static final String ALL_COLUMNS = C_ID + "," + COLUMNS_WITHOUT_ID;
    private static final String SESSION_TYPE_NAMES = "session_type_names";
    private static final String GET_LAST_ID_SQL = "SELECT last_insert_rowid();";
    private final String SELECT_ALL = "SELECT " + TABLE_NAME + "." + ALL_COLUMNS + ",GROUP_CONCAT(" + getSessionTypeField(SessionTypesTable.C_ID) + ") AS " + SESSION_TYPE_NAMES + "\n" +
            "FROM " + getTableName() + "\n" +
            "LEFT JOIN " + LogSessionTypesTable.TABLE_NAME + " ON " + getLogField(LogsTable.C_ID) + " = " + getLogSessionTypeField(LogSessionTypesTable.C_LOG_ID) + "\n" +
            "LEFT JOIN " + SessionTypesTable.TABLE_NAME + " ON " + getLogSessionTypeField(LogSessionTypesTable.C_SESSION_TYPE_ID) + " = " + getSessionTypeField(SessionTypesTable.C_ID) + "\n";

    public LogsDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    public List<Log> getLogs(LogsFilter filter) throws SQLException {
        Statement stmt = connection.createStatement();
        final String sql = SELECT_ALL +
                buildWhere(filter) +
                "\nGROUP BY " + getLogField(C_ID);

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
                "\nGROUP BY " + getLogField(C_ID) + "\n" +
                "ORDER BY " + C_DATE + " DESC\n" +
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
        int category_id = rs.getInt(C_CATEGORY_ID);
        String sessionTypeIds = rs.getString(SESSION_TYPE_NAMES);
        Set<SessionType> sessionTypes = null;
        if (sessionTypeIds != null) {
            sessionTypes = new HashSet<>();
            Arrays.stream(sessionTypeIds.split(","))
                    .map(Integer::valueOf)
                    .map(SessionType::findById)
                    .forEach(sessionTypes::add);
        }
        return new Log(id, date, description, price, Category.findById(category_id), sessionTypes);
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
        if (filter.getSessionTypes() != null) {
            if (needAnd) {
                where.append(" AND ");
            }
            String sessionTypes = filter.getSessionTypes().stream()
                    .map(SessionType::getId)
                    .map(Objects::toString)
                    .collect(Collectors.joining(","));
            where.append(getSessionTypeField(SessionTypesTable.C_ID))
                    .append(" IN (").append(sessionTypes).append(")");
        }
        return where.toString();
    }

    public int addLog(LogItem logItem) throws SQLException {
        String sql = "INSERT INTO " + getTableName() + " (" + COLUMNS_WITHOUT_ID + ") VALUES (?,?,?,?);";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setDate(1, logItem.date());
        stmt.setString(2, logItem.description());
        stmt.setInt(3, logItem.price());
        stmt.setInt(4, logItem.category_id());
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
                + "FOREIGN KEY (" + C_CATEGORY_ID + ") REFERENCES " + CategoriesTable.TABLE_NAME + "(id)"
                + ");";

        stmt.execute(sql);
    }
}