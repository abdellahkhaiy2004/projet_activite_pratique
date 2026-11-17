package ma.ebank.ebankbot.discord;

import lombok.extern.slf4j.Slf4j;
import ma.ebank.ebankbot.agent.EBankAgent;
import ma.ebank.ebankbot.chat.MessageSplitter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discord adapter: forwards channel messages to the shared {@link EBankAgent} and replies in the
 * same channel. It contains no business logic and never calls the LLM itself.
 */
@Slf4j
public class DiscordBot extends ListenerAdapter {

    /** Discord rejects messages longer than 2000 characters. */
    static final int DISCORD_MAX_LENGTH = 2000;

    private final EBankAgent agent;
    // answers can take several seconds: never block the JDA event thread
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public DiscordBot(EBankAgent agent) {
        this.agent = agent;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // ignore bots (including ourselves) to avoid reply loops
        if (event.getAuthor().isBot()) {
            return;
        }
        String text = event.getMessage().getContentRaw();
        if (text == null || text.isBlank()) {
            return; // attachments only, embeds, ...
        }
        String conversationId = "discord-" + event.getChannel().getId();
        executor.submit(() -> answer(event, conversationId, text));
    }

    private void answer(MessageReceivedEvent event, String conversationId, String text) {
        try {
            event.getChannel().sendTyping().queue();
            String answer = agent.askAgent(conversationId, text);
            if (answer == null || answer.isBlank()) {
                answer = "I have no answer for that.";
            }
            for (String part : MessageSplitter.split(answer, DISCORD_MAX_LENGTH)) {
                event.getChannel().sendMessage(part).queue();
            }
        } catch (Exception e) {
            log.error("Discord request failed in channel {}", event.getChannel().getId(), e);
            event.getChannel().sendMessage("Sorry, something went wrong while processing your request.").queue();
        }
    }

    void shutdown() {
        executor.shutdownNow();
    }
}
