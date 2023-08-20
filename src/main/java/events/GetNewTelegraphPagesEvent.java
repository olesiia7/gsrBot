package events;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import telegram.model.LogWithUrl;

public record GetNewTelegraphPagesEvent(List<String> lastSessionOrDiagnostic,
                                        CompletableFuture<List<LogWithUrl>> result) implements Event {
}
