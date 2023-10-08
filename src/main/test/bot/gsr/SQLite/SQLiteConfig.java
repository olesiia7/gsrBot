package bot.gsr.SQLite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource("classpath:sqliteTest.properties")
public class SQLiteConfig {
    @Value("${db.path}")
    private String dbPath;

    @Bean
    public ConnectionManager getConnectionManager() {
        return new ConnectionManager();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public LogsDAO getLogDAO(ConnectionManager connectionManager) {
        return new LogsDAO(connectionManager);
    }

    @Bean
    public LogsService getLogService(LogsDAO logsDAO) {
        return new LogsService(logsDAO);
    }

    @Bean
    public DbController getDbController(LogsService logsService) {
        return new DbController(logsService);
    }
}
