package SQLite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import SQLite.model.CategoriesTable;
import SQLite.model.LogsTable;
import SQLite.model.SessionType;

import static SQLite.model.LogSessionTypesTable.C_LOG_ID;
import static SQLite.model.LogSessionTypesTable.C_SESSION_TYPE_ID;
import static SQLite.model.LogSessionTypesTable.TABLE_NAME;

@Component
public class LogSessionTypesDAO extends DAO {

    public LogSessionTypesDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    public void addLogSessionTypes(int id, Set<SessionType> sessionTypes) throws SQLException {
        String sql = "INSERT INTO " + getTableName() + " (" + C_LOG_ID + ", " + C_SESSION_TYPE_ID + ") VALUES (?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        for (SessionType sessionType : sessionTypes) {
            stmt.setInt(1, id);
            stmt.setInt(2, sessionType.getId());
            stmt.addBatch();
        }

        stmt.executeBatch();
    }

    public Set<SessionType> getSessionTypesById(int id) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "SELECT " + C_SESSION_TYPE_ID + " FROM " + getTableName() +
                " WHERE " + C_LOG_ID + "=" + id;
        ResultSet rs = stmt.executeQuery(sql);

        Set<SessionType> sessionTypes = new HashSet<>();
        while (rs.next()) {
            int sessionTypeId = rs.getInt(C_SESSION_TYPE_ID);
            sessionTypes.add(SessionType.findById(sessionTypeId));
        }
        return sessionTypes;
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                C_LOG_ID + " INTEGER, " +
                C_SESSION_TYPE_ID + " INTEGER," +
                " FOREIGN KEY (" + C_LOG_ID + ") REFERENCES " +
                LogsTable.TABLE_NAME + "(" + LogsTable.C_ID + ")," +
                "FOREIGN KEY (" + C_SESSION_TYPE_ID + ") REFERENCES " +
                CategoriesTable.TABLE_NAME + "(" + CategoriesTable.C_ID + ")," +
                " PRIMARY KEY (" + C_LOG_ID + ", " + C_SESSION_TYPE_ID + ")" +
                ")";

        stmt.execute(sql);
    }
}
