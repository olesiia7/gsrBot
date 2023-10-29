package bot.gsr.utils;

import bot.gsr.model.Category;
import bot.gsr.model.Log;
import bot.gsr.model.SessionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.*;

/**
 * Использовался для выкачки первоначальной базы Google Sheets
 * Преобразовал данные из .csv в коллекцию Log.
 */
@Deprecated
public final class CSVLogParser {

    public static List<Log> parseLogs() throws IOException {
        FileReader reader = new FileReader("src/main/resources/GSR log2.csv");
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        List<Log> logs = new ArrayList<>();
        for (CSVRecord csvRecord : csvParser) {
            Date date = Utils.toDate(csvRecord.get("Дата"));
            String description = csvRecord.get("Описание");
            int price = Integer.parseInt(csvRecord.get("Стоимость").replace(" ", ""));
            String categoryText = csvRecord.get("Категория");
            if (categoryText.isEmpty()) {
                continue;
            }
            //ToDo: переделать заполнение
            Set<SessionType> sessionTypes = null;
            Category category;
            Optional<Category> catOpt = Arrays.stream(Category.values())
                    .filter(cat -> cat.getName().equals(categoryText))
                    .findFirst();
            if (catOpt.isEmpty()) {
                if (categoryText.equals("По всем полям")) {
                    sessionTypes = Set.of(SessionType.SR, SessionType.SCH1, SessionType.SCH2, SessionType.STRUCTURE);
                    category = Category.SESSION;
                } else if (categoryText.equals("По остальным полям")) {
                    sessionTypes = Set.of(SessionType.SCH1, SessionType.SCH2, SessionType.STRUCTURE);
                    category = Category.SESSION;
                } else {
                    throw new IllegalArgumentException("Категории " + categoryText + " не существует");
                }
            } else {
                category = catOpt.get();
            }

            if (categoryText.equals("Сессия")) {
                sessionTypes = Set.of(SessionType.SR);
            }
            if (description.equals("Ранг")) {
                sessionTypes = Set.of(SessionType.RANG);
            }
            if (sessionTypes != null) {
                for (SessionType sessionType : sessionTypes) {
                    logs.add(new Log(date, description, price, category, sessionType));
                }
            } else {
                logs.add(new Log(date, description, price, category, null));
            }
        }
        return logs;
    }
}