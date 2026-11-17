package ma.ebank.ebankbot.discord;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import ma.ebank.ebankbot.agent.EBankAgent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Connects the Discord bot at startup. A missing/placeholder token or a failure never stops the app. */
@Slf4j
@Component
public class DiscordBotRegistrar implements ApplicationRunner {

    private final String token;
    private final EBankAgent agent;
    private JDA jda;
    private DiscordBot bot;

    public DiscordBotRegistrar(@Value("${discord.bot.token:}") String token, EBankAgent agent) {
        this.token = token == null ? "" : token.trim();
        this.agent = agent;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (token.isEmpty() || token.startsWith("put_")) {
            log.warn("DISCORD_BOT_TOKEN is not set: Discord bot disabled");
            return;
        }
        try {
            bot = new DiscordBot(agent);
            // only what is needed: read messages (needs the Message Content intent) and answer
            jda = JDABuilder.createLight(token,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(bot)
                    .build();
            log.info("Discord bot connecting");
        } catch (Exception e) {
            log.error("Discord bot could not start (check DISCORD_BOT_TOKEN and the Message Content intent): {}",
                    e.getMessage());
        }
    }

    @PreDestroy
    void stop() {
        if (jda != null) {
            jda.shutdown();
        }
        if (bot != null) {
            bot.shutdown();
        }
    }
}
