package bot.gsr.events;

import bot.gsr.telegram.model.LogWithUrl;

/**
 * Публикует в канале статью
 */
public record PublishInChannelEvent(LogWithUrl logWithUrl) implements Event {
}
