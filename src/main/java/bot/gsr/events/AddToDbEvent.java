package bot.gsr.events;


import bot.gsr.model.Log;

/**
 * Добавляет Log в базу данных
 */
public record AddToDbEvent(Log log) implements Event {
}
