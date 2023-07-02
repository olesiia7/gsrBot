package telegraph;

import org.springframework.stereotype.Component;

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

}
