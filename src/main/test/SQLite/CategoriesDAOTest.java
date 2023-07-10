package SQLite;

import java.sql.SQLException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import SQLite.model.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
public class CategoriesDAOTest {

    @Autowired
    private CategoriesDAO dao;

    @BeforeEach
    public void setUp() throws SQLException {
        dao.createTableIfNotExists();
    }

    @AfterEach
    public void clear() {
        dao.clearAllData();
    }

    @Test
    public void addCategoryTest() {
        Set<Category> allCategories = Set.of(Category.SESSION, Category.SOURCE, Category.DIAGNOSTIC);
        try {
            dao.addCategories(allCategories);
            Set<Category> categories = dao.getAllCategories();

            assertEquals(categories.size(), allCategories.size(), String.format("categories got: %s\n allCategories: %s", categories, allCategories));
            assertTrue(categories.containsAll(allCategories));
            assertTrue(allCategories.containsAll(categories));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void addDuplicateCategoryTest() {
        try {
            dao.addCategories(Set.of(Category.SESSION));
            dao.addCategories(Set.of(Category.SESSION));
        } catch (SQLException e) {
            String error = "[SQLITE_CONSTRAINT_PRIMARYKEY] A PRIMARY KEY constraint failed (UNIQUE constraint failed: category.id)";
            assertEquals(error, e.getMessage());
            return;
        }
        fail("Тест должен был упасть при попытке добавления новой категории");
    }

    @Test
    public void deleteCategoryTest() {
        try {
            dao.addCategories(Set.of(Category.SESSION, Category.SOURCE));
            assertEquals(dao.getCategoriesSize(), 2);

            dao.deleteCategory(Category.SESSION.getName());
            assertEquals(dao.getCategoriesSize(), 1);

            Set<Category> categories = dao.getAllCategories();
            assertEquals(categories.size(), 1);
            assertTrue(categories.contains(Category.SOURCE));
        } catch (SQLException e) {
            fail(e);
        }
    }
}
