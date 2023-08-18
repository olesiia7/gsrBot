package events;

import java.util.concurrent.CompletableFuture;

import telegram.model.LogWithUrl;

public record VerifyAndPublishLogEvent(LogWithUrl logWithUrl, CompletableFuture<Void> resultPromise) implements Event {
}
