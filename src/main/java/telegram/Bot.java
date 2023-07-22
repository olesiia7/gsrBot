package telegram;

import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static telegram.TelegramUtils.addDecisionToMsg;
import static telegram.TelegramUtils.cleanText;

public final class Bot extends TelegramLongPollingCommandBot {
    private final String botName;
    private AnswerListener listener;

    public Bot(String botToken, String botName) {
        super(botToken);
        this.botName = botName;
    }

    public void setListener(AnswerListener listener) {
        this.listener = listener;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            String json = update.getCallbackQuery().getData();
            try {
                listener.processAnswer(json);
                handleButtonClick(update.getCallbackQuery());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        if (update.hasMessage()) {
            String text = update.getMessage().getText();
            try {
                listener.processAnswer(text);
            } catch (TelegramApiException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void handleButtonClick(CallbackQuery callbackQuery) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(callbackQuery.getMessage().getChatId());
        editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
        String text = callbackQuery.getMessage().getText();
        text = addDecisionToMsg(text, callbackQuery.getData());
        text = cleanText(text);

        editMessageText.setText(text);
        editMessageText.setParseMode("MarkdownV2");
        editMessageText.setReplyMarkup(null);

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}