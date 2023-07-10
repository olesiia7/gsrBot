package SQLite;

import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

import SQLite.model.CategoriesTable;
import SQLite.model.LogsTable;

import static SQLite.model.LogSessionTypesTable.C_ID;
import static SQLite.model.LogSessionTypesTable.C_SESSION_TYPE_ID;
import static SQLite.model.LogSessionTypesTable.TABLE_NAME;

@Component
public class LogSessionTypesDAO extends DAO {

    public LogSessionTypesDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                C_ID + " INTEGER INUQUE, " +
                C_SESSION_TYPE_ID + " INTEGER INUQUE," +
                " FOREIGN KEY (" + C_ID + ") REFERENCES " +
                LogsTable.TABLE_NAME + "(" + LogsTable.C_ID + ")," +
                "FOREIGN KEY (" + C_SESSION_TYPE_ID + ") REFERENCES " +
                CategoriesTable.TABLE_NAME + "(" + CategoriesTable.C_ID + ")," +
                " PRIMARY KEY (" + C_ID + ", " + C_SESSION_TYPE_ID + ")" +
                ")";

        stmt.execute(sql);
    }
}
