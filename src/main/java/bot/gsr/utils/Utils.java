package bot.gsr.utils;

import bot.gsr.SQLite.model.Category;
import bot.gsr.SQLite.model.Log;
import bot.gsr.SQLite.model.SessionType;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.sql.Date;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static bot.gsr.SQLite.model.Category.EXPERT_SUPPORT;
import static bot.gsr.SQLite.model.Category.ONE_PLUS;

public class Utils {
    public static final List<String> MONTH_NAMES = Arrays.asList("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("###,###");

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
     * Возвращает цену в формате 42 000 ₽
     */
    public static String formatPrice(int price) {
        return PRICE_FORMAT.format(price) + " ₽";
    }

    /**
     * Возвращает строку с логом в виде:
     * дата (dd.MM.yyyy),"описание",цена,категория,(?тип сессии)
     */
    @SuppressWarnings("GrazieInspection")
    public static String getCSV(Log log) {
        return Utils.getDate(log.date()) + "," +
                "\"" + log.description() + "\"," +
                log.price() + "," +
                log.category().getName() + "," +
                (log.sessionType() == null ? "" : log.sessionType().getName());
    }

    /**
     * Возвращает строку с логом в виде:
     * дата ("yyyy-MM-dd"),"описание",цена,"категория",(?"тип сессии")
     */
    @SuppressWarnings("GrazieInspection")
    public static String getCSV(bot.gsr.model.Log log) {
        return "\"" + log.date() + "\"," +
                "\"" + log.description() + "\"," +
                log.price() + "," +
                "\"" + log.category().getName() + "\"," +
                (log.sessionType() == null ? "" : "\"" + log.sessionType().getName() + "\"");
    }

    /**
     * @param month месяц
     * @return номер месяца - 1, Январь = 0
     */
    public static int getMonthNumber(String month) {
        return MONTH_NAMES.indexOf(month);
    }

    /**
     * @param monthNumber номер месяца - 1 (январь = 0)
     * @return название
     */
    public static String getMonth(int monthNumber) {
        return MONTH_NAMES.get(monthNumber);
    }

    /**
     * @param day   день
     * @param month месяц - 1 (январь = 0)
     * @param year  год
     * @return дату
     */
    public static Date getDate(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        return new Date(calendar.getTime().getTime());
    }

    /**
     * По описанию и категории (может быть null) пытается определить категорию и тип сессии
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
                category = Category.SESSION;
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
