package SQLite;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import SQLite.model.Category;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
public class CategoriesServiceTest {

    @Autowired
    private CategoriesService service;

    @MockBean
    private CategoriesDAO dao;

    private static final HashSet<Category> ALL_CATEGORIES = new HashSet<>(Arrays.asList(Category.values()));

    @Test
    public void testInitFromScratch() throws SQLException {
        when(dao.getCategoriesSize()).thenReturn(0);
        service.init();
        Mockito.verify(dao, Mockito.times(1)).getCategoriesSize();
        Mockito.verify(dao, Mockito.times(1)).fillCategories();
        Mockito.verifyNoMoreInteractions(dao);
    }

    @Test
    public void testInitNoDifference() throws SQLException {
        when(dao.getCategoriesSize()).thenReturn(Category.values().length);
        service.init();
        Mockito.verify(dao, Mockito.times(1)).getCategoriesSize();
        Mockito.verifyNoMoreInteractions(dao);
    }

    @Test // в базе меньше категорий, чем в коде
    public void testInitNotEnoughCategories() throws SQLException {
        when(dao.getCategoriesSize()).thenReturn(Category.values().length - 1);

        HashSet<Category> lessCategories = new HashSet<>(ALL_CATEGORIES);
        lessCategories.remove(Category.SESSION);
        when(dao.getAllCategories()).thenReturn(lessCategories);

        service.init();
        Mockito.verify(dao, Mockito.times(1)).getCategoriesSize();
        Mockito.verify(dao, Mockito.times(1)).getAllCategories();
        Mockito.verify(dao, Mockito.times(1)).addCategories(Set.of(Category.SESSION));
        Mockito.verifyNoMoreInteractions(dao);
    }

    @Test // в базе больше категорий, чем в коде
    public void testInitTooManyCategories() throws SQLException {
        when(dao.getCategoriesSize()).thenReturn(Category.values().length + 1);
        IllegalArgumentException expected = new IllegalArgumentException("No enum constant with name: NEW_CATEGORY");
        when(dao.getAllCategories()).thenThrow(expected);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.init());
        Assertions.assertEquals(expected, exception);
        Mockito.verify(dao, times(1)).getCategoriesSize();
        Mockito.verify(dao, times(1)).getAllCategories();
        Mockito.verifyNoMoreInteractions(dao);
    }
}
