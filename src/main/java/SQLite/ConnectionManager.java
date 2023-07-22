package SQLite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:sqlite.properties")
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ConnectionManager {
    @Value("${db.path}")
    private String pathToDb;

    private Connection connection;

    @PostConstruct
    private void init() {
        String DB_URL = "jdbc:sqlite:" + pathToDb;
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}