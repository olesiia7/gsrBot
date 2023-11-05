package bot.gsr.utils;

import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Date;
import java.time.LocalDate;
import java.util.stream.Stream;

import static bot.gsr.model.Category.*;
import static bot.gsr.model.SessionType.*;
import static bot.gsr.utils.Utils.predictLog;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("Адекватизация Толи", SESSION, SR, 2600), // сессия SR
                Arguments.of("Диагностика на приоритетность ПГ", DIAGNOSTIC, null, 0), // диагностика
                Arguments.of("ПГ2. Самосессии (2), барьер 2", SESSION, SR, 2600), // ПГ2 + барьер
                Arguments.of("ПГ2. Самосессии (2). Барьер 2", SESSION, SR, 2600), // ПГ2 + барьер
                Arguments.of("ПГ2. Самосессии (2)", PG2, null, 0), // ПГ2 + нет барьера
                Arguments.of("Ранговая", SESSION, RANG, 6000), // ранг / ранговая
                Arguments.of("Ранг в СЧ1 - 4", SESSION, RANG_SCH1, 8000), // ранг в СЧ1
                Arguments.of("Ранговая в СЧ1 - 4", SESSION, RANG_SCH1, 8000), // ранговая в СЧ1
                Arguments.of("ПГ1, самосессии, поток 1", SESSION, SR, 2600), // ПГ1 + поток
                Arguments.of("ПГ1, самосессии. Поток 1", SESSION, SR, 2600), // ПГ1 + Поток
                Arguments.of("ПГ1 Самосессии", PG1, null, 0), // ПГ1 + нет потока
                Arguments.of("С# залипание на фрустрации", SESSION, STRUCTURE, 6000), // C# + нет СЧ1 / СЧ2
                Arguments.of("С#СЧ1 залипание на фрустрации", SESSION, STRUCTURE_SCH1, 6000), // C# + СЧ1
                Arguments.of("С#СЧ2 залипание на фрустрации", SESSION, STRUCTURE_SCH2, 6000) // C# + СЧ2
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void predictLogNoCategoryTest(String desc, Category category, SessionType sessionType, int price) {
        Date date = Date.valueOf(LocalDate.now());
        Log log = predictLog(desc, null, date);
        checkLog(log, category, sessionType, price);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    @CsvSource(value = {
            "1 модуль с ДУ;GSR_PRODUCT;;0",
            "Июнь 2023;ONE_PLUS;;4000",
            "Июль 2023;EXPERT_SUPPORT;;10000",
            "Самосессии. Потоки;PG1;;0",
            "Сочи. Доп расходы;OTHER_EXPENSES;;0",
            "Сочи;SOURCE;;0"
    }, delimiter = ';')
    void predictLogWithCategoryTest(String desc, Category category, SessionType sessionType, int price) {
        Date date = Date.valueOf(LocalDate.now());
        Log log = predictLog(desc, category, date);
        checkLog(log, category, sessionType, price);
    }

    private void checkLog(Log log, Category category, SessionType sessionType, int price) {
        assertEquals(category, log.category());
        assertEquals(sessionType, log.sessionType());
        assertEquals(price, log.price());
    }
}