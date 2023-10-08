package bot.gsr.SQLite;

import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.Log;
import bot.gsr.SQLite.model.SessionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
public class LogsDAOTest {

    @Autowired
    private LogsDAO dao;

    @BeforeEach
    public void setUp() throws SQLException {
        dao.createTableIfNotExists();
    }

    @AfterEach
    public void clear() {
        dao.clearAllData();
    }

    @Test
    public void getLogs() {
        try {
            Date now = Date.valueOf(LocalDate.now());
            String desc1 = "desc1";
            Log item1 = new Log(now, desc1, 2600, Category.SESSION, SessionType.SR);
            int id1 = dao.addLog(item1);
            Log log1 = new Log(id1, item1);

            Date dayAgo = Date.valueOf(LocalDate.now().minusDays(1));
            String desc2 = "desc2";
            Log item2 = new Log(dayAgo, desc2, 4000, Category.DIAGNOSTIC, null);
            int id2 = dao.addLog(item2);
            Log log2 = new Log(id2, item2);

            List<Log> logs = dao.getLogs(LogsFilter.EMPTY);
            assertEquals(2, logs.size());

            // id
            LogsFilter.Builder builder = new LogsFilter.Builder().setId(id1);
            logs = dao.getLogs(builder.build());
            assertEquals(1, logs.size());
            assertEquals(log1, logs.get(0));

            // date
            builder = new LogsFilter.Builder().setDate(dayAgo);
            logs = dao.getLogs(builder.build());
            assertEquals(1, logs.size());
            assertEquals(log2, logs.get(0));

            // description
            builder = new LogsFilter.Builder().setDescription(desc1);
            logs = dao.getLogs(builder.build());
            assertEquals(1, logs.size());
            assertEquals(log1, logs.get(0));

            // category
            builder = new LogsFilter.Builder().setCategory(Category.SESSION);
            logs = dao.getLogs(builder.build());
            assertEquals(1, logs.size());
            assertEquals(log1, logs.get(0));

            // session type
            builder = new LogsFilter.Builder().setSessionType(SessionType.SR);
            logs = dao.getLogs(builder.build());
            assertEquals(1, logs.size());
            assertEquals(log1, logs.get(0));

            // id + category
            builder  = new LogsFilter.Builder()
                    .setId(log1.id())
                    .setCategory(Category.SESSION);
            logs = dao.getLogs(builder.build());
            assertEquals(1, logs.size());
            assertEquals(log1, logs.get(0));

            builder  = new LogsFilter.Builder()
                    .setId(log2.id())
                    .setCategory(Category.SESSION);
            logs = dao.getLogs(builder.build());
            assertEquals(0, logs.size());

            // добавим еще одну запись
            int id3 = dao.addLog(item1);
            Log log3 = new Log(id3, item1);
            builder = new LogsFilter.Builder().setCategory(Category.SESSION);
            logs = dao.getLogs(builder.build());
            assertEquals(2, logs.size());
            List<Log> expected = List.of(log1, log3);
            assertTrue(expected.containsAll(logs));
            assertTrue(logs.containsAll(expected));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void addLogTest() {
        Date date = Date.valueOf(LocalDate.now());
        String description = "description";
        int price = 2600;
        Category category = Category.SESSION;

        Log item = new Log(date, description, price, category, null);
        try {
            int id = dao.addLog(item);
            List<Log> logs = dao.getLogs(LogsFilter.EMPTY);
            assertEquals(1, logs.size());
            Log log = logs.get(0);
            assertEquals(id, log.id());
            assertEquals(date, log.date());
            assertEquals(description, log.description());
            assertEquals(price, log.price());
            assertEquals(category.getId(), log.category().getId());
        } catch (SQLException e) {
            fail(e.getMessage() + "\n" + e.getSQLState());
        }
    }
}
