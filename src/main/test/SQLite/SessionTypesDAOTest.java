package SQLite;

import java.sql.SQLException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import SQLite.model.SessionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
public class SessionTypesDAOTest {

    @Autowired
    private SessionTypesDAO dao;

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
        Set<SessionType> allSessionTypes = Set.of(SessionType.ALL, SessionType.TO_ALL, SessionType.RANG);
        try {
            dao.addSessionTypes(allSessionTypes);
            Set<SessionType> sessionTypes = dao.getAllSessionTypes();

            assertEquals(sessionTypes.size(), allSessionTypes.size(), String.format("sessionTypes got: %s\n allSessionTypes: %s", sessionTypes, allSessionTypes));
            assertTrue(sessionTypes.containsAll(allSessionTypes));
            assertTrue(allSessionTypes.containsAll(sessionTypes));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void addDuplicateCategoryTest() {
        try {
            dao.addSessionTypes(Set.of(SessionType.ALL));
            dao.addSessionTypes(Set.of(SessionType.ALL));
        } catch (SQLException e) {
            String error = "[SQLITE_CONSTRAINT_PRIMARYKEY] A PRIMARY KEY constraint failed (UNIQUE constraint failed: session_type.id)";
            assertEquals(error, e.getMessage());
            return;
        }
        fail("Тест должен был упасть при попытке добавления нового session type");
    }

    @Test
    public void deleteCategoryTest() {
        try {
            dao.addSessionTypes(Set.of(SessionType.ALL, SessionType.RANG));
            assertEquals(dao.getSessionTypeSize(), 2);

            dao.deleteSessionType(SessionType.ALL.getName());
            assertEquals(dao.getSessionTypeSize(), 1);

            Set<SessionType> sessionTypes = dao.getAllSessionTypes();
            assertEquals(sessionTypes.size(), 1);
            assertTrue(sessionTypes.contains(SessionType.RANG));
        } catch (SQLException e) {
            fail(e);
        }
    }
}
