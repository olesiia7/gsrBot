package utils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.validation.constraints.NotNull;

import org.springframework.lang.Nullable;

import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;

import static SQLite.model.Category.EXPERT_SUPPORT;
import static SQLite.model.Category.ONE_PLUS;

public class Utils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * @param inputDate dd.MM.yyyy
     * @return date
     */
    public static Date toDate(@NotNull String inputDate) throws DateTimeParseException {
        LocalDate localDate = LocalDate.parse(inputDate, DATE_FORMATTER);
        return Date.valueOf(localDate);
    }

    /**
     * @param date дата
     * @return дату в формате dd.MM.yyyy
     */
    public static String getDate(@NotNull Date date) {
        LocalDate localDate = date.toLocalDate();
        return localDate.format(DATE_FORMATTER);
    }

    /**
     * Возвращает строку с логом в виде:
     * дата (dd.MM.yyyy),"описание",цена,категория,(?тип сессии)
     */
    public static String getCSV(Log log) {
        return Utils.getDate(log.date()) + "," +
                "\"" + log.description() + "\"," +
                log.price() + "," +
                log.category().getName() + "," +
                (log.sessionType() == null ? "" : log.sessionType().getName()) +
                "\n";
    }

    /**
     *  По описанию и категории (может быть null) пытается определить категорию и тип сессии
     */
    public static Log predictLog(@NotNull String description,
                                 @Nullable Category category,
                                 @NotNull Date date) {
        int price = 0;
        SessionType sessionType = null;
        if (category == null || category == Category.SESSION) {
            if (description.contains("Диагностика") && category == null) {
                category = Category.DIAGNOSTIC;
            } else if (description.contains("СЧ1")) {
                price = 5000;
                sessionType = SessionType.SCH1;
            } else if (description.contains("СЧ2")) {
                price = 5000;
                sessionType = SessionType.SCH2;
            } else if (description.contains("С#") || description.contains("C#")) {
                price = 5000;
                sessionType = SessionType.STRUCTURE;
            } else if (description.contains("СЧ#1")) {
                price = 8000;
                sessionType = SessionType.STRUCTURE_SCH1;
            } else if (description.contains("СЧ#2")) {
                price = 8000;
                sessionType = SessionType.STRUCTURE_SCH2;
            } else if (description.contains("Ранг в СЧ") || description.contains("Ранговая в СЧ")) {
                price = 8000;
                sessionType = SessionType.RANG_SCH1;
            } else {
                price = 2600;
                sessionType = SessionType.SR;
            }
        } else if (category == EXPERT_SUPPORT) {
            price = 10_000;
        } else if (category == ONE_PLUS) {
            price = 4000;
        }
        return new Log(date, description, price, category, sessionType);
    }
}
