package bot.gsr.telegraph.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageTest {

    @ParameterizedTest
    @CsvSource(value = {
            "https://telegra.ph/Aktualnoe-dr-07-10;10;7",
            "https://telegra.ph/PG1-Tolya-Potok-2-10-15;15;10", // с цифрой до даты
            "https://telegra.ph/Aktualnoe-06-28-5;28;6", // -copy
            "https://telegra.ph/Adekvatizaciya-celi-10-15-tendencij-11-14;14;11" // - есть цифры в середине
    }, delimiter = ';')
    @DisplayName("Получение даты создания статьи")
    void pageDateTest(String url, int expectedDay, int expectedMonth) {
        Page page = new Page(url, "title");
        LocalDate localDate = page.getCreated().toLocalDate();
        assertEquals(expectedDay, localDate.getDayOfMonth());
        assertEquals(expectedMonth, localDate.getMonth().getValue()); // jan = 1
    }

}