package SQLite.model;

public class LogSessionTypesTable {
    public static final String TABLE_NAME = "log_session_types";

    public static final String C_LOG_ID = "log_id";
    public static final String C_SESSION_TYPE_ID = "session_type_id";

    public static String getLogSessionTypeField(String field) {
        return TABLE_NAME + "." + field;
    }
}
