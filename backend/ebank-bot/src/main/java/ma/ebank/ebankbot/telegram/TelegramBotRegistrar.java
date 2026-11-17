package ma.ebank.ebankbot.telegram;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import ma.ebank.ebankbot.agent.EBankAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

/** Registers the Telegram bot at startup. A missing/placeholder token or a failure never stops the app. */
@Slf4j
@Component
public class TelegramBotRegistrar implements ApplicationRunner {

    private final String token;
    private final EBankAgent agent;
    private TelegramBotsLongPollingApplication botsApplication;
    private TelegramBot bot;

    public TelegramBotRegistrar(@Value("${telegram.bot.token:}") String token, EBankAgent agent) {
        this.token = token == null ? "" : token.trim();
        this.agent = agent;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (token.isEmpty() || token.startsWith("put_")) {
            log.warn("TELEGRAM_BOT_TOKEN is not set: Telegram bot disabled");
            return;
        }
        try {
            bot = new TelegramBot(token, agent);
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(token, bot);
            log.info("Telegram long-polling bot registered");
        } catch (Exception e) {
            log.error("Telegram bot could not start (check TELEGRAM_BOT_TOKEN): {}", e.getMessage());
        }
    }

    @PreDestroy
    void stop() throws Exception {
        if (botsApplication != null) {
            botsApplication.close();
        }
        if (bot != null) {
            bot.shutdown();
        }
    }
}
