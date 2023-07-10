package SQLite;

import org.springframework.stereotype.Component;

@Component
public class LogSessionTypesService extends Service<LogSessionTypesDAO> {

    public LogSessionTypesService(LogSessionTypesDAO dao) {
        super(dao);
    }
}
