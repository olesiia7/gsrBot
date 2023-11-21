package bot.gsr.events;

import bot.gsr.telegram.model.LogWithUrl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record GetNewTelegraphPagesEvent(List<String> lastPageNames,
                                        CompletableFuture<List<LogWithUrl>> result) implements Event {
}
