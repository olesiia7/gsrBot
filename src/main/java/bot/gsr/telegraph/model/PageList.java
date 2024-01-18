package bot.gsr.telegraph.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PageList implements TelegraphObject {
    private static final String TOTAL_COUNT_FIELD = "total_count";
    private static final String PAGES_FIELD = "pages";

    @JsonProperty(TOTAL_COUNT_FIELD)
    private Integer totalCount; // total amount of author's pages
    @JsonProperty(PAGES_FIELD)
    private List<Page> pages;

    public Integer getTotalCount() {
        return totalCount;
    }

    public List<Page> getPages() {
        return pages;
    }

    @Override
    public String toString() {
        return "PageList{" +
                "totalCount=" + totalCount +
                ", pages=\n" + pages.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(",\n")) +
                '}';
    }
}