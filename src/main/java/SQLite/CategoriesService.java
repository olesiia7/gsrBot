package SQLite;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import SQLite.model.Category;

@Component
public class CategoriesService extends Service<CategoriesDAO> {

    public CategoriesService(CategoriesDAO dao) {
        super(dao);
    }

    /**
     * Наполняет табличку с данными в соответствии с {@link SQLite.model.Category}
     */
    public void init() throws SQLException {
        int existCategoriesSize = dao.getCategoriesSize();
        // если в таблице лишние категории, то метод выкинет IllegalArgument
        final Set<Category> allCategories = new HashSet<>(Arrays.asList(Category.values()));
        if (existCategoriesSize != allCategories.size()) {
            if (existCategoriesSize == 0) {
                dao.fillCategories();
            } else { // если не хватает категорий – добавляем
                Set<Category> existCategories = dao.getAllCategories();
                if (existCategoriesSize < allCategories.size()) {
                    existCategories.forEach(allCategories::remove);
                    dao.addCategories(allCategories);
                    System.out.printf("Added new categories: %s\n", allCategories);
                }
            }
        }
    }

    public Set<Category> getAllCategories() throws SQLException {
        return dao.getAllCategories();
    }
}
