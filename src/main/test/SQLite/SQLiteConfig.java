package SQLite;

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
    public CategoriesDAO getCategoriesDAO(ConnectionManager connectionManager) {
        return new CategoriesDAO(connectionManager);
    }

    @Bean
    public CategoriesService getCategoriesService(CategoriesDAO categoriesDAO) {
        return new CategoriesService(categoriesDAO);
    }

    @Bean
    public SessionTypesDAO getSessionTypesDAO(ConnectionManager connectionManager) {
        return new SessionTypesDAO(connectionManager);
    }

    @Bean
    public SessionTypesService getSessionTypesService(SessionTypesDAO categoriesDAO) {
        return new SessionTypesService(categoriesDAO);
    }

    @Bean
    public LogSessionTypesDAO getLogSessionTypesDAO(ConnectionManager connectionManager) {
        return new LogSessionTypesDAO(connectionManager);
    }

    @Bean
    public LogSessionTypesService getLogSessionTypesService(LogSessionTypesDAO logSessionTypesDAO) {
        return new LogSessionTypesService(logSessionTypesDAO);
    }

    @Bean
    public DbController getDbController(LogsService logsService,
                                        CategoriesService categoriesService,
                                        SessionTypesService sessionTypesService,
                                        LogSessionTypesService logSessionTypesService) {
        return new DbController(logsService, categoriesService, sessionTypesService, logSessionTypesService);
    }
}
