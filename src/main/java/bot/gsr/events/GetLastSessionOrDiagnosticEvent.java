package bot.gsr.events;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record GetLastSessionOrDiagnosticEvent(@NotNull CompletableFuture<List<String>> result) implements Event{
}
