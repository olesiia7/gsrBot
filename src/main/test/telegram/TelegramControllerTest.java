package telegram;

import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {TelegramController.class, TelegramService.class})
@TestPropertySource(properties = "spring.main.banner-mode=off")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramControllerTest {

    @Autowired
    private TelegramController controller;

    @BeforeAll
    public void connect() {
        final boolean succeeded = controller.connectToBot();
        Assertions.assertTrue(succeeded);
    }

    @Test
    @Ignore // this test really post a message in chat
    public void sendMessageTest() {
        String message = "I'm alive!";
        controller.sendMessage(message);
    }

}