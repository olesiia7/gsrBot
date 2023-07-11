package telegram;

import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import telegraph.model.Page;

import static telegram.TelegramService.formatMessage;

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
        String url = "https://telegra.ph/Aktualnoe-dr-07-10";
        String title = "Актуальное: др";
        Page page = new Page(url, title);
        String formattedMessage = formatMessage(page);
        controller.sendMessage(formattedMessage);
    }

}