package SQLite;

import java.util.List;

import org.springframework.stereotype.Component;

import SQLite.model.LogItem;

@Component
public class LogsService extends Service<LogsDAO> {

    public LogsService(LogsDAO dao) {
        super(dao);
    }

    public List<LogItem> getLogs() {
        return dao.getLogItems();
    }
}
