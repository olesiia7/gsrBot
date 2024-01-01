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


public final class Utils {
    public static final List<String> MONTH_NAMES = Arrays.asList("Январь", "Февраль", "Март", "Апрель",
            "Май", "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь", "Декабрь");
    public static final List<String> SHORT_MONTH_NAMES = Arrays.asList("Янв", "Фев", "Март", "Апр",
            "Май", "Июнь", "Июль", "Авг",
            "Сент", "Окт", "Ноя", "Дек");
    public static final String BACK_TEXT = "Назад";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("###,###");

    private Utils() {
    }

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
     * @param date YYYY-dd
     * @return дата вида Сентябрь 2023
     */
    public static String getDate(String date) {
        String[] split = date.split("-");
        String year = split[0];
        int monthId = Integer.parseInt(split[1]);
        String month = getMonth(monthId);
        return month + " " + year;
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
     * @param monthNumber номер месяца (январь = 1)
     * @return название месяца
     */
    public static String getMonth(int monthNumber) {
        return MONTH_NAMES.get(monthNumber - 1);
    }

    /**
     * @param monthNumber номер месяца (январь = 1)
     * @return короткое название месяца
     */
    public static String getShortMonth(int monthNumber) {
        return SHORT_MONTH_NAMES.get(monthNumber - 1);
    }

    /**
     * @param day   день
     * @param month месяц (январь = 1)
     * @param year  год
     * @return дату
     */
    public static Date getDate(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
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
                if (description.contains("Ранг")) {
                    sessionType = RANG;
                    if (description.contains("в СЧ")) {
                        sessionType = RANG_SCH1;
                        if (description.contains("в СЧ2")) {
                            sessionType = RANG_SCH2;
                        }
                    }
                } else if (description.contains("С#") | description.contains("C#")) {
                    sessionType = STRUCTURE;
                    if (description.contains("СЧ1")) {
                        sessionType = STRUCTURE_SCH1;
                    } else if (description.contains("СЧ2")) {
                        sessionType = STRUCTURE_SCH2;
                    }
                } else if (description.contains("СЧ")) {
                    sessionType = SCH1;
                    if (description.contains("СЧ2")) {
                        sessionType = SCH2;
                    }
                }
            }
            case ONE_PLUS -> price = 4_000;
            case EXPERT_SUPPORT -> price = 10_000;
        }
        if (sessionType != null) {
            price = getSessionTypePrice(sessionType);
        }
        return new Log(date, description, price, category, sessionType);
    }

    public static int getSessionTypePrice(SessionType sessionType) {
        int price = 0;
        switch (sessionType) {
            case SR -> price = 2600;
            case RANG, SCH1, SCH2, STRUCTURE, STRUCTURE_SCH1, STRUCTURE_SCH2 -> price = 6_000;
            case RANG_SCH1, RANG_SCH2 -> price = 8_000;
        }
        return price;
    }
}
