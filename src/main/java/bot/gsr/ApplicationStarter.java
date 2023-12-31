package bot.gsr;

import bot.gsr.events.ConvertDbToCSVEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.service.LogService;
import bot.gsr.telegram.TelegramController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public class ApplicationStarter implements ApplicationRunner {
    @SuppressWarnings("unused")
    @Value("${convert.db.to.csv}")
    private boolean convertDbToCsv;

    @SuppressWarnings("unused")
    @Value("${connect.to.bot}")
    private boolean connectToBot;

    private final TelegramController telegramController;
    private final EventManager eventManager;
    private final LogService logService;

    public ApplicationStarter(TelegramController telegramController, EventManager eventManager, LogService logService) {
        this.telegramController = telegramController;
        this.eventManager = eventManager;
        this.logService = logService;
    }

    @Override
    public void run(ApplicationArguments args) {
        logService.createTableIfNotExists();
        if (connectToBot) {
            telegramController.connectToBot();
        }

        if (convertDbToCsv) {
            String pathForResult = "src/main/resources/db.csv";
            eventManager.handleEvent(new ConvertDbToCSVEvent(pathForResult));
            logService.createTableIfNotExists();
            String currentDirectory = System.getProperty("user.dir");
            logService.applyDump(currentDirectory + "/" + pathForResult);
        }
    }
}
