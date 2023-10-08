package bot.gsr.SQLite.model;

public class LogsTable {
    public static final String TABLE_NAME = "logs";

    public static final String C_ID = "id";
    public static final String C_DATE = "date";
    public static final String C_DESCRIPTION = "description";
    public static final String C_PRICE = "price";
    public static final String C_CATEGORY_ID = "category_id";
    public static final String C_SESSION_TYPE_ID = "session_type_id";

    public static String getLogField(String field) {
        return TABLE_NAME + "." + field;
    }
}
