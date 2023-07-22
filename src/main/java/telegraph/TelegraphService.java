package telegraph;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import telegraph.model.PageList;
import telegraph.model.TelegraphResponse;
import web.WebManager;

@Component
@PropertySource("classpath:telegraph.properties")
@ComponentScan(basePackageClasses = web.WebManager.class)
public class TelegraphService {
    private final WebManager web;

    private static final String GET_PAGE_LIST_URL = "https://api.telegra.ph/getPageList";
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${access.token}")
    private String accessToken;

    public TelegraphService(WebManager web) {
        this.web = web;
    }

    public PageList getPageList(int pages) {
        String url = GET_PAGE_LIST_URL + "?access_token=" + accessToken + "&limit=" + pages;
        String response = web.processGetRequest(url);
        try {
            TelegraphResponse<PageList> result = OBJECT_MAPPER.readValue(response, new TypeReference<>() {});
            if (!result.getOk()) {
                System.out.printf("Error while requesting page list: %s", result.getError());
                return null;
            }
            return result.getResult();
        } catch (JsonProcessingException e) {
            System.out.printf("Error while reading response: %s, %s\n", response, e.getMessage());
            return null;
        }
    }

}