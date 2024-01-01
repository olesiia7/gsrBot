package bot.gsr.telegraph;

import bot.gsr.telegraph.model.PageList;
import bot.gsr.telegraph.model.TelegraphResponse;
import bot.gsr.web.WebManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:telegraph.properties")
public class TelegraphService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
                logger.error("Error while requesting page list: {}", result.getError());
                return null;
            }
            return result.getResult();
        } catch (JsonProcessingException e) {
            logger.error("Error while reading response: {}, {}", response, e.getMessage());
            return null;
        }
    }

}