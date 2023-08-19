package events;

/**
 * Создает копию базы данных в формате .csv
 */
public record ConvertDbToCSVEvent(String pathForResult) implements Event {
}
