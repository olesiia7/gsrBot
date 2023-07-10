package SQLite;

import org.springframework.stereotype.Component;

@Component
public class SessionTypesService extends Service<SessionTypesDAO> {
    public SessionTypesService(SessionTypesDAO dao) {
        super(dao);
    }
}
