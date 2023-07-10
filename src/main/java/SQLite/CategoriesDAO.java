package SQLite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import SQLite.model.Category;

import static SQLite.model.CategoriesTable.C_ID;
import static SQLite.model.CategoriesTable.C_NAME;
import static SQLite.model.CategoriesTable.TABLE_NAME;

@Component
public class CategoriesDAO extends DAO {

    public CategoriesDAO(ConnectionManager connectionManager) {
        super(connectionManager.getConnection(), TABLE_NAME);
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        Statement stmt = connection.createStatement();

        String sql = "CREATE TABLE IF NOT EXISTS " + getTableName() + " ("
                + C_ID + " INTEGER PRIMARY KEY UNIQUE,"
                + C_NAME + " TEXT UNIQUE"
                + ");";

        stmt.execute(sql);
    }

    public void fillCategories() throws SQLException {
        Set<Category> allCategories = new HashSet<>(Arrays.asList(Category.values()));
        addCategories(allCategories);
    }

    public void addCategories(Set<Category> categories) throws SQLException {
        String sql = "INSERT INTO " + getTableName() + " (" + C_ID + ", " + C_NAME + ") VALUES (?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        for (Category category : categories) {
            stmt.setInt(1, category.getId());
            stmt.setString(2, category.getName());
            stmt.addBatch();
        }

        stmt.executeBatch();
    }

    public void deleteCategory(String name) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "DELETE FROM " + getTableName() + " WHERE " + C_NAME + " = '" + name + "';";
        stmt.executeUpdate(sql);
    }

    public int getCategoriesSize() throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "SELECT COUNT(*) FROM " + getTableName() + ";";
        ResultSet resultSet = statement.executeQuery(sql);
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        throw new SQLException("Error while getting Categories size");
    }

    public Set<Category> getAllCategories() throws SQLException {
        Set<Category> result = new HashSet<>();
        Statement stmt = connection.createStatement();

        String sql = "SELECT " + C_NAME + " FROM " + getTableName() + ";";
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            String name = rs.getString(C_NAME);
            final Category category = Category.findByName(name);
            result.add(category);
        }
        return result;
    }
}
