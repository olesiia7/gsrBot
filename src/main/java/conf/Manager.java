package conf;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import SQLite.DbController;
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
        dbController.getLogs();
    }

}
