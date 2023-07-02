package telegraph;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import telegraph.model.Page;
import telegraph.model.PageList;

@SpringBootTest(classes = {TelegraphController.class, TelegraphService.class, web.WebManager.class})
@TestPropertySource(properties = "spring.main.banner-mode=off")
class TelegraphControllerTest {

    @Autowired
    private TelegraphController controller;

    @Test
    public void getPageListTest() {
        PageList pageList = controller.getPageList(4);
        Assertions.assertNotNull(pageList);
        List<Page> pages = pageList.getPages();
        Assertions.assertEquals(4, pages.size());
        for (Page page : pages) {
            Assertions.assertNotNull(page.getTitle());
            Assertions.assertNotNull(page.getUrl());
            Assertions.assertNotNull(page.getCreated());
        }
    }


}