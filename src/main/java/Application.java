import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import conf.AppConfig;
import conf.Manager;

public class Application {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Manager manager = context.getBean(Manager.class);
        manager.start();
    }
}