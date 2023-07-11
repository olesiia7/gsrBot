package conf;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import SQLite.DbController;
import SQLite.LogsFilter;
import telegraph.TelegraphController;

@Component
public class Manager {
    @Autowired
    private TelegraphController telegraphController;
    @Autowired
    private DbController dbController;

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void start() {
        System.out.println("started");
        try {
            dbController.getLogs(LogsFilter.EMPTY);
        } catch (SQLException e) {
            e.getMessage();
        }
    }

}
