package bot.gsr.events;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public record GetBackupEvent(CompletableFuture<InputStream> resultPromise) implements Event {
}
