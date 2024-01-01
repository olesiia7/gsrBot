package bot.gsr.telegram.model;

import bot.gsr.model.Log;
import org.springframework.lang.Nullable;

public record LogWithUrl(Log log, @Nullable String url) {

    public String channelLog() {
        return log.description() + ", " + log.date() + ", " + url;
    }
}
