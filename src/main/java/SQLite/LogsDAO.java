package SQLite;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.CategoriesTable;
import SQLite.model.LogItem;

import static SQLite.model.LogsTable.C_CATEGORY_ID;
import static SQLite.model.LogsTable.C_DATE;
import static SQLite.model.LogsTable.C_DESCRIPTION;
import static SQLite.model.LogsTable.C_ID;
import static SQLite.model.LogsTable.C_PRICE;
import static SQLite.model.LogsTable.TABLE_NAME;

@Component
public class LogsDAO extends DAO {

    public LogsDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    public List<LogItem> getLogItems() {
        return Collections.emptyList();
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
                + "FOREIGN KEY (" + C_CATEGORY_ID + ") REFERENCES " + CategoriesTable.TABLE_NAME +  "(id)"
                + ");";

        stmt.execute(sql);
    }
}