package events;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.validation.constraints.NotNull;

public record GetLastSessionOrDiagnosticEvent(@NotNull CompletableFuture<List<String>> result) implements Event{
}
