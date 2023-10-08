package bot.gsr.handlers;

import bot.gsr.events.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventManager {
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public EventManager(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void handleEvent(Event event) {
         eventPublisher.publishEvent(event);
    }
}
