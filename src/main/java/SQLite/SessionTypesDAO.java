package SQLite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import SQLite.model.SessionType;
import SQLite.model.SessionTypesTable;

import static SQLite.model.SessionTypesTable.C_ID;
import static SQLite.model.SessionTypesTable.C_NAME;
import static SQLite.model.SessionTypesTable.TABLE_NAME;

@Component
public class SessionTypesDAO extends DAO {

    public SessionTypesDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        Statement stmt = connection.createStatement();

        String sql = "CREATE TABLE IF NOT EXISTS " + getTableName() + " ("
                + SessionTypesTable.C_ID + " INTEGER PRIMARY KEY,"
                + SessionTypesTable.C_NAME + " TEXT"
                + ");";

        stmt.execute(sql);
    }

    public int getSessionTypeSize() throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "SELECT COUNT(*) FROM " + getTableName() + ";";
        ResultSet resultSet = statement.executeQuery(sql);
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        throw new SQLException("Error while getting SessionType size");
    }

    public void fillSessionTypes() throws SQLException {
        Set<SessionType> allSessionTypes = new HashSet<>(Arrays.asList(SessionType.values()));
        addSessionTypes(allSessionTypes);
    }

    public void addSessionTypes(Set<SessionType> sessionTypes) throws SQLException {
        String sql = "INSERT INTO " + getTableName() + " (" + C_ID + ", " + C_NAME + ") VALUES (?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        for (SessionType sessionType : sessionTypes) {
            stmt.setInt(1, sessionType.getId());
            stmt.setString(2, sessionType.getName());
            stmt.addBatch();
        }

        stmt.executeBatch();
    }

    public Set<SessionType> getAllSessionTypes() throws SQLException {
        Set<SessionType> result = new HashSet<>();
        Statement stmt = connection.createStatement();

        String sql = "SELECT " + C_NAME + " FROM " + getTableName() + ";";
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            String name = rs.getString(C_NAME);
            SessionType sessionType = SessionType.findByName(name);
            result.add(sessionType);
        }
        return result;
    }

    public void deleteSessionType(String name) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "DELETE FROM " + getTableName() + " WHERE " + C_NAME + " = '" + name + "';";
        stmt.executeUpdate(sql);
    }
}
