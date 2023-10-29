package bot.gsr.telegram.commands;

import bot.gsr.events.GetBackupEvent;
import bot.gsr.handlers.EventManager;
import bot.gsr.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class GetBackupCommand extends BotCommand {
    private final EventManager eventManager;
    private Bot bot;
    private AbsSender abs;
    private Long chatId;

    public GetBackupCommand(EventManager eventManager) {
        super("backup", "Получить backup из БД");
        this.eventManager = eventManager;
    }

    public void setListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        abs = absSender;
        chatId = chat.getId();
        try {

            CompletableFuture<InputStream> promise = new CompletableFuture<>();
            eventManager.handleEvent(new GetBackupEvent(promise));
            InputStream inputStream = promise.get();

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setDocument(new InputFile(inputStream, "dump.csv"));
            absSender.execute(sendDocument);

        } catch (TelegramApiException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
