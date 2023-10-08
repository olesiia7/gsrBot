package bot.gsr.SQLite;

import java.sql.SQLException;

public abstract class Service<T extends DAO> {
    protected final T dao;

    protected Service(T dao) {
        this.dao = dao;
    }

    public void createTableIfNotExists() throws SQLException {
        try {
            dao.createTableIfNotExists();
        } catch (SQLException e) {
            System.out.printf("Error while creating table %s: %s\n", dao.getTableName(), e.getMessage());
            throw e;
        }
    }

    public void clearAllData() {
        dao.clearAllData();
    }

    public void dropTable() {
        dao.dropTable();
    }
}
