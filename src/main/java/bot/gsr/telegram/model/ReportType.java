package bot.gsr.telegram.model;

public enum ReportType {
    LAST_ALL("Последние события"), // последние события по типу
    MONTHLY("За месяц"), // все активности за месяц
    MONEY_BY_MONTH("Потрачено по месяцам"),
    MONEY_BY_CATEGORY("Потрачено по категориям");
    private final String name;

    ReportType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ReportType findByName(String name) {
        for (ReportType reportType : ReportType.values()) {
            if (reportType.getName().equals(name)) {
                return reportType;
            }
        }
        throw new IllegalArgumentException("No enum constant with name: " + name);
    }
}
