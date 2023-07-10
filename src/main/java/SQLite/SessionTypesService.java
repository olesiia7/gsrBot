package SQLite;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import SQLite.model.SessionType;

@Component
public class SessionTypesService extends Service<SessionTypesDAO> {
    public SessionTypesService(SessionTypesDAO dao) {
        super(dao);
    }

    /**
     * Наполняет табличку с данными в соответствии с {@link SQLite.model.SessionType}
     */
    public void init() throws SQLException {
        int existSessionTypeSize = dao.getSessionTypeSize();
        // если в таблице лишние session type, то метод выкинет IllegalArgument
        final Set<SessionType> allSessionTypes = new HashSet<>(Arrays.asList(SessionType.values()));
        if (existSessionTypeSize != allSessionTypes.size()) {
            if (existSessionTypeSize == 0) {
                dao.fillSessionTypes();
            } else { // если не хватает session type – добавляем
                Set<SessionType> existCategories = dao.getAllSessionTypes();
                if (existSessionTypeSize < allSessionTypes.size()) {
                    existCategories.forEach(allSessionTypes::remove);
                    dao.addSessionTypes(allSessionTypes);
                    System.out.printf("Added new session type: %s\n", allSessionTypes);
                }
            }
        }
    }
}
