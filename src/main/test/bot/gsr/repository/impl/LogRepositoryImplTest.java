package bot.gsr.repository.impl;

import bot.gsr.SQLite.LogsFilter;
import bot.gsr.TestConfig;
import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import bot.gsr.telegram.model.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static bot.gsr.SQLite.model.Category.DIAGNOSTIC;
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

    @Test
    @DisplayName("Создание дампа")
    void makeDump() {
        Date date = Date.valueOf("2023-10-15");
        String desc1 = "desc1";
        Log log1 = new Log(date, desc1, 2600, Category.SESSION, SessionType.SR);
        logRepository.addLog(log1);

        Date date2 = Date.valueOf("2023-10-14");
        String desc2 = "desc2";
        Log log2 = new Log(date2, desc2, 4000, Category.DIAGNOSTIC, null);
        logRepository.addLog(log2);

        logRepository.addLog(log1);

        String backupFilePath = "src/main/test/resources/dump.csv";
        logRepository.makeDump(backupFilePath);
        String expectedCsvOutput = """
                date,description,price,category,session_type
                "2023-10-14","desc2",4000,"Диагностика",
                "2023-10-15","desc1",2600,"Сессия","Судьба Рода"
                "2023-10-15","desc1",2600,"Сессия","Судьба Рода"
                """;

        try {
            List<String> lines = Files.readAllLines(Paths.get(backupFilePath));
            StringBuilder scvOutput = new StringBuilder();

            for (String line : lines) {
                scvOutput.append(line).append("\n");
            }

            String fileContent = scvOutput.toString();
            assertEquals(expectedCsvOutput, fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @DisplayName("Применение дампа")
    void applyDump() {
        String backupFilePath = "src/main/test/resources/dump.csv";
        String csvBackup = """
                date,description,price,category,session_type
                "2023-10-14","desc2",4000,"Диагностика",
                "2023-10-15","desc1",2600,"Сессия","Судьба Рода"
                "2023-10-15","desc1",2600,"Сессия","Судьба Рода\"""";

        try {
            Files.write(Paths.get(backupFilePath), csvBackup.getBytes());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            fail();
        }

        String currentDirectory = System.getProperty("user.dir");
        logRepository.applyDump(currentDirectory + "/" + backupFilePath);

        List<Log> logs = logRepository.getLogs(LogsFilter.EMPTY);
        assertEquals(3, logs.size());

        Log log1 = new Log(Date.valueOf("2023-10-15"), "desc1", 2600, Category.SESSION, SessionType.SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, Category.DIAGNOSTIC, null);

        assertTrue(logs.contains(log1));
        assertTrue(logs.contains(log2));

        long log1amount = logs.stream()
                .filter(log -> log.equals(log1))
                .count();
        assertEquals(2, log1amount);
    }

    @Test
    @DisplayName("Последние записи по фильтру")
    void getLastLogs() {
        Log log1 = new Log(Date.valueOf("2023-10-12"), "desc1", 2600, Category.SESSION, SessionType.SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, Category.DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-10-15"), "desc3", 5000, Category.PG2, null);
        Log log4 = new Log(Date.valueOf("2023-10-13"), "desc4", 10_000, Category.SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);

        // amount > записей
        List<Log> lastLogs = logRepository.getLastLogs(LogsFilter.EMPTY, 5);
        assertEquals(4, lastLogs.size());

        // последняя запись по всем категориям
        lastLogs = logRepository.getLastLogs(LogsFilter.EMPTY, 1);
        assertEquals(1, lastLogs.size());
        assertEquals(lastLogs.get(0), log3);

        // последняя запись категории (из 2)
        LogsFilter.Builder builder = new LogsFilter.Builder().setCategory(SESSION);
        lastLogs = logRepository.getLastLogs(builder.build(), 1);
        assertEquals(1, lastLogs.size());
        assertEquals(lastLogs.get(0), log4);

        // 2 записи категории
        lastLogs = logRepository.getLastLogs(builder.build(), 2);
        assertEquals(2, lastLogs.size());

        // не существует
        builder = new LogsFilter.Builder().setCategory(DIAGNOSTIC).setDescription("desc1");
        lastLogs = logRepository.getLastLogs(builder.build(), 1);
        assertEquals(0, lastLogs.size());
    }

    @Test
    @DisplayName("Последние сессия / диагностика")
    void getLastSessionOrDiagnostic() {
        Log log1 = new Log(Date.valueOf("2023-10-12"), "desc1", 2600, Category.SESSION, SessionType.SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, Category.DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-10-15"), "desc3", 5000, Category.PG2, null);
        Log log4 = new Log(Date.valueOf("2023-10-14"), "desc4", 10_000, Category.SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);

        List<String> lastSessionOrDiagnostic = logRepository.getLastSessionOrDiagnostic();
        assertEquals(2, lastSessionOrDiagnostic.size());
        assertTrue(lastSessionOrDiagnostic.contains(log2.description()));
        assertTrue(lastSessionOrDiagnostic.contains(log4.description()));

        Log log5 = new Log(Date.valueOf("2023-10-15"), "desc4", 10_000, Category.SESSION, null);
        logRepository.addLog(log5);

        lastSessionOrDiagnostic = logRepository.getLastSessionOrDiagnostic();
        assertEquals(1, lastSessionOrDiagnostic.size());
        assertTrue(lastSessionOrDiagnostic.contains(log5.description()));
    }

    @Test
    @DisplayName("Все годы - месяцы в таблице")
    void getAllPeriods() {
        Log log1 = new Log(Date.valueOf("2023-09-12"), "desc1", 2600, Category.SESSION, SessionType.SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, Category.DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-11-15"), "desc3", 5000, Category.PG2, null);
        Log log4 = new Log(Date.valueOf("2023-10-14"), "desc4", 10_000, Category.SESSION, null);
        Log log5 = new Log(Date.valueOf("2021-12-14"), "desc4", 10_000, Category.SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);
        logRepository.addLog(log5);

        List<YearMonth> allPeriods = logRepository.getAllPeriods();
        assertEquals(4, allPeriods.size());

        assertEquals(new YearMonth(2023, 11), allPeriods.get(0));
        assertEquals(new YearMonth(2023, 10), allPeriods.get(1));
        assertEquals(new YearMonth(2023, 9), allPeriods.get(2));
        assertEquals(new YearMonth(2021, 12), allPeriods.get(3));
    }
}