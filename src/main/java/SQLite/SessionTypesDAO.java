package SQLite;

import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

import SQLite.model.SessionTypesTable;

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
}
