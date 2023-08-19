package utils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.validation.constraints.NotNull;

import SQLite.model.Log;

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
}
