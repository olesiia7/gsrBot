package bot.gsr;

import bot.gsr.repository.impl.LogRepositoryImpl;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@SuppressWarnings("unused")
@Configuration
@Import(LogRepositoryImpl.class)
public class TestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = "jdbc:postgresql://localhost:5435/test";
        String username = "postgres";
        String password = "123456";

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
