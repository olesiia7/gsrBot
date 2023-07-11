import java.io.IOException;
import java.sql.SQLException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import conf.AppConfig;
import conf.Manager;

public class Application {

    public static void main(String[] args) throws SQLException, IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Manager manager = context.getBean(Manager.class);
        manager.start();
    }
}