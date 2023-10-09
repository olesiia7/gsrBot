package bot.gsr.service;

import bot.gsr.repository.LogRepository;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    private final LogRepository repository;

    public LogService(LogRepository repository) {
        this.repository = repository;
    }

    public void createTableIfNotExists() {
        repository.createTableIfNotExists();
    }
}
