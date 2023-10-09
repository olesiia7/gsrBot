package bot.gsr.repository;

import bot.gsr.SQLite.LogsFilter;
import bot.gsr.TestConfig;
import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static bot.gsr.SQLite.model.Category.SESSION;
import static bot.gsr.SQLite.model.SessionType.SR;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestConfig.class)
class LogRepositoryImplTest {

    @SuppressWarnings("unused")
    @Autowired
    private LogRepositoryImpl logRepository;

    @BeforeEach
    void setUp() {
        logRepository.createTableIfNotExists();
    }

    @AfterEach
    void tearDown() {
        logRepository.clearAllData();
    }

    @Test
    @DisplayName("Добавление логов")
    void addLog() {
        Date now = Date.valueOf(LocalDate.now());
        Log log = new Log(now, "desc", 2600, Category.SESSION, SessionType.RANG);
        logRepository.addLog(log);
        List<Log> logs = logRepository.getLogs(LogsFilter.EMPTY);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());

        assertEquals(log, logs.get(0));

        logRepository.clearAllData();
        log = new Log(now, "desc", 2600, Category.SESSION, null);
        logRepository.addLog(log);
        logs = logRepository.getLogs(LogsFilter.EMPTY);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());

        assertEquals(log, logs.get(0));
    }

    @Test
    @DisplayName("Работа фильтров поиска логов")
    void getLogs() {
        Date now = Date.valueOf(LocalDate.now());
        String desc1 = "desc1";
        Log log1 = new Log(now, desc1, 2600, Category.SESSION, SessionType.SR);
        logRepository.addLog(log1);

        Date dayAgo = Date.valueOf(LocalDate.now().minusDays(1));
        String desc2 = "desc2";
        Log log2 = new Log(dayAgo, desc2, 4000, Category.DIAGNOSTIC, null);
        logRepository.addLog(log2);

        List<Log> logs = logRepository.getLogs(LogsFilter.EMPTY);
        assertEquals(2, logs.size());

        // date
        LogsFilter.Builder builder = new LogsFilter.Builder().setDate(dayAgo);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log2, logs.get(0));

        // description
        builder = new LogsFilter.Builder().setDescription(desc1);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        // category
        builder = new LogsFilter.Builder().setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        // session type
        builder = new LogsFilter.Builder().setSessionType(SR);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        // description + category
        builder = new LogsFilter.Builder()
                .setDescription(log1.description())
                .setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        builder = new LogsFilter.Builder()
                .setDescription(log2.description())
                .setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(0, logs.size());

        // добавим еще одну запись
        logRepository.addLog(log1);
        builder = new LogsFilter.Builder().setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(2, logs.size());
        List<Log> expected = List.of(log1, log1);
        assertTrue(expected.containsAll(logs));
        assertTrue(logs.containsAll(expected));
    }

    @Test
    @DisplayName("Удаление и создание таблицы и enum")
    void createTableIfNotExists() {
        boolean tableExists = logRepository.isTableExists();
        assertTrue(tableExists);

        logRepository.dropTableIfExists();
        tableExists = logRepository.isTableExists();
        assertFalse(tableExists);

        logRepository.createTableIfNotExists();
        tableExists = logRepository.isTableExists();
        assertTrue(tableExists);
    }
}