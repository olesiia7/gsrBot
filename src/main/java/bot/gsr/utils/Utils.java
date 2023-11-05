package bot.gsr.utils;

import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
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

import static bot.gsr.model.Category.*;
import static bot.gsr.model.SessionType.*;


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
     * дата ("yyyy-MM-dd"),"описание",цена,"категория",(?"тип сессии")
     */
    @SuppressWarnings("GrazieInspection")
    public static String getCSV(Log log) {
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
        SessionType sessionType = null;
        int price = 0;
        if (category == null) {
            category = SESSION;
            if (description.contains("Диагностика")) {
                category = DIAGNOSTIC;
            } else if (description.contains("ПГ")) {
                if (description.contains("ПГ1") && !description.toLowerCase().contains("поток")) {
                    category = PG1;
                } else if (description.contains("ПГ2") && !description.toLowerCase().contains("барьер")) {
                    category = PG2;
                }
            }
        }
        switch (category) {
            case SESSION -> {
                sessionType = SR;
                price = 2600;
                if (description.contains("Ранг")) {
                    sessionType = RANG;
                    price = 6_000;
                    if (description.contains("в СЧ")) {
                        sessionType = RANG_SCH1;
                        price = 8_000;
                        if (description.contains("в СЧ2")) {
                            sessionType = RANG_SCH2;
                        }
                    }
                } else if (description.contains("С#") | description.contains("C#")) {
                    sessionType = STRUCTURE;
                    price = 6_000;
                    if (description.contains("СЧ1")) {
                        sessionType = STRUCTURE_SCH1;
                    } else if (description.contains("СЧ2")) {
                        sessionType = STRUCTURE_SCH2;
                    }
                } else if (description.contains("СЧ")) {
                    sessionType = SCH1;
                    price = 6_000;
                    if (description.contains("СЧ2")) {
                        sessionType = SCH2;
                    }
                }
            }
            case ONE_PLUS -> price = 4_000;
            case EXPERT_SUPPORT -> price = 10_000;
        }
        return new Log(date, description, price, category, sessionType);
    }
}
