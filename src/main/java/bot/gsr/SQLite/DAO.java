package bot.gsr.SQLite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DAO {
    protected final Connection connection;
    protected final String tableName;

    public DAO(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public abstract void createTableIfNotExists() throws SQLException;

    public void dropTable() {
        try (Statement statement = connection.createStatement()) {
            String sql = "DROP TABLE IF EXISTS " + getTableName() + ";";
            statement.execute(sql);
        } catch (SQLException e) {
            System.out.printf("Error while deleting table %s: %s\n", getTableName(), e.getMessage());
        }
    }

    public void clearAllData() {
        try (Statement statement = connection.createStatement()) {
            String sql = "DELETE FROM " + getTableName() + ";";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.printf("Error while clearing data from table %s: %s\n", getTableName(), e.getMessage());
        }
    }
}
