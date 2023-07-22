import java.io.IOException;
import java.sql.SQLException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import conf.AppConfig;
import conf.Manager;

public class Application {

    public static void main(String[] args) throws SQLException, IOException, TelegramApiException, InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Manager manager = context.getBean(Manager.class);
        manager.start();
    }
}