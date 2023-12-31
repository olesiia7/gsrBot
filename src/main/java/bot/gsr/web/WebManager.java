package bot.gsr.web;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebManager {
    private static final HttpClient CLIENT = HttpClients.createDefault();

    public String processGetRequest(String url) {
        HttpGet request = new HttpGet(url);
        HttpResponse response;
        try {
            response = CLIENT.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            }
            System.out.printf("error while processing %s: code %s", url, statusCode);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
