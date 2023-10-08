package bot.gsr.events;

import bot.gsr.telegram.model.LogWithUrl;

import java.util.concurrent.CompletableFuture;

public record VerifyAndPublishLogEvent(LogWithUrl logWithUrl, CompletableFuture<Void> resultPromise) implements Event {
}
