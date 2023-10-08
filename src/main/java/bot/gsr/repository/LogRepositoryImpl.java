package bot.gsr.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
public class LogRepositoryImpl implements LogRepository {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataSource dataSource;

    public LogRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void checkTable() {
        String create = "CREATE TEMPORARY TABLE temp_table (id SERIAL PRIMARY KEY,name VARCHAR(255));";
        String add = "INSERT INTO temp_table (name) VALUES ('Example Name');";
        String get = "SELECT name FROM temp_table;";
        try (Connection con = dataSource.getConnection()) {
            Statement statement = con.createStatement();
            statement.execute(create);
            statement.execute(add);

            ResultSet resultSet = statement.executeQuery(get);
            if (resultSet.next()) {
                String string = resultSet.getString(1);
                System.out.println(string);
            }

        } catch (SQLException e) {
            logger.error("SQL" + "\n\t" + e.getMessage());
        }
    }
}
