package telegraph;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import telegraph.model.Page;
import telegraph.model.PageList;

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
     * @param lastSessionOrDiagnostic last note in database (could be some notes for one day)
     */
    public List<Page> getNewPages(List<String> lastSessionOrDiagnostic) {
        int pagesToLoad = 0;
        int newPagesAmount = 0;
        List<Page> newPages = new ArrayList<>();
        search:
        while (true) {
            pagesToLoad += 5;
            PageList pageList = getPageList(pagesToLoad);
            List<Page> pages = pageList.getPages();
            for (; newPagesAmount < pages.size(); newPagesAmount++) {
                Page page = pages.get(newPagesAmount);
                if (lastSessionOrDiagnostic.contains(page.getTitle())) {
                    break search;
                }
                newPages.add(page);
            }
        }
        return newPages;
    }

}
