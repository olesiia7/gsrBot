package bot.gsr.repository.impl;

import bot.gsr.TestConfig;
import bot.gsr.model.*;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static bot.gsr.model.Category.*;
import static bot.gsr.model.SessionType.SR;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestConfig.class)
class LogRepositoryImplTest {
    private final DateTimeFormatter formatter_yyyy_MM = DateTimeFormatter.ofPattern("yyyy-MM");

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
        Log log = new Log(now, "desc", 2600, SESSION, SessionType.RANG);
        logRepository.addLog(log);
        List<Log> logs = logRepository.getLogs(LogFilter.EMPTY);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());

        assertEquals(log, logs.get(0));

        logRepository.clearAllData();
        log = new Log(now, "desc", 2600, SESSION, null);
        logRepository.addLog(log);
        logs = logRepository.getLogs(LogFilter.EMPTY);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());

        assertEquals(log, logs.get(0));
    }

    @Test
    @DisplayName("Работа фильтров поиска логов")
    void getLogs() {
        Date now = Date.valueOf(LocalDate.now());
        String desc1 = "desc1";
        Log log1 = new Log(now, desc1, 2600, SESSION, SR);
        logRepository.addLog(log1);

        Date dayAgo = Date.valueOf(LocalDate.now().minusDays(1));
        String desc2 = "desc2";
        Log log2 = new Log(dayAgo, desc2, 4000, DIAGNOSTIC, null);
        logRepository.addLog(log2);

        List<Log> logs = logRepository.getLogs(LogFilter.EMPTY);
        assertEquals(2, logs.size());

        // date
        LogFilter.Builder builder = new LogFilter.Builder().setDate(dayAgo);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log2, logs.get(0));

        // description
        builder = new LogFilter.Builder().setDescription(desc1);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        // category
        builder = new LogFilter.Builder().setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        // session type
        builder = new LogFilter.Builder().setSessionType(SR);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        // description + category
        builder = new LogFilter.Builder()
                .setDescription(log1.description())
                .setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(1, logs.size());
        assertEquals(log1, logs.get(0));

        builder = new LogFilter.Builder()
                .setDescription(log2.description())
                .setCategory(SESSION);
        logs = logRepository.getLogs(builder.build());
        assertEquals(0, logs.size());

        // добавим еще одну запись
        logRepository.addLog(log1);
        builder = new LogFilter.Builder().setCategory(SESSION);
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
        Log log1 = new Log(date, desc1, 2600, SESSION, SR);
        logRepository.addLog(log1);

        Date date2 = Date.valueOf("2023-10-14");
        String desc2 = "desc2";
        Log log2 = new Log(date2, desc2, 4000, DIAGNOSTIC, null);
        logRepository.addLog(log2);

        logRepository.addLog(log1);

        String backupFilePath = "src/main/test/resources/dump.csv";
        logRepository.createDump(backupFilePath);
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

        List<Log> logs = logRepository.getLogs(LogFilter.EMPTY);
        assertEquals(3, logs.size());

        Log log1 = new Log(Date.valueOf("2023-10-15"), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, DIAGNOSTIC, null);

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
        Log log1 = new Log(Date.valueOf("2023-10-12"), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-10-15"), "desc3", 5000, PG2, null);
        Log log4 = new Log(Date.valueOf("2023-10-13"), "desc4", 10_000, SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);

        // amount > записей
        List<Log> lastLogs = logRepository.getLastLogs(LogFilter.EMPTY, 5);
        assertEquals(4, lastLogs.size());

        // последняя запись по всем категориям
        lastLogs = logRepository.getLastLogs(LogFilter.EMPTY, 1);
        assertEquals(1, lastLogs.size());
        assertEquals(lastLogs.get(0), log3);

        // последняя запись категории (из 2)
        LogFilter.Builder builder = new LogFilter.Builder().setCategory(SESSION);
        lastLogs = logRepository.getLastLogs(builder.build(), 1);
        assertEquals(1, lastLogs.size());
        assertEquals(lastLogs.get(0), log4);

        // 2 записи категории
        lastLogs = logRepository.getLastLogs(builder.build(), 2);
        assertEquals(2, lastLogs.size());

        // не существует
        builder = new LogFilter.Builder().setCategory(DIAGNOSTIC).setDescription("desc1");
        lastLogs = logRepository.getLastLogs(builder.build(), 1);
        assertEquals(0, lastLogs.size());
    }

    @Test
    @DisplayName("Последние сессия / диагностика")
    void getLastSessionOrDiagnostic() {
        Log log1 = new Log(Date.valueOf("2023-10-12"), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-10-15"), "desc3", 5000, PG2, null);
        Log log4 = new Log(Date.valueOf("2023-10-14"), "desc4", 10_000, SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);

        List<String> lastSessionOrDiagnostic = logRepository.getLastSessionOrDiagnostic();
        assertEquals(2, lastSessionOrDiagnostic.size());
        assertTrue(lastSessionOrDiagnostic.contains(log2.description()));
        assertTrue(lastSessionOrDiagnostic.contains(log4.description()));

        Log log5 = new Log(Date.valueOf("2023-10-15"), "desc4", 10_000, SESSION, null);
        logRepository.addLog(log5);

        lastSessionOrDiagnostic = logRepository.getLastSessionOrDiagnostic();
        assertEquals(1, lastSessionOrDiagnostic.size());
        assertTrue(lastSessionOrDiagnostic.contains(log5.description()));
    }

    @Test
    @DisplayName("Все годы - месяцы в таблице")
    void getAllPeriods() {
        Log log1 = new Log(Date.valueOf("2023-09-12"), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-11-15"), "desc3", 5000, PG2, null);
        Log log4 = new Log(Date.valueOf("2023-10-14"), "desc4", 10_000, SESSION, null);
        Log log5 = new Log(Date.valueOf("2021-12-14"), "desc4", 10_000, SESSION, null);

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

    @Test
    @DisplayName("Отчёт по категории")
    void getCategorySummary() {
        Log log1 = new Log(Date.valueOf("2022-10-12"), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf("2023-10-14"), "desc2", 4000, DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf("2023-10-15"), "desc3", 5000, PG2, null);
        Log log4 = new Log(Date.valueOf("2023-11-14"), "desc4", 10_000, SESSION, null);
        Log log5 = new Log(Date.valueOf("2023-12-14"), "desc4", 10_000, SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);
        logRepository.addLog(log5);

        // все
        List<CategorySummary> actual = logRepository.getCategorySummary(null, null);
        CategorySummary allSessions = new CategorySummary(SESSION, 3, 2600 + 10_000 + 10_000);
        CategorySummary diagnostic = new CategorySummary(DIAGNOSTIC, 1, 4000);
        CategorySummary pg2 = new CategorySummary(PG2, 1, 5000);

        List<CategorySummary> expected = new ArrayList<>(List.of(allSessions, diagnostic, pg2));
        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));

        // с годом
        actual = logRepository.getCategorySummary("2022", null);
        CategorySummary in2022 = new CategorySummary(SESSION, 1, 2600);

        assertEquals(1, actual.size());
        assertEquals(in2022, actual.get(0));

        // с месяцем
        actual = logRepository.getCategorySummary(null, "10");

        expected.clear();
        expected.add(in2022);
        expected.add(diagnostic);
        expected.add(pg2);

        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));

        // год и месяц
        actual = logRepository.getCategorySummary("2023", "10");

        expected.clear();
        expected.add(diagnostic);
        expected.add(pg2);

        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));

        // не существует
        actual = logRepository.getCategorySummary("2022", "11");
        assertEquals(0, actual.size());
    }

    @Test
    @DisplayName("Отчет помесячно SHORT")
    void getShortMonthlySummary() {
        List<MonthlyReport> result = logRepository.getShortMonthlySummary(0);
        assertEquals(0, result.size());

        LocalDate today = LocalDate.now();
        String todayPeriod = today.format(formatter_yyyy_MM);

        LocalDate yesterday = today.minusDays(1);
        boolean twoInTodayMonth = today.getMonth() == yesterday.getMonth(); // yesterday в today месяце

        LocalDate minus1Month = today.minusMonths(1);
        String minus1MonthPeriod = minus1Month.format(formatter_yyyy_MM);

        Log log1 = new Log(Date.valueOf(today), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf(yesterday), "desc2", 4000, DIAGNOSTIC, null);
        Log log3 = new Log(Date.valueOf(minus1Month), "desc3", 10_000, SESSION, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);

        result = logRepository.getShortMonthlySummary(0);
        assertEquals(1, result.size());

        MonthlyReport actualReport = result.get(0);
        assertTrue(actualReport.getSummaries().isEmpty());
        assertEquals(todayPeriod, actualReport.getPeriod());
        assertEquals(twoInTodayMonth ? log1.price() + log2.price() : log1.price(), actualReport.getTotalSpent());

        result = logRepository.getShortMonthlySummary(1);
        assertEquals(2, result.size());
        actualReport = result.get(0);
        assertTrue(actualReport.getSummaries().isEmpty());
        assertEquals(todayPeriod, actualReport.getPeriod());
        assertEquals(twoInTodayMonth ? log1.price() + log2.price() : log1.price(), actualReport.getTotalSpent());

        actualReport = result.get(1);
        assertTrue(actualReport.getSummaries().isEmpty());
        assertEquals(minus1MonthPeriod, actualReport.getPeriod());
        assertEquals(twoInTodayMonth ? log3.price() : log2.price() + log3.price(), actualReport.getTotalSpent());
    }

    @Test
    @DisplayName("Отчет помесячно EXTENDED")
    void getExtendedMonthlySummary() {
        List<MonthlyReport> result = logRepository.getExtendedMonthlySummary(0);
        assertEquals(0, result.size());

        LocalDate today = LocalDate.now();
        String todayPeriod = today.format(formatter_yyyy_MM);

        LocalDate yesterday = today.minusDays(1);
        boolean twoInTodayMonth = today.getMonth() == yesterday.getMonth(); // yesterday в today месяце

        LocalDate minus1Month = today.minusMonths(1);
        String minus1MonthPeriod = minus1Month.format(formatter_yyyy_MM);

        Log log1 = new Log(Date.valueOf(today), "desc1", 2600, SESSION, SR);
        Log log2 = new Log(Date.valueOf(yesterday), "desc2", 4000, SESSION, null);
        Log log3 = new Log(Date.valueOf(minus1Month), "desc3", 10_000, DIAGNOSTIC, null);
        Log log4 = new Log(Date.valueOf(minus1Month), "desc4", 15_000, Category.PG1, null);

        logRepository.addLog(log1);
        logRepository.addLog(log2);
        logRepository.addLog(log3);
        logRepository.addLog(log4);

        result = logRepository.getExtendedMonthlySummary(0);
        assertEquals(1, result.size());

        MonthlyReport actualReport = result.get(0);
        assertEquals(todayPeriod, actualReport.getPeriod());
        List<CategorySummary> summaries = actualReport.getSummaries();
        assertEquals(1, summaries.size());
        CategorySummary actual = summaries.get(0);
        assertEquals(actualReport.getTotalSpent(), actual.priceSum());

        CategorySummary expected = twoInTodayMonth
                ? new CategorySummary(SESSION, 2, log1.price() + log2.price())
                : new CategorySummary(SESSION, 1, log1.price());

        assertEquals(expected, actual);
        assertEquals(expected.priceSum(), actualReport.getTotalSpent());

        // за этот и предыдущий месяц

        result = logRepository.getExtendedMonthlySummary(1);
        assertEquals(2, result.size());

        actualReport = result.get(0);
        assertEquals(todayPeriod, actualReport.getPeriod());
        summaries = actualReport.getSummaries();
        assertEquals(1, summaries.size());
        actual = summaries.get(0);

        expected = twoInTodayMonth
                ? new CategorySummary(SESSION, 2, log1.price() + log2.price())
                : new CategorySummary(SESSION, 1, log1.price());

        assertEquals(expected, actual);
        assertEquals(expected.priceSum(), actualReport.getTotalSpent());

        actualReport = result.get(1);
        assertEquals(minus1MonthPeriod, actualReport.getPeriod());
        summaries = actualReport.getSummaries();

        int expectedSummariesAmount = 2;
        List<CategorySummary> expectedList = new ArrayList<>();
        expectedList.add(new CategorySummary(DIAGNOSTIC, 1, log3.price()));
        expectedList.add(new CategorySummary(Category.PG1, 1, log4.price()));
        int expectedTotalSpent = log3.price() + log4.price();

        if (!twoInTodayMonth) {
            expectedList.add(new CategorySummary(SESSION, 1, log2.price()));
            expectedSummariesAmount = 3;
            expectedTotalSpent += log2.price();
        }
        assertEquals(expectedSummariesAmount, summaries.size());
        assertTrue(expectedList.containsAll(summaries));
        assertTrue(summaries.containsAll(expectedList));

        assertEquals(expectedTotalSpent, actualReport.getTotalSpent());
    }
}