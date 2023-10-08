package bot.gsr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@ComponentScan(basePackages = {"conf", "telegram", "telegraph", "SQLite", "handlers", "web"})
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
//        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
//        bot.gsr.conf.Manager manager = context.getBean(bot.gsr.conf.Manager.class);
//        manager.start();
    }
}