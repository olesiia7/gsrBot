package bot.gsr.telegraph;

import bot.gsr.model.Log;
import bot.gsr.telegram.model.LogWithUrl;
import bot.gsr.telegraph.model.Page;
import bot.gsr.telegraph.model.PageList;
import bot.gsr.utils.Utils;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TelegraphController {

    private final TelegraphService service;

    public TelegraphController(TelegraphService service) {
        this.service = service;
    }

    /**
     * @param pages amount of last pages
     * @return list of pages
     */
    public PageList getPageList(int pages) {
        return service.getPageList(pages);
    }

    /**
     * Get pages which are not written in database
     *
     * @param lastPageNames last note in database (could be some notes for one day)
     */
    public List<LogWithUrl> getNewLogs(List<String> lastPageNames) {
        int pagesToLoad = 0;
        int newPagesAmount = 0;
        List<LogWithUrl> newLogs = new ArrayList<>();
        search:
        while (true) {
            pagesToLoad += 5;
            PageList pageList = getPageList(pagesToLoad);
            List<Page> pages = pageList.getPages();
            for (; newPagesAmount < pages.size(); newPagesAmount++) {
                Page page = pages.get(newPagesAmount);
                if (lastPageNames.contains(page.getTitle())) {
                    break search;
                }
                newLogs.add(pageToLogWithUrl(page));
            }
        }
        newLogs.sort(Comparator.comparing(l -> l.log().date()));
        return newLogs;
    }

    private LogWithUrl pageToLogWithUrl(@NotNull Page page) {
        Log log = Utils.predictLog(page.getTitle(), null, page.getCreated());
        return new LogWithUrl(log, page.getUrl());
    }
}
