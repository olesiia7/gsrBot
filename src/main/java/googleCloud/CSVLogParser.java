package googleCloud;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import SQLite.model.Category;
import SQLite.model.Log;
import SQLite.model.SessionType;

public final class CSVLogParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static List<Log> parseLogs() throws IOException {
        FileReader reader = new FileReader("src/main/resources/GSR log2.csv");
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        List<Log> logs = new ArrayList<>();
        for (CSVRecord csvRecord : csvParser) {
            Date date = toDate(csvRecord.get("Дата"));
            String description = csvRecord.get("Описание");
            int price = Integer.parseInt(csvRecord.get("Стоимость").replace(" ", ""));
            String categoryText = csvRecord.get("Категория");
            if (categoryText.isEmpty()) {
                continue;
            }
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
            logs.add(new Log(null, date, description, price, category, sessionTypes));
        }
        return logs;
    }

    private static Date toDate(String inputDate) {
        LocalDate localDate = LocalDate.parse(inputDate, DATE_FORMATTER);
        return Date.valueOf(localDate);
    }
}