package telegraph.model;

import java.sql.Date;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Page implements TelegraphObject {
    private static final String URL_FIELD = "url";
    private static final String TITLE_FIELD = "title";

    @JsonProperty(URL_FIELD)
    private String url;
    @JsonProperty(TITLE_FIELD)
    private String title;

    private Date created;

    public Page() {
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
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int month = Integer.parseInt(url.substring(url.length() - 5, url.length() - 3));
        int day = Integer.parseInt(url.substring(url.length() - 2));
        try {
            LocalDate localDate = LocalDate.of(currentYear, month, day);
            created = Date.valueOf(localDate);
        } catch (DateTimeException ex) {
            // sometimes if url is already exists, url can be formatted url-dd-mm-copy
            // for this case we will use regexp
            Pattern pattern = Pattern.compile("(\\d{2})-(\\d{2})");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                month = Integer.parseInt(matcher.group(1));
                day = Integer.parseInt(matcher.group(2));
            } else {
                System.out.printf("Error while getting date from url %s\n", url);
            }
            LocalDate localDate = LocalDate.of(currentYear, month, day);
            created = Date.valueOf(localDate);
        }
    }

    @Override
    public String toString() {
        return "Page{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", created=" + created +
                '}';
    }
}