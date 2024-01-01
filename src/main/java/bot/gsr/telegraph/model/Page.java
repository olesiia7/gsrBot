package bot.gsr.telegraph.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page implements TelegraphObject {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String URL_FIELD = "url";
    private static final String TITLE_FIELD = "title";

    @JsonProperty(URL_FIELD)
    private String url;
    @JsonProperty(TITLE_FIELD)
    private String title;

    private Date created;

    public Page() {
    }

    public Page(final String url, final String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public Date getCreated() {
        if (created == null) {
            calculateDate();
        }
        return created;
    }

    private void calculateDate() {
        // sometimes if url is already exists, url can be formatted url-dd-mm-copy
        // for this case we will use regexp
        Pattern pattern = Pattern.compile("(\\d{2})-(\\d{2})");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            LocalDate localDate = LocalDate.of(currentYear, month, day);
            created = Date.valueOf(localDate);
        }
        if (created == null) {
            logger.error("Error while getting date from url {}", url);
        }
    }

    @Override
    public String toString() {
        return "Page{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", created=" + getCreated() +
                '}';
    }
}