package conf;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"conf", "telegram", "telegraph", "SQLite"})
public class AppConfig {
}