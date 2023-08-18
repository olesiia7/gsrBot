package events;

import telegram.model.LogWithUrl;

/**
 * Публикует в канале статью
 */
public record PublishInChannelEvent(LogWithUrl logWithUrl) implements Event {
}
