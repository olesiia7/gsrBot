package SQLite.model;

public class SessionTypesTable {
    public static final String TABLE_NAME = "session_type";

    public static final String C_ID = "id";
    public static final String C_NAME = "name";

    public static String getSessionTypeField(String field) {
        return TABLE_NAME + "." + field;
    }
}
