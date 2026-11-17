package ma.ebank.ebankbot.telegram;

import lombok.extern.slf4j.Slf4j;
import ma.ebank.ebankbot.agent.EBankAgent;
import ma.ebank.ebankbot.chat.MessageSplitter;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Telegram adapter: receives updates by long polling and forwards the text to the shared
 * {@link EBankAgent}. It contains no business logic and never calls the LLM itself.
 */
@Slf4j
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    static final int TELEGRAM_MAX_LENGTH = 4096;

    private final TelegramClient telegramClient;
    private final EBankAgent agent;
    private final ScheduledExecutorService typingScheduler = Executors.newSingleThreadScheduledExecutor();

    public TelegramBot(String token, EBankAgent agent) {
        this(new OkHttpTelegramClient(token), agent);
    }

    TelegramBot(TelegramClient telegramClient, EBankAgent agent) {
        this.telegramClient = telegramClient;
        this.agent = agent;
    }

    @Override
    public void consume(Update update) {
        if (update == null || !update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        long chatId = message.getChatId();

        if (!message.hasText()) {
            send(chatId, "I can only read text messages. Please ask me about customers or bank accounts in text.");
            return;
        }

        ScheduledFuture<?> typing = typingScheduler.scheduleAtFixedRate(
                () -> sendTyping(chatId), 0, 4, TimeUnit.SECONDS);
        try {
            // one conversation (and memory) per Telegram chat
            String answer = agent.askAgent("telegram-" + chatId, message.getText());
            if (answer == null || answer.isBlank()) {
                answer = "I have no answer for that.";
            }
            for (String part : MessageSplitter.split(answer, TELEGRAM_MAX_LENGTH)) {
                send(chatId, part);
            }
        } catch (Exception e) {
            log.error("Telegram request failed for chat {}", chatId, e);
            send(chatId, "Sorry, something went wrong while processing your request.");
        } finally {
            typing.cancel(false);
        }
    }

    private void sendTyping(long chatId) {
        try {
            telegramClient.execute(SendChatAction.builder()
                    .chatId(chatId)
                    .action("typing")
                    .build());
        } catch (Exception e) {
            log.debug("Could not send typing action: {}", e.getMessage());
        }
    }

    private void send(long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (Exception e) {
            log.error("Could not send Telegram message to chat {}", chatId, e);
        }
    }

    void shutdown() {
        typingScheduler.shutdownNow();
    }
}
