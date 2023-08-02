package SQLite;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
class dbCotrollerTest {

    @Autowired
    private DbController controller;


    @BeforeEach
    public void setUp() throws SQLException {
        controller.createTablesIfNotExists();
    }

    @AfterEach
    public void clear() {
        controller.clearAllData();
    }

    @Test
    public void getLogsTest() {
        try {
            Date now = Date.valueOf(LocalDate.now());
            Log log = new Log(null, now, "desc", 2600, Category.SESSION, SessionType.RANG);
            int id = controller.addLog(log);

            List<Log> logs = controller.getLogs(LogsFilter.EMPTY);
            assertEquals(1, logs.size());

            Log expected = new Log(id, log);
            assertEquals(expected, logs.get(0));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createTablesTest() {
        try {
            controller.dropTables();
            controller.createTablesIfNotExists();
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

}