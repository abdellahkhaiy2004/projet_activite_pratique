package ma.ebank.ebankbot.discord;

import ma.ebank.ebankbot.agent.EBankAgent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscordBotTest {

    EBankAgent agent = mock(EBankAgent.class);
    DiscordBot bot;

    @BeforeEach
    void setUp() {
        bot = new DiscordBot(agent);
    }

    @AfterEach
    void tearDown() {
        bot.shutdown();
    }

    private MessageReceivedEvent event(boolean fromBot, String text) {
        MessageReceivedEvent event = mock(MessageReceivedEvent.class, Answers.RETURNS_DEEP_STUBS);
        when(event.getAuthor().isBot()).thenReturn(fromBot);
        when(event.getMessage().getContentRaw()).thenReturn(text);
        when(event.getChannel().getId()).thenReturn("123");
        return event;
    }

    @Test
    void ignoresMessagesWrittenByBots() {
        MessageReceivedEvent event = event(true, "list customers");

        bot.onMessageReceived(event);

        verify(agent, never()).askAgent(anyString(), anyString());
    }

    @Test
    void answersUserMessageInTheSameChannel() {
        when(agent.askAgent("discord-123", "list customers")).thenReturn("Mohammed, Iman, Yassine");
        MessageReceivedEvent event = event(false, "list customers");

        bot.onMessageReceived(event);

        verify(event.getChannel(), timeout(2000)).sendMessage("Mohammed, Iman, Yassine");
    }

    @Test
    void splitsAnswersLongerThanDiscordLimit() {
        when(agent.askAgent(anyString(), anyString())).thenReturn("word ".repeat(1000).strip());
        MessageReceivedEvent event = event(false, "list accounts");

        bot.onMessageReceived(event);

        ArgumentCaptor<CharSequence> sent = ArgumentCaptor.forClass(CharSequence.class);
        verify(event.getChannel(), timeout(2000).atLeast(3)).sendMessage(sent.capture());
        assertTrue(sent.getAllValues().stream().allMatch(m -> m.length() <= DiscordBot.DISCORD_MAX_LENGTH));
    }
}
