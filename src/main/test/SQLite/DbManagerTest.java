package SQLite;

import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SQLiteConfig.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
class DbManagerTest {

    @Autowired
    private DbController controller;

    @Test
    public void getLogsTest() {
        controller.getLogs();
    }

    @Test
    public void createTablesTest() {
        try {
            controller.dropTables();
            controller.createTablesIfNotExists();
        } catch (SQLException e) {
            Assertions.fail(e.getMessage());
        }
    }

}