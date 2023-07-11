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

import SQLite.model.SessionType;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
public class SessionTypesServiceTest {

    @Autowired
    private SessionTypesService service;

    @MockBean
    private SessionTypesDAO dao;

    private static final HashSet<SessionType> ALL_SESSION_TYPES = new HashSet<>(Arrays.asList(SessionType.values()));

    @Test
    public void testInitFromScratch() throws SQLException {
        when(dao.getSessionTypeSize()).thenReturn(0);
        service.init();
        Mockito.verify(dao, Mockito.times(1)).getSessionTypeSize();
        Mockito.verify(dao, Mockito.times(1)).fillSessionTypes();
        Mockito.verifyNoMoreInteractions(dao);
    }

    @Test
    public void testInitNoDifference() throws SQLException {
        when(dao.getSessionTypeSize()).thenReturn(SessionType.values().length);
        service.init();
        Mockito.verify(dao, Mockito.times(1)).getSessionTypeSize();
        Mockito.verifyNoMoreInteractions(dao);
    }

    @Test // в базе меньше категорий, чем в коде
    public void testInitNotEnoughCategories() throws SQLException {
        when(dao.getSessionTypeSize()).thenReturn(SessionType.values().length - 1);

        HashSet<SessionType> lessCategories = new HashSet<>(ALL_SESSION_TYPES);
        lessCategories.remove(SessionType.SR);
        when(dao.getAllSessionTypes()).thenReturn(lessCategories);

        service.init();
        Mockito.verify(dao, Mockito.times(1)).getSessionTypeSize();
        Mockito.verify(dao, Mockito.times(1)).getAllSessionTypes();
        Mockito.verify(dao, Mockito.times(1)).addSessionTypes(Set.of(SessionType.SR));
        Mockito.verifyNoMoreInteractions(dao);
    }

    @Test // в базе больше категорий, чем в коде
    public void testInitTooManyCategories() throws SQLException {
        when(dao.getSessionTypeSize()).thenReturn(SessionType.values().length + 1);
        IllegalArgumentException expected = new IllegalArgumentException("No enum constant with name: NEW_SESSION_TYPE");
        when(dao.getAllSessionTypes()).thenThrow(expected);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.init());
        Assertions.assertEquals(expected, exception);
        Mockito.verify(dao, times(1)).getSessionTypeSize();
        Mockito.verify(dao, times(1)).getAllSessionTypes();
        Mockito.verifyNoMoreInteractions(dao);
    }
}
