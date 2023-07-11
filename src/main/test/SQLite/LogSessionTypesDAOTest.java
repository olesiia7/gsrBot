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
public class LogSessionTypesDAOTest {

    @Autowired
    private LogSessionTypesDAO dao;

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
        Set<SessionType> allSessionTypes = Set.of(SessionType.SR, SessionType.RANG, SessionType.TO_ALL);
        try {
            dao.addLogSessionTypes(1, allSessionTypes);
            Set<SessionType> sessionTypes = dao.getSessionTypesById(1);

            assertEquals(allSessionTypes.size(), sessionTypes.size(), String.format("expected: %s\n got: %s", allSessionTypes, sessionTypes));
            assertTrue(sessionTypes.containsAll(allSessionTypes));
            assertTrue(allSessionTypes.containsAll(sessionTypes));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
}
