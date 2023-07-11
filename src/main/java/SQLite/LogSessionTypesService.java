package SQLite;

import java.sql.SQLException;
import java.util.Set;

import org.springframework.stereotype.Component;

import SQLite.model.SessionType;

@Component
public class LogSessionTypesService extends Service<LogSessionTypesDAO> {

    public LogSessionTypesService(LogSessionTypesDAO dao) {
        super(dao);
    }

    public void addLogSessionTypes(int id, Set<SessionType> sessionTypes) throws SQLException {
        if (sessionTypes == null || sessionTypes.isEmpty()) {
            return;
        }
        dao.addLogSessionTypes(id, sessionTypes);
    }
}
